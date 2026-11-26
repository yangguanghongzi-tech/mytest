package com.learning.platform;

import com.learning.platform.storage.StorageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 测试用：模拟 OBS 分片上传。用 putPart 模拟浏览器直传分片。 */
public class InMemoryStorage implements StorageService {

    private final Map<String, Map<Integer, Long>> uploads = new ConcurrentHashMap<>();
    private final Map<String, Long> objects = new ConcurrentHashMap<>();

    public void putPart(String uploadId, int partNumber, long size) {
        uploads.get(uploadId).put(partNumber, size);
    }

    @Override
    public String initMultipart(String objectKey, String contentType) {
        String id = UUID.randomUUID().toString();
        uploads.put(id, new ConcurrentHashMap<>());
        return id;
    }

    @Override
    public String signPartUploadUrl(String objectKey, String uploadId, int partNumber, long expiresSeconds) {
        return "https://obs.test/" + objectKey + "?uploadId=" + uploadId + "&partNumber=" + partNumber;
    }

    @Override
    public List<PartInfo> listParts(String objectKey, String uploadId) {
        List<PartInfo> list = new ArrayList<>();
        uploads.getOrDefault(uploadId, Map.of()).entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> list.add(new PartInfo(e.getKey(), "etag" + e.getKey(), e.getValue())));
        return list;
    }

    @Override
    public void completeMultipart(String objectKey, String uploadId, List<PartInfo> parts) {
        objects.put(objectKey, parts.stream().mapToLong(PartInfo::size).sum());
        uploads.remove(uploadId);
    }

    @Override
    public void abortMultipart(String objectKey, String uploadId) {
        uploads.remove(uploadId);
    }

    @Override
    public long objectSize(String objectKey) {
        return objects.getOrDefault(objectKey, -1L);
    }

    @Override
    public String signGetUrl(String objectKey, long expiresSeconds, String downloadFileName) {
        return "https://obs.test/" + objectKey + (downloadFileName == null ? "" : "?download");
    }
}
