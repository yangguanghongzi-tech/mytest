package com.learning.platform.module.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.learning.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content")
public class Content extends BaseEntity {

    public static final String DRAFT = "DRAFT";
    public static final String PUBLISHED = "PUBLISHED";

    private Long categoryId;
    /** ContentType 名称：VIDEO / DOC / TEXT，创建后不可修改。 */
    private String type;
    private String title;
    private String description;
    /** 允许更新时显式写入 null，以支持删除封面。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String coverKey;
    private String status;
    private Integer sort;
    private LocalDateTime publishedAt;
}
