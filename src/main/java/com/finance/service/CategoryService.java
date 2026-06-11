package com.finance.service;

import com.finance.dao.CategoryDao;
import com.finance.entity.Category;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
/**
 * 分类业务服务，负责按当前用户隔离分类数据，并执行分类保存和删除校验。
 */
public class CategoryService {
    private final CategoryDao categoryDao;
    private final UserService userService;

    public CategoryService(CategoryDao categoryDao, UserService userService) {
        this.categoryDao = categoryDao;
        this.userService = userService;
    }

    /**
     * 查询当前用户的全部分类。
     */
    public List<Category> findAll() {
        return categoryDao.findAll(userService.currentUserId());
    }

    /**
     * 查询当前用户指定类型的分类，type 为 1 表示收入，2 表示支出。
     */
    public List<Category> findByType(int type) {
        return categoryDao.findByType(userService.currentUserId(), type);
    }

    /**
     * 查询当前用户指定分类，不存在时抛出业务异常。
     */
    public Category findById(Long id) {
        return categoryDao.findById(userService.currentUserId(), id)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));
    }

    /**
     * 保存分类；没有 id 时新增，有 id 时更新。
     */
    public Category save(Category category) {
        validate(category);
        Long userId = userService.currentUserId();
        if (category.getId() == null) {
            return categoryDao.create(userId, category);
        }
        if (categoryDao.update(userId, category) == 0) {
            throw new IllegalArgumentException("分类不存在");
        }
        category.setUserId(userId);
        return category;
    }

    /**
     * 删除分类；若分类已有收支记录则禁止删除，避免历史记录失去分类。
     */
    public void delete(Long id) {
        Long userId = userService.currentUserId();
        if (categoryDao.countTransactions(userId, id) > 0) {
            throw new IllegalArgumentException("该分类已有收支记录，不能删除");
        }
        if (categoryDao.delete(userId, id) == 0) {
            throw new IllegalArgumentException("分类不存在");
        }
    }

    /**
     * 校验分类名称、类型，并补齐默认图标和排序值。
     */
    private void validate(Category category) {
        if (!StringUtils.hasText(category.getName())) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        if (category.getType() == null || (category.getType() != 1 && category.getType() != 2)) {
            throw new IllegalArgumentException("分类类型必须为收入或支出");
        }
        if (!StringUtils.hasText(category.getIcon())) {
            category.setIcon("tag");
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
    }
}
