package com.learning.platform.module.upload.controller;

import com.learning.platform.common.Result;
import com.learning.platform.module.upload.dto.UploadDtos.CompleteResponse;
import com.learning.platform.module.upload.dto.UploadDtos.InitRequest;
import com.learning.platform.module.upload.dto.UploadDtos.InitResponse;
import com.learning.platform.module.upload.dto.UploadDtos.PartUrl;
import com.learning.platform.module.upload.dto.UploadDtos.PartUrlsRequest;
import com.learning.platform.module.upload.dto.UploadDtos.ProgressResponse;
import com.learning.platform.module.upload.service.UploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/api/uploads")
@RequiredArgsConstructor
public class AdminUploadController {

    private final UploadService uploadService;

    @PostMapping
    public Result<InitResponse> init(@Valid @RequestBody InitRequest req) {
        return Result.ok(uploadService.init(req));
    }

    @PostMapping("/{id}/part-urls")
    public Result<List<PartUrl>> partUrls(@PathVariable Long id, @Valid @RequestBody PartUrlsRequest req) {
        return Result.ok(uploadService.signPartUrls(id, req.partNumbers()));
    }

    @GetMapping("/{id}")
    public Result<ProgressResponse> progress(@PathVariable Long id) {
        return Result.ok(uploadService.progress(id));
    }

    @PostMapping("/{id}/complete")
    public Result<CompleteResponse> complete(@PathVariable Long id) {
        return Result.ok(uploadService.complete(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> abort(@PathVariable Long id) {
        uploadService.abort(id);
        return Result.ok();
    }
}
