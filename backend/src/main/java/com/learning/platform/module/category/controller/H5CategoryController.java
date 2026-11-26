package com.learning.platform.module.category.controller;

import com.learning.platform.common.Result;
import com.learning.platform.module.category.dto.CategoryDtos.Node;
import com.learning.platform.module.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/h5/api/categories")
@RequiredArgsConstructor
public class H5CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public Result<List<Node>> tree() {
        return Result.ok(categoryService.tree(true));
    }
}
