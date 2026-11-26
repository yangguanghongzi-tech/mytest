package com.learning.platform.module.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.learning.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 视频 / 文档内容对应的 OBS 文件。每个内容只有一条有效记录。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("media_file")
public class MediaFile extends BaseEntity {

    private Long contentId;
    private String objectKey;
    private String fileName;
    private String mime;
    private Long fileSize;
    /** 视频时长（秒），由管理端读取视频元数据后上报。 */
    private Integer duration;
    /** 二期 MPC 转码使用。 */
    private String hlsKey;
    private String transcodeStatus;
}
