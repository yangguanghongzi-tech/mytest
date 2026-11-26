package com.learning.platform.storage;

import java.util.List;

/** 对象存储抽象。生产环境用 ObsStorageService，测试中可替换为内存实现。 */
public interface StorageService {

    /** 初始化分片上传，返回 uploadId。 */
    String initMultipart(String objectKey, String contentType);

    /** 为指定分片签发浏览器可直接 PUT 的临时 URL。 */
    String signPartUploadUrl(String objectKey, String uploadId, int partNumber, long expiresSeconds);

    /** 列出已上传成功的分片（用于断点续传和合并）。 */
    List<PartInfo> listParts(String objectKey, String uploadId);

    void completeMultipart(String objectKey, String uploadId, List<PartInfo> parts);

    void abortMultipart(String objectKey, String uploadId);

    /** 对象大小；对象不存在时返回 -1。 */
    long objectSize(String objectKey);

    /**
     * 签发临时 GET URL，用于播放和预览。
     * downloadFileName 不为空时，以附件形式下载并使用该文件名。
     */
    String signGetUrl(String objectKey, long expiresSeconds, String downloadFileName);

    record PartInfo(int partNumber, String etag, long size) {
    }
}
