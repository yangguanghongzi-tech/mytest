package com.learning.platform.module.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.learning.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 文本内容的正文（已在服务端做过 HTML 白名单清洗）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("text_body")
public class TextBody extends BaseEntity {

    private Long contentId;
    private String body;
}
