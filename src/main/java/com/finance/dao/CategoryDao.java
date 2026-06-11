package com.finance.dao;

import com.finance.entity.Category;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
/**
 * 分类表 category 的数据库访问对象，负责分类 CRUD 和默认分类初始化。
 */
public class CategoryDao {
    private final JdbcTemplate jdbcTemplate;

    /**
     * 将 category 查询结果映射为 Category 实体。
     */
    private final RowMapper<Category> rowMapper = (rs, rowNum) -> {
        Category category = new Category();
        category.setId(rs.getLong("id"));
        category.setUserId(rs.getLong("user_id"));
        category.setName(rs.getString("name"));
        category.setType(rs.getInt("type"));
        category.setIcon(rs.getString("icon"));
        category.setSortOrder(rs.getInt("sort_order"));
        category.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return category;
    };

    public CategoryDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询用户的全部分类，并按类型和排序值排列。
     */
    public List<Category> findAll(Long userId) {
        return jdbcTemplate.query(
                "SELECT * FROM category WHERE user_id = ? ORDER BY type, sort_order, id",
                rowMapper,
                userId
        );
    }

    /**
     * 查询用户指定收入/支出类型下的分类。
     */
    public List<Category> findByType(Long userId, int type) {
        return jdbcTemplate.query(
                "SELECT * FROM category WHERE user_id = ? AND type = ? ORDER BY sort_order, id",
                rowMapper,
                userId,
                type
        );
    }

    /**
     * 根据用户和分类 id 查询单个分类。
     */
    public Optional<Category> findById(Long userId, Long id) {
        List<Category> categories = jdbcTemplate.query(
                "SELECT * FROM category WHERE user_id = ? AND id = ?",
                rowMapper,
                userId,
                id
        );
        return categories.stream().findFirst();
    }

    /**
     * 新增分类并回填数据库生成的主键。
     */
    public Category create(Long userId, Category category) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO category (user_id, name, type, icon, sort_order) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, userId);
            ps.setString(2, category.getName());
            ps.setInt(3, category.getType());
            ps.setString(4, category.getIcon());
            ps.setInt(5, category.getSortOrder());
            return ps;
        }, keyHolder);
        category.setUserId(userId);
        Number key = generatedId(keyHolder);
        if (key != null) {
            category.setId(key.longValue());
        }
        return category;
    }

    /**
     * 更新用户自己的分类信息。
     */
    public int update(Long userId, Category category) {
        return jdbcTemplate.update(
                "UPDATE category SET name = ?, type = ?, icon = ?, sort_order = ? WHERE user_id = ? AND id = ?",
                category.getName(),
                category.getType(),
                category.getIcon(),
                category.getSortOrder(),
                userId,
                category.getId()
        );
    }

    /**
     * 删除用户自己的分类。
     */
    public int delete(Long userId, Long id) {
        return jdbcTemplate.update("DELETE FROM category WHERE user_id = ? AND id = ?", userId, id);
    }

    /**
     * 统计指定分类下的收支记录数量，用于删除前保护。
     */
    public long countTransactions(Long userId, Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `transaction` WHERE user_id = ? AND category_id = ?",
                Long.class,
                userId,
                id
        );
    }

    /**
     * 新用户注册后批量创建默认收入和支出分类。
     */
    public void createDefaultsForUser(Long userId) {
        List<DefaultCategory> defaults = Arrays.asList(
                new DefaultCategory("工资", 1, "briefcase", 1),
                new DefaultCategory("奖金", 1, "gift", 2),
                new DefaultCategory("兼职", 1, "clock", 3),
                new DefaultCategory("理财", 1, "chart", 4),
                new DefaultCategory("餐饮", 2, "food", 1),
                new DefaultCategory("交通", 2, "car", 2),
                new DefaultCategory("购物", 2, "cart", 3),
                new DefaultCategory("娱乐", 2, "game", 4),
                new DefaultCategory("医疗", 2, "medical", 5)
        );
        jdbcTemplate.batchUpdate(
                "INSERT INTO category (user_id, name, type, icon, sort_order) VALUES (?, ?, ?, ?, ?)",
                defaults,
                defaults.size(),
                (ps, category) -> {
                    ps.setLong(1, userId);
                    ps.setString(2, category.name());
                    ps.setInt(3, category.type());
                    ps.setString(4, category.icon());
                    ps.setInt(5, category.sortOrder());
                }
        );
    }

    /**
     * 默认分类配置项。
     */
    private record DefaultCategory(String name, int type, String icon, int sortOrder) {
    }

    /**
     * 从 JDBC KeyHolder 中兼容获取自增主键。
     */
    private Number generatedId(KeyHolder keyHolder) {
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys == null || keys.isEmpty()) {
            return null;
        }
        Object id = keys.getOrDefault("id", keys.get("ID"));
        if (id instanceof Number number) {
            return number;
        }
        return keys.values().stream()
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .findFirst()
                .orElse(null);
    }
}
