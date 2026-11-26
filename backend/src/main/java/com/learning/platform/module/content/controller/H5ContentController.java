package com.learning.platform.module.content.controller;

import com.learning.platform.common.PageResult;
import com.learning.platform.common.Result;
import com.learning.platform.module.content.dto.ContentDtos.Detail;
import com.learning.platform.module.content.dto.ContentDtos.ListItem;
import com.learning.platform.module.content.dto.ContentDtos.ContinueWatching;
import com.learning.platform.module.content.dto.ContentDtos.ProgressRequest;
import com.learning.platform.module.content.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/h5/api/contents")
@RequiredArgsConstructor
public class H5ContentController {

    private final ContentService contentService;

    @GetMapping
    public Result<PageResult<ListItem>> page(@RequestParam(required = false) Long categoryId,
                                             @RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "20") long size) {
        return Result.ok(contentService.h5Page(categoryId, page, size));
    }

    @GetMapping("/{id}")
    public Result<Detail> detail(@PathVariable Long id) {
        return Result.ok(contentService.h5Detail(id));
    }

    @GetMapping("/continue-watching")
    public Result<ContinueWatching> continueWatching() {
        return Result.ok(contentService.continueWatching());
    }

    @PutMapping("/{id}/progress")
    public Result<Void> saveProgress(@PathVariable Long id, @Valid @RequestBody ProgressRequest req) {
        contentService.saveProgress(id, req.progressSeconds());
        return Result.ok();
    }
}
