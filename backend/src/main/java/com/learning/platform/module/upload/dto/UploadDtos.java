package com.learning.platform.module.upload.dto;

import com.learning.platform.module.upload.entity.UploadPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class UploadDtos {

    private UploadDtos() {
    }

    public record InitRequest(
            @NotNull(message = "上传用途不能为空") UploadPurpose purpose,
            @NotBlank(message = "文件名不能为空") @Size(max = 200, message = "文件名过长") String fileName,
            @NotNull(message = "文件大小不能为空") @Positive(message = "文件大小不正确") Long fileSize) {
    }

    public record InitResponse(Long taskId, long partSize, int partCount) {
    }

    public record PartUrlsRequest(@NotEmpty(message = "分片号不能为空") List<Integer> partNumbers) {
    }

    public record PartUrl(int partNumber, String url) {
    }

    /** 断点续传：返回已上传完成的分片号。 */
    public record ProgressResponse(Long taskId, String status, long partSize, int partCount,
                                   List<Integer> uploadedParts) {
    }

    public record CompleteResponse(Long taskId, String fileName, long fileSize) {
    }
}
