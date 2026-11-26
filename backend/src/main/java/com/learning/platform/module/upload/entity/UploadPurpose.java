package com.learning.platform.module.upload.entity;

import java.util.Map;

/** 上传用途决定了允许的文件类型。MIME 由服务端根据扩展名确定，不信任客户端传入的值。 */
public enum UploadPurpose {

    VIDEO(Map.of("mp4", "video/mp4")),
    DOC(Map.of(
            "pdf", "application/pdf",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "ppt", "application/vnd.ms-powerpoint",
            "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "xls", "application/vnd.ms-excel",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
    COVER(Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp"));

    private final Map<String, String> mimeByExt;

    UploadPurpose(Map<String, String> mimeByExt) {
        this.mimeByExt = mimeByExt;
    }

    /** 返回扩展名对应的 MIME，不支持时返回 null。 */
    public String mimeOf(String ext) {
        return mimeByExt.get(ext);
    }

    public java.util.Set<String> extensions() {
        return mimeByExt.keySet();
    }
}
