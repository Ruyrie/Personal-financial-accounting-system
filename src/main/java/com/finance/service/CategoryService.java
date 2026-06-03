package com.finance.service;

import com.finance.dao.CategoryDao;
import com.finance.entity.Category;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryDao categoryDao;
    private final UserService userService;

    public CategoryService(CategoryDao categoryDao, UserService userService) {
        this.categoryDao = categoryDao;
        this.userService = userService;
    }

    public List<Category> findAll() {
        return categoryDao.findAll(userService.currentUserId());
    }

    public List<Category> findByType(int type) {
        return categoryDao.findByType(userService.currentUserId(), type);
    }

    public Category findById(Long id) {
        return categoryDao.findById(userService.currentUserId(), id)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));
    }

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

    public void delete(Long id) {
        Long userId = userService.currentUserId();
        if (categoryDao.countTransactions(userId, id) > 0) {
            throw new IllegalArgumentException("该分类已有收支记录，不能删除");
        }
        if (categoryDao.delete(userId, id) == 0) {
            throw new IllegalArgumentException("分类不存在");
        }
    }

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
