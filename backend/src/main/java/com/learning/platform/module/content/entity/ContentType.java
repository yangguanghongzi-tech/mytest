package com.learning.platform.module.content.entity;

import com.learning.platform.module.upload.entity.UploadPurpose;

public enum ContentType {
    VIDEO(UploadPurpose.VIDEO),
    DOC(UploadPurpose.DOC),
    TEXT(null);

    /** 该类型内容需要的上传文件用途；TEXT 不需要文件。 */
    private final UploadPurpose filePurpose;

    ContentType(UploadPurpose filePurpose) {
        this.filePurpose = filePurpose;
    }

    public UploadPurpose filePurpose() {
        return filePurpose;
    }
}
