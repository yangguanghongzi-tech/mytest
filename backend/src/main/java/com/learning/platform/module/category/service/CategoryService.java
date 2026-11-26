package com.learning.platform.module.category.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.learning.platform.common.BizException;
import com.learning.platform.module.category.dto.CategoryDtos.Node;
import com.learning.platform.module.category.dto.CategoryDtos.SaveRequest;
import com.learning.platform.module.category.entity.Category;
import com.learning.platform.module.category.mapper.CategoryMapper;
import com.learning.platform.module.content.entity.Content;
import com.learning.platform.module.content.mapper.ContentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 分类管理。所有查询都会被租户插件自动加上 tenant_id 条件，这里不需要手动处理。 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final ContentMapper contentMapper;

    public List<Node> tree(boolean enabledOnly) {
        List<Category> all = categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .eq(enabledOnly, Category::getStatus, Category.ENABLED)
                .orderByAsc(Category::getSort, Category::getId));

        Map<Long, Node> roots = new LinkedHashMap<>();
        for (Category c : all) {
            if (c.getParentId() == Category.ROOT) {
                roots.put(c.getId(), toNode(c));
            }
        }
        for (Category c : all) {
            Node parent = roots.get(c.getParentId());
            if (parent != null) {
                parent.children().add(toNode(c));
            }
        }
        return new ArrayList<>(roots.values());
    }

    @Transactional
    public Long create(SaveRequest req) {
        Category c = new Category();
        c.setParentId(validParent(req.parentId(), null));
        c.setName(req.name().trim());
        c.setSort(req.sort() == null ? 0 : req.sort());
        c.setStatus(req.status() == null ? Category.ENABLED : req.status());
        categoryMapper.insert(c);
        return c.getId();
    }

    @Transactional
    public void update(Long id, SaveRequest req) {
        Category c = require(id);
        long newParent = validParent(req.parentId(), c);
        c.setParentId(newParent);
        c.setName(req.name().trim());
        if (req.sort() != null) {
            c.setSort(req.sort());
        }
        if (req.status() != null) {
            c.setStatus(req.status());
        }
        categoryMapper.updateById(c);
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        if (categoryMapper.exists(Wrappers.<Category>lambdaQuery().eq(Category::getParentId, id))) {
            throw BizException.conflict("请先删除子分类");
        }
        if (contentMapper.exists(Wrappers.<Content>lambdaQuery().eq(Content::getCategoryId, id))) {
            throw BizException.conflict("分类下还有内容，不能删除");
        }
        categoryMapper.deleteById(id);
    }

    /** 查询当前租户下的分类，不存在（包括属于其他租户）时抛 404。 */
    public Category require(Long id) {
        Category c = id == null ? null : categoryMapper.selectById(id);
        if (c == null) {
            throw BizException.notFound("分类不存在");
        }
        return c;
    }

    /** 返回分类本身及其子分类的 ID，用于按一级分类查询内容。 */
    public List<Long> selfAndChildrenIds(Long id) {
        List<Long> ids = new ArrayList<>();
        ids.add(id);
        categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                        .select(Category::getId)
                        .eq(Category::getParentId, id))
                .forEach(c -> ids.add(c.getId()));
        return ids;
    }

    /** H5 可见的分类 ID：启用的一级分类，以及启用的一级分类下启用的二级分类。 */
    public Set<Long> visibleIds() {
        Set<Long> ids = new HashSet<>();
        for (Node root : tree(true)) {
            ids.add(root.id());
            root.children().forEach(c -> ids.add(c.id()));
        }
        return ids;
    }

    /** H5 按分类查内容：分类不可见时返回 404；一级分类会包含其可见的子分类。 */
    public List<Long> visibleSelfAndChildrenIds(Long id) {
        for (Node root : tree(true)) {
            if (root.id().equals(id)) {
                List<Long> ids = new ArrayList<>();
                ids.add(id);
                root.children().forEach(c -> ids.add(c.id()));
                return ids;
            }
            for (Node child : root.children()) {
                if (child.id().equals(id)) {
                    return List.of(id);
                }
            }
        }
        throw BizException.notFound("分类不存在");
    }

    private long validParent(Long parentId, Category self) {
        if (parentId == null || parentId == Category.ROOT) {
            return Category.ROOT;
        }
        if (self != null) {
            if (Objects.equals(parentId, self.getId())) {
                throw BizException.badRequest("不能把自己设为上级分类");
            }
            boolean hasChildren = categoryMapper.exists(
                    Wrappers.<Category>lambdaQuery().eq(Category::getParentId, self.getId()));
            if (hasChildren) {
                throw BizException.badRequest("该分类下有子分类，不能设为二级分类");
            }
        }
        Category parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw BizException.badRequest("上级分类不存在");
        }
        if (parent.getParentId() != Category.ROOT) {
            throw BizException.badRequest("最多支持两级分类");
        }
        return parentId;
    }

    private static Node toNode(Category c) {
        return new Node(c.getId(), c.getParentId(), c.getName(), c.getSort(), c.getStatus());
    }
}
