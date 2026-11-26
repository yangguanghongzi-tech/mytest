package com.learning.platform.module.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.learning.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 当前用户观看视频的最后播放卡点。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("watch_progress")
public class WatchProgress extends BaseEntity {

    private String userId;
    private Long contentId;
    private Integer progressSeconds;
}
