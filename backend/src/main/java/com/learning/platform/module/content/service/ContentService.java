package com.learning.platform.module.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learning.platform.common.BizException;
import com.learning.platform.common.PageResult;
import com.learning.platform.auth.TenantContext;
import com.learning.platform.module.content.dto.ContentDtos.ContinueWatching;
import com.learning.platform.module.category.service.CategoryService;
import com.learning.platform.module.content.ContentProperties;
import com.learning.platform.module.content.dto.ContentDtos.Detail;
import com.learning.platform.module.content.dto.ContentDtos.FileView;
import com.learning.platform.module.content.dto.ContentDtos.ListItem;
import com.learning.platform.module.content.dto.ContentDtos.SaveRequest;
import com.learning.platform.module.content.entity.Content;
import com.learning.platform.module.content.entity.ContentType;
import com.learning.platform.module.content.entity.MediaFile;
import com.learning.platform.module.content.entity.TextBody;
import com.learning.platform.module.content.entity.WatchProgress;
import com.learning.platform.module.content.mapper.ContentMapper;
import com.learning.platform.module.content.mapper.MediaFileMapper;
import com.learning.platform.module.content.mapper.TextBodyMapper;
import com.learning.platform.module.content.mapper.WatchProgressMapper;
import com.learning.platform.module.upload.entity.UploadPurpose;
import com.learning.platform.module.upload.entity.UploadTask;
import com.learning.platform.module.upload.service.UploadService;
import com.learning.platform.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 内容管理（视频 / 文档 / 文本）。租户条件由 MyBatis-Plus 租户插件自动追加。
 * 删除或替换文件时只做逻辑删除，OBS 中的旧对象暂不清理（二期可加定时清理）。
 */
@Service
@RequiredArgsConstructor
public class ContentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ContentMapper contentMapper;
    private final MediaFileMapper mediaFileMapper;
    private final TextBodyMapper textBodyMapper;
    private final WatchProgressMapper watchProgressMapper;
    private final CategoryService categoryService;
    private final UploadService uploadService;
    private final StorageService storage;
    private final ContentProperties props;

    // ---------------- 管理端 ----------------

    public PageResult<ListItem> adminPage(Long categoryId, ContentType type, String status, String keyword,
                                          long page, long size) {
        LambdaQueryWrapper<Content> q = Wrappers.<Content>lambdaQuery()
                .eq(type != null, Content::getType, type == null ? null : type.name())
                .eq(StringUtils.hasText(status), Content::getStatus, status)
                .like(StringUtils.hasText(keyword), Content::getTitle, keyword == null ? null : keyword.trim())
                .orderByAsc(Content::getSort)
                .orderByDesc(Content::getId);
        if (categoryId != null) {
            q.in(Content::getCategoryId, categoryService.selfAndChildrenIds(categoryId));
        }
        return page(q, page, size);
    }

    public Detail adminDetail(Long id) {
        return toDetail(require(id), null);
    }

    @Transactional
    public Long create(SaveRequest req) {
        categoryService.require(req.categoryId());
        Content c = new Content();
        c.setType(req.type().name());
        c.setStatus(Content.DRAFT);
        applyFields(c, req);
        contentMapper.insert(c);
        saveBody(c, req, true);
        return c.getId();
    }

    @Transactional
    public void update(Long id, SaveRequest req) {
        Content c = require(id);
        if (!c.getType().equals(req.type().name())) {
            throw BizException.badRequest("内容类型不能修改");
        }
        categoryService.require(req.categoryId());
        applyFields(c, req);
        contentMapper.updateById(c);
        saveBody(c, req, false);
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        contentMapper.deleteById(id);
        mediaFileMapper.delete(Wrappers.<MediaFile>lambdaQuery().eq(MediaFile::getContentId, id));
        textBodyMapper.delete(Wrappers.<TextBody>lambdaQuery().eq(TextBody::getContentId, id));
    }

    @Transactional
    public void publish(Long id, boolean published) {
        Content c = require(id);
        c.setStatus(published ? Content.PUBLISHED : Content.DRAFT);
        if (published) {
            c.setPublishedAt(LocalDateTime.now());
        }
        contentMapper.updateById(c);
    }

    // ---------------- H5 ----------------

    /** 只返回已发布、且所在分类（及上级分类）均为启用状态的内容。categoryId 为空时返回全部可见内容。 */
    public PageResult<ListItem> h5Page(Long categoryId, long page, long size) {
        Collection<Long> ids = categoryId == null
                ? categoryService.visibleIds()
                : categoryService.visibleSelfAndChildrenIds(categoryId);
        if (ids.isEmpty()) {
            return new PageResult<>(0, page, size, List.of());
        }
        LambdaQueryWrapper<Content> q = Wrappers.<Content>lambdaQuery()
                .eq(Content::getStatus, Content.PUBLISHED)
                .in(Content::getCategoryId, ids)
                .orderByAsc(Content::getSort)
                .orderByDesc(Content::getPublishedAt);
        return page(q, page, size);
    }

    public Detail h5Detail(Long id) {
        Content c = require(id);
        if (!Content.PUBLISHED.equals(c.getStatus()) || !categoryService.visibleIds().contains(c.getCategoryId())) {
            throw BizException.notFound("内容不存在");
        }
        Integer progress = ContentType.VIDEO.name().equals(c.getType()) ? currentProgress(c.getId()) : null;
        return toDetail(c, progress);
    }

    /** 保存当前用户的视频播放卡点。进度不得超出后台记录的视频总时长。 */
    @Transactional
    public void saveProgress(Long contentId, Integer progressSeconds) {
        Content c = requireVisibleVideo(contentId);
        MediaFile file = currentFile(c.getId());
        if (file == null) {
            throw BizException.notFound("视频文件不存在");
        }
        int seconds = Math.max(0, progressSeconds == null ? 0 : progressSeconds);
        if (file.getDuration() != null && file.getDuration() > 0) {
            seconds = Math.min(seconds, file.getDuration());
        }
        String userId = TenantContext.requireUserId();
        WatchProgress progress = watchProgressMapper.selectOne(Wrappers.<WatchProgress>lambdaQuery()
                .eq(WatchProgress::getUserId, userId)
                .eq(WatchProgress::getContentId, contentId));
        if (progress == null) {
            progress = new WatchProgress();
            progress.setUserId(userId);
            progress.setContentId(contentId);
            progress.setProgressSeconds(seconds);
            watchProgressMapper.insert(progress);
        } else {
            // 浏览器在视频元数据尚未加载、页面刷新或组件销毁时可能短暂上报 0。
            // 0 不是“播放完成”的标志，不能清除已有的有效卡点；播放完成由接近视频总时长的值表示。
            if (seconds == 0 && progress.getProgressSeconds() != null && progress.getProgressSeconds() > 0) {
                return;
            }
            progress.setProgressSeconds(seconds);
            watchProgressMapper.updateById(progress);
        }
    }

    /** 返回当前用户最近一条未看完、且仍可见的视频。 */
    public ContinueWatching continueWatching() {
        String userId = TenantContext.requireUserId();
        List<WatchProgress> recent = watchProgressMapper.selectList(Wrappers.<WatchProgress>lambdaQuery()
                .eq(WatchProgress::getUserId, userId)
                .gt(WatchProgress::getProgressSeconds, 0)
                .orderByDesc(WatchProgress::getUpdatedAt)
                .last("LIMIT 20"));
        Collection<Long> visibleIds = categoryService.visibleIds();
        for (WatchProgress progress : recent) {
            Content c = contentMapper.selectById(progress.getContentId());
            if (c == null || !Content.PUBLISHED.equals(c.getStatus())
                    || !ContentType.VIDEO.name().equals(c.getType())
                    || !visibleIds.contains(c.getCategoryId())) {
                continue;
            }
            MediaFile file = currentFile(c.getId());
            if (file == null) continue;
            int duration = file.getDuration() == null ? 0 : file.getDuration();
            int seconds = Math.max(0, progress.getProgressSeconds());
            if (duration > 0 && seconds >= duration) continue;
            int percent = duration > 0 ? Math.min(99, Math.max(1,
                    (int) Math.floor(seconds * 100.0 / duration))) : 0;
            return new ContinueWatching(c.getId(), c.getTitle(), c.getDescription(),
                    signedUrl(c.getCoverKey()), storage.signGetUrl(file.getObjectKey(), props.getUrlExpiresSeconds(), null),
                    file.getDuration(), seconds, percent, progress.getUpdatedAt());
        }
        return null;
    }

    // ---------------- 内部 ----------------

    private Content require(Long id) {
        Content c = id == null ? null : contentMapper.selectById(id);
        if (c == null) {
            throw BizException.notFound("内容不存在");
        }
        return c;
    }

    private void applyFields(Content c, SaveRequest req) {
        if (Boolean.TRUE.equals(req.removeCover()) && req.coverTaskId() != null) {
            throw BizException.badRequest("不能同时删除和替换封面");
        }
        c.setCategoryId(req.categoryId());
        c.setTitle(req.title().trim());
        c.setDescription(req.description());
        if (req.sort() != null || c.getSort() == null) {
            c.setSort(req.sort() == null ? 0 : req.sort());
        }
        if (req.coverTaskId() != null) {
            c.setCoverKey(uploadService.consume(req.coverTaskId(), UploadPurpose.COVER).getObjectKey());
        } else if (Boolean.TRUE.equals(req.removeCover())) {
            c.setCoverKey(null);
        }
    }

    private void saveBody(Content c, SaveRequest req, boolean creating) {
        ContentType type = ContentType.valueOf(c.getType());
        if (type == ContentType.TEXT) {
            if (!StringUtils.hasText(req.body())) {
                if (creating) {
                    throw BizException.badRequest("正文不能为空");
                }
                return;
            }
            TextBody tb = textBodyMapper.selectOne(
                    Wrappers.<TextBody>lambdaQuery().eq(TextBody::getContentId, c.getId()));
            String body = HtmlSanitizer.clean(req.body());
            if (tb == null) {
                tb = new TextBody();
                tb.setContentId(c.getId());
                tb.setBody(body);
                textBodyMapper.insert(tb);
            } else {
                tb.setBody(body);
                textBodyMapper.updateById(tb);
            }
            return;
        }

        if (req.fileTaskId() == null) {
            if (creating) {
                throw BizException.badRequest(type == ContentType.VIDEO ? "请上传视频" : "请上传文档");
            }
            if (req.duration() != null) {
                MediaFile mf = currentFile(c.getId());
                if (mf != null) {
                    mf.setDuration(req.duration());
                    mediaFileMapper.updateById(mf);
                }
            }
            return;
        }
        UploadTask task = uploadService.consume(req.fileTaskId(), type.filePurpose());
        mediaFileMapper.delete(Wrappers.<MediaFile>lambdaQuery().eq(MediaFile::getContentId, c.getId()));
        MediaFile mf = new MediaFile();
        mf.setContentId(c.getId());
        mf.setObjectKey(task.getObjectKey());
        mf.setFileName(task.getFileName());
        mf.setMime(task.getMime());
        mf.setFileSize(task.getFileSize());
        mf.setDuration(req.duration());
        mediaFileMapper.insert(mf);
    }

    private MediaFile currentFile(Long contentId) {
        return mediaFileMapper.selectOne(Wrappers.<MediaFile>lambdaQuery()
                .eq(MediaFile::getContentId, contentId)
                .orderByDesc(MediaFile::getId)
                .last("LIMIT 1"));
    }

    private PageResult<ListItem> page(LambdaQueryWrapper<Content> q, long page, long size) {
        long p = Math.max(1, page);
        long s = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        Page<Content> result = contentMapper.selectPage(Page.of(p, s), q);

        List<Long> ids = result.getRecords().stream().map(Content::getId).toList();
        Map<Long, MediaFile> files = ids.isEmpty() ? Map.of()
                : mediaFileMapper.selectList(Wrappers.<MediaFile>lambdaQuery().in(MediaFile::getContentId, ids))
                .stream().collect(Collectors.toMap(MediaFile::getContentId, Function.identity(), (a, b) -> b));

        return PageResult.of(result, c -> {
            MediaFile mf = files.get(c.getId());
            ContentType type = ContentType.valueOf(c.getType());
            String videoUrl = type == ContentType.VIDEO && mf != null
                    ? storage.signGetUrl(mf.getObjectKey(), props.getUrlExpiresSeconds(), null) : null;
            return new ListItem(c.getId(), c.getCategoryId(), type, c.getTitle(),
                    c.getDescription(), c.getStatus(), c.getSort(), signedUrl(c.getCoverKey()), videoUrl,
                    mf == null ? null : mf.getFileName(), mf == null ? null : mf.getMime(),
                    mf == null ? null : mf.getFileSize(), mf == null ? null : mf.getDuration(),
                    c.getPublishedAt(), c.getUpdatedAt());
        });
    }

    private Detail toDetail(Content c, Integer progressSeconds) {
        ContentType type = ContentType.valueOf(c.getType());
        FileView file = null;
        String body = null;
        if (type == ContentType.TEXT) {
            TextBody tb = textBodyMapper.selectOne(
                    Wrappers.<TextBody>lambdaQuery().eq(TextBody::getContentId, c.getId()));
            body = tb == null ? null : tb.getBody();
        } else {
            MediaFile mf = currentFile(c.getId());
            if (mf != null) {
                long exp = props.getUrlExpiresSeconds();
                String download = type == ContentType.DOC
                        ? storage.signGetUrl(mf.getObjectKey(), exp, mf.getFileName()) : null;
                file = new FileView(mf.getFileName(), mf.getMime(), mf.getFileSize(), mf.getDuration(),
                        storage.signGetUrl(mf.getObjectKey(), exp, null), download);
            }
        }
        return new Detail(c.getId(), c.getCategoryId(), type, c.getTitle(), c.getDescription(), c.getStatus(),
                c.getSort(), signedUrl(c.getCoverKey()), file, body, progressSeconds,
                c.getPublishedAt(), c.getUpdatedAt());
    }

    private Content requireVisibleVideo(Long id) {
        Content c = require(id);
        if (!Content.PUBLISHED.equals(c.getStatus()) || !ContentType.VIDEO.name().equals(c.getType())
                || !categoryService.visibleIds().contains(c.getCategoryId())) {
            throw BizException.notFound("视频不存在");
        }
        return c;
    }

    private Integer currentProgress(Long contentId) {
        WatchProgress progress = watchProgressMapper.selectOne(Wrappers.<WatchProgress>lambdaQuery()
                .eq(WatchProgress::getUserId, TenantContext.requireUserId())
                .eq(WatchProgress::getContentId, contentId));
        return progress == null ? 0 : progress.getProgressSeconds();
    }

    private String signedUrl(String key) {
        return StringUtils.hasText(key) ? storage.signGetUrl(key, props.getUrlExpiresSeconds(), null) : null;
    }
}
