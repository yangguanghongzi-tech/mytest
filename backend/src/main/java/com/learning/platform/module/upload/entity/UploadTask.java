package com.learning.platform.module.upload.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.learning.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("upload_task")
public class UploadTask extends BaseEntity {

    public static final String UPLOADING = "UPLOADING";
    public static final String COMPLETED = "COMPLETED";
    /** 已被内容引用，不能重复使用。 */
    public static final String USED = "USED";
    public static final String ABORTED = "ABORTED";

    private String purpose;
    private String uploadId;
    private String objectKey;
    private String fileName;
    private String mime;
    private Long fileSize;
    private Long partSize;
    private Integer partCount;
    private String status;
}
