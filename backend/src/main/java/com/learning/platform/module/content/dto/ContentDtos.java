package com.learning.platform.module.content.dto;

import com.learning.platform.module.content.entity.ContentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class ContentDtos {

    private ContentDtos() {
    }

    /**
     * 新建 / 修改内容。
     * fileTaskId / coverTaskId 为已上传完成（COMPLETED）的上传任务 ID；修改时为空表示不替换。
     * removeCover=true 表示删除已有封面；与 coverTaskId 同时出现时请求无效。
     * VIDEO / DOC 新建时必须带 fileTaskId，TEXT 新建时必须带 body。type 创建后不可修改。
     */
    public record SaveRequest(
            @NotNull(message = "分类不能为空") Long categoryId,
            @NotNull(message = "内容类型不能为空") ContentType type,
            @NotBlank(message = "标题不能为空") @Size(max = 100, message = "标题最多 100 个字") String title,
            @Size(max = 500, message = "简介最多 500 个字") String description,
            Integer sort,
            Long coverTaskId,
            Boolean removeCover,
            Long fileTaskId,
            @Min(value = 0, message = "时长不正确") Integer duration,
            @Size(max = 500_000, message = "正文过长") String body) {
    }

    public record FileView(String fileName, String mime, long fileSize, Integer duration,
                           /** 播放 / 在线预览地址（临时签名）。 */
                           String url,
                           /** 以附件方式下载的地址（仅文档）。 */
                           String downloadUrl) {
    }

    public record ListItem(Long id, Long categoryId, ContentType type, String title, String description,
                           String status, Integer sort, String coverUrl,
                           /** 视频没有上传封面时，H5 用该地址加载首帧作为列表封面。 */
                           String videoUrl,
                           /** VIDEO / DOC 的文件展示信息；TEXT 为空。 */
                           String fileName, String mime, Long fileSize, Integer duration,
                           LocalDateTime publishedAt, LocalDateTime updatedAt) {
    }

    public record Detail(Long id, Long categoryId, ContentType type, String title, String description,
                         String status, Integer sort, String coverUrl, FileView file, String body,
                         /** 当前用户的视频观看卡点（秒）；管理端或非视频为空。 */
                         Integer progressSeconds,
                         LocalDateTime publishedAt, LocalDateTime updatedAt) {
    }

    public record ProgressRequest(
            @NotNull(message = "观看进度不能为空")
            @Min(value = 0, message = "观看进度不能小于 0") Integer progressSeconds) {
    }

    public record ContinueWatching(Long contentId, String title, String description,
                                   String coverUrl, String videoUrl, Integer duration,
                                   Integer progressSeconds, Integer progressPercent,
                                   LocalDateTime watchedAt) {
    }
}
