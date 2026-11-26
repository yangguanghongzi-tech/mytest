package com.learning.platform.module.upload.service;

import com.learning.platform.auth.TenantContext;
import com.learning.platform.common.BizException;
import com.learning.platform.module.upload.UploadProperties;
import com.learning.platform.module.upload.dto.UploadDtos.CompleteResponse;
import com.learning.platform.module.upload.dto.UploadDtos.InitRequest;
import com.learning.platform.module.upload.dto.UploadDtos.InitResponse;
import com.learning.platform.module.upload.dto.UploadDtos.PartUrl;
import com.learning.platform.module.upload.dto.UploadDtos.ProgressResponse;
import com.learning.platform.module.upload.entity.UploadPurpose;
import com.learning.platform.module.upload.entity.UploadTask;
import com.learning.platform.module.upload.mapper.UploadTaskMapper;
import com.learning.platform.storage.StorageService;
import com.learning.platform.storage.StorageService.PartInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 分片直传：后端只负责发起、签名和合并，文件数据由浏览器直接 PUT 到 OBS。
 * 对象 key 由服务端生成，以 tenantId 开头，保证存储层也按租户隔离。
 */
@Service
@RequiredArgsConstructor
public class UploadService {

    /** OBS 限制：分片号范围为 1~10000。 */
    private static final int MAX_PARTS = 10000;
    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyyMM");

    private final UploadTaskMapper taskMapper;
    private final StorageService storage;
    private final UploadProperties props;

    @Transactional
    public InitResponse init(InitRequest req) {
        String fileName = sanitizeFileName(req.fileName());
        String ext = extension(fileName);
        String mime = req.purpose().mimeOf(ext);
        if (mime == null) {
            throw BizException.badRequest("不支持的文件类型，允许：" + String.join("/", req.purpose().extensions()));
        }
        long max = maxSize(req.purpose());
        if (req.fileSize() > max) {
            throw BizException.badRequest("文件不能超过 " + (max / 1024 / 1024) + "MB");
        }

        long partSize = props.getPartSize().toBytes();
        int partCount = (int) Math.max(1, (req.fileSize() + partSize - 1) / partSize);
        if (partCount > MAX_PARTS) {
            throw BizException.badRequest("文件过大");
        }

        String objectKey = "%s/%s/%s/%s.%s".formatted(TenantContext.requireTenantId(),
                req.purpose().name().toLowerCase(Locale.ROOT), YearMonth.now().format(YM),
                UUID.randomUUID().toString().replace("-", ""), ext);
        String uploadId = storage.initMultipart(objectKey, mime);

        UploadTask task = new UploadTask();
        task.setPurpose(req.purpose().name());
        task.setUploadId(uploadId);
        task.setObjectKey(objectKey);
        task.setFileName(fileName);
        task.setMime(mime);
        task.setFileSize(req.fileSize());
        task.setPartSize(partSize);
        task.setPartCount(partCount);
        task.setStatus(UploadTask.UPLOADING);
        taskMapper.insert(task);
        return new InitResponse(task.getId(), partSize, partCount);
    }

    public List<PartUrl> signPartUrls(Long taskId, List<Integer> partNumbers) {
        UploadTask task = requireUploading(taskId);
        Set<Integer> unique = new LinkedHashSet<>(partNumbers);
        if (unique.size() > props.getMaxPartUrlsPerRequest()) {
            throw BizException.badRequest("单次最多申请 " + props.getMaxPartUrlsPerRequest() + " 个分片");
        }
        for (Integer n : unique) {
            if (n == null || n < 1 || n > task.getPartCount()) {
                throw BizException.badRequest("分片号不正确：" + n);
            }
        }
        return unique.stream()
                .map(n -> new PartUrl(n, storage.signPartUploadUrl(task.getObjectKey(), task.getUploadId(), n,
                        props.getPartUrlExpiresSeconds())))
                .toList();
    }

    public ProgressResponse progress(Long taskId) {
        UploadTask task = require(taskId);
        List<Integer> uploaded = UploadTask.UPLOADING.equals(task.getStatus())
                ? storage.listParts(task.getObjectKey(), task.getUploadId()).stream()
                .map(PartInfo::partNumber).toList()
                : List.of();
        return new ProgressResponse(task.getId(), task.getStatus(), task.getPartSize(), task.getPartCount(),
                uploaded);
    }

    @Transactional
    public CompleteResponse complete(Long taskId) {
        UploadTask task = requireUploading(taskId);
        List<PartInfo> parts = storage.listParts(task.getObjectKey(), task.getUploadId());

        if (parts.size() != task.getPartCount()
                || parts.stream().anyMatch(p -> p.partNumber() < 1 || p.partNumber() > task.getPartCount())) {
            throw BizException.badRequest("还有分片未上传完成（%d/%d）".formatted(parts.size(), task.getPartCount()));
        }
        long total = parts.stream().mapToLong(PartInfo::size).sum();
        if (total != task.getFileSize()) {
            throw BizException.badRequest("上传的文件大小与声明不一致");
        }

        storage.completeMultipart(task.getObjectKey(), task.getUploadId(), parts);
        if (storage.objectSize(task.getObjectKey()) != task.getFileSize()) {
            throw BizException.badRequest("文件合并后大小校验失败");
        }

        task.setStatus(UploadTask.COMPLETED);
        taskMapper.updateById(task);
        return new CompleteResponse(task.getId(), task.getFileName(), task.getFileSize());
    }

    @Transactional
    public void abort(Long taskId) {
        UploadTask task = requireUploading(taskId);
        storage.abortMultipart(task.getObjectKey(), task.getUploadId());
        task.setStatus(UploadTask.ABORTED);
        taskMapper.updateById(task);
    }

    /**
     * 内容引用已上传完成的文件：校验用途后标记为已使用，防止同一个文件被多个内容重复引用。
     * 需在调用方事务中执行。
     */
    public UploadTask consume(Long taskId, UploadPurpose expected) {
        UploadTask task = require(taskId);
        if (!expected.name().equals(task.getPurpose())) {
            throw BizException.badRequest("文件类型与内容类型不匹配");
        }
        if (!UploadTask.COMPLETED.equals(task.getStatus())) {
            throw BizException.badRequest("文件未上传完成或已被使用");
        }
        task.setStatus(UploadTask.USED);
        taskMapper.updateById(task);
        return task;
    }

    private UploadTask require(Long taskId) {
        UploadTask task = taskId == null ? null : taskMapper.selectById(taskId);
        if (task == null) {
            throw BizException.notFound("上传任务不存在");
        }
        return task;
    }

    private UploadTask requireUploading(Long taskId) {
        UploadTask task = require(taskId);
        if (!UploadTask.UPLOADING.equals(task.getStatus())) {
            throw BizException.conflict("上传任务已结束");
        }
        return task;
    }

    private long maxSize(UploadPurpose purpose) {
        return switch (purpose) {
            case VIDEO -> props.getMaxVideoSize().toBytes();
            case DOC -> props.getMaxDocSize().toBytes();
            case COVER -> props.getMaxCoverSize().toBytes();
        };
    }

    /** 去掉路径部分和控制字符，只保留原始文件名用于展示和下载。 */
    static String sanitizeFileName(String name) {
        String n = name.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}\"]", "").trim();
        if (n.isEmpty()) {
            throw BizException.badRequest("文件名不正确");
        }
        return n;
    }

    static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
