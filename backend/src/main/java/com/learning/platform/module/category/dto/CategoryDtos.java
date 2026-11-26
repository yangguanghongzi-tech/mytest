package com.learning.platform.module.category.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record SaveRequest(
            /** 为空或 0 表示一级分类。 */
            Long parentId,
            @NotBlank(message = "分类名称不能为空") @Size(max = 50, message = "分类名称最多 50 个字") String name,
            Integer sort,
            @Min(value = 0, message = "状态不正确") @Max(value = 1, message = "状态不正确") Integer status) {
    }

    public record Node(Long id, Long parentId, String name, Integer sort, Integer status, List<Node> children) {
        public Node(Long id, Long parentId, String name, Integer sort, Integer status) {
            this(id, parentId, name, sort, status, new ArrayList<>());
        }
    }
}
