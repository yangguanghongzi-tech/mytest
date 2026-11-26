package com.learning.platform.module.content.controller;

import com.learning.platform.common.PageResult;
import com.learning.platform.common.Result;
import com.learning.platform.module.content.dto.ContentDtos.Detail;
import com.learning.platform.module.content.dto.ContentDtos.ListItem;
import com.learning.platform.module.content.dto.ContentDtos.SaveRequest;
import com.learning.platform.module.content.entity.ContentType;
import com.learning.platform.module.content.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/contents")
@RequiredArgsConstructor
public class AdminContentController {

    private final ContentService contentService;

    @GetMapping
    public Result<PageResult<ListItem>> page(@RequestParam(required = false) Long categoryId,
                                             @RequestParam(required = false) ContentType type,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "20") long size) {
        return Result.ok(contentService.adminPage(categoryId, type, status, keyword, page, size));
    }

    @GetMapping("/{id}")
    public Result<Detail> detail(@PathVariable Long id) {
        return Result.ok(contentService.adminDetail(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody SaveRequest req) {
        return Result.ok(contentService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SaveRequest req) {
        contentService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        contentService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        contentService.publish(id, true);
        return Result.ok();
    }

    @PostMapping("/{id}/unpublish")
    public Result<Void> unpublish(@PathVariable Long id) {
        contentService.publish(id, false);
        return Result.ok();
    }
}
