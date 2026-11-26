package com.learning.platform.storage;

import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.model.AbortMultipartUploadRequest;
import com.obs.services.model.CompleteMultipartUploadRequest;
import com.obs.services.model.HttpMethodEnum;
import com.obs.services.model.InitiateMultipartUploadRequest;
import com.obs.services.model.ListPartsRequest;
import com.obs.services.model.ListPartsResult;
import com.obs.services.model.Multipart;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.PartEtag;
import com.obs.services.model.TemporarySignatureRequest;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 华为云 OBS 实现。桶应设为私有，访问全部通过临时签名 URL。 */
@Service
@ConditionalOnProperty(prefix = "app.obs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ObsStorageService implements StorageService {

    private final ObsClient client;
    private final String bucket;

    public ObsStorageService(ObsProperties props) {
        if (!StringUtils.hasText(props.getEndpoint()) || !StringUtils.hasText(props.getBucket())) {
            throw new IllegalStateException("请配置 app.obs.endpoint / bucket / access-key / secret-key");
        }
        this.client = new ObsClient(props.getAccessKey(), props.getSecretKey(), props.getEndpoint());
        this.bucket = props.getBucket();
    }

    @Override
    public String initMultipart(String objectKey, String contentType) {
        InitiateMultipartUploadRequest req = new InitiateMultipartUploadRequest(bucket, objectKey);
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentType(contentType);
        req.setMetadata(meta);
        return client.initiateMultipartUpload(req).getUploadId();
    }

    @Override
    public String signPartUploadUrl(String objectKey, String uploadId, int partNumber, long expiresSeconds) {
        TemporarySignatureRequest req = new TemporarySignatureRequest(HttpMethodEnum.PUT, expiresSeconds);
        req.setBucketName(bucket);
        req.setObjectKey(objectKey);
        Map<String, Object> query = new HashMap<>();
        query.put("uploadId", uploadId);
        query.put("partNumber", String.valueOf(partNumber));
        req.setQueryParams(query);
        return client.createTemporarySignature(req).getSignedUrl();
    }

    @Override
    public List<PartInfo> listParts(String objectKey, String uploadId) {
        List<PartInfo> parts = new ArrayList<>();
        ListPartsRequest req = new ListPartsRequest(bucket, objectKey, uploadId);
        req.setMaxParts(1000);
        while (true) {
            ListPartsResult result = client.listParts(req);
            for (Multipart p : result.getMultipartList()) {
                parts.add(new PartInfo(p.getPartNumber(), p.getEtag(), p.getSize()));
            }
            if (!result.isTruncated()) {
                break;
            }
            req.setPartNumberMarker(Integer.parseInt(result.getNextPartNumberMarker()));
        }
        parts.sort(Comparator.comparingInt(PartInfo::partNumber));
        return parts;
    }

    @Override
    public void completeMultipart(String objectKey, String uploadId, List<PartInfo> parts) {
        List<PartEtag> etags = parts.stream().map(p -> new PartEtag(p.etag(), p.partNumber())).toList();
        client.completeMultipartUpload(new CompleteMultipartUploadRequest(bucket, objectKey, uploadId, etags));
    }

    @Override
    public void abortMultipart(String objectKey, String uploadId) {
        client.abortMultipartUpload(new AbortMultipartUploadRequest(bucket, objectKey, uploadId));
    }

    @Override
    public long objectSize(String objectKey) {
        try {
            return client.getObjectMetadata(bucket, objectKey).getContentLength();
        } catch (ObsException e) {
            if (e.getResponseCode() == 404) {
                return -1;
            }
            throw e;
        }
    }

    @Override
    public String signGetUrl(String objectKey, long expiresSeconds, String downloadFileName) {
        TemporarySignatureRequest req = new TemporarySignatureRequest(HttpMethodEnum.GET, expiresSeconds);
        req.setBucketName(bucket);
        req.setObjectKey(objectKey);
        if (StringUtils.hasText(downloadFileName)) {
            String encoded = URLEncoder.encode(downloadFileName, StandardCharsets.UTF_8).replace("+", "%20");
            Map<String, Object> query = new HashMap<>();
            query.put("response-content-disposition", "attachment; filename*=UTF-8''" + encoded);
            req.setQueryParams(query);
        }
        return client.createTemporarySignature(req).getSignedUrl();
    }

    @PreDestroy
    public void close() throws IOException {
        client.close();
    }
}
