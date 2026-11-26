package com.learning.platform.module.category.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.learning.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class Category extends BaseEntity {

    public static final long ROOT = 0L;
    public static final int ENABLED = 1;
    public static final int DISABLED = 0;

    /** 0 表示一级分类。最多支持两级。 */
    private Long parentId;
    private String name;
    private Integer sort;
    private Integer status;
}
