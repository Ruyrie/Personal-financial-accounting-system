package com.finance.dao;

import com.finance.entity.AppUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
/**
 * 用户表 app_user 的数据库访问对象，封装用户查询、创建和资料更新 SQL。
 */
public class UserDao {
    private final JdbcTemplate jdbcTemplate;

    /**
     * 将 app_user 查询结果映射为 AppUser 实体。
     */
    private final RowMapper<AppUser> rowMapper = (rs, rowNum) -> {
        AppUser user = new AppUser();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setAvatarData(rs.getString("avatar_data"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return user;
    };

    public UserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 根据用户名查询用户，用于登录、注册校验和当前用户定位。
     */
    public Optional<AppUser> findByUsername(String username) {
        List<AppUser> users = jdbcTemplate.query(
                "SELECT * FROM app_user WHERE username = ?",
                rowMapper,
                username
        );
        return users.stream().findFirst();
    }

    /**
     * 判断用户名是否已经存在。
     */
    public boolean existsByUsername(String username) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE username = ?",
                Long.class,
                username
        );
        return count != null && count > 0;
    }

    /**
     * 更新用户头像 Data URL。
     */
    public void updateAvatar(Long id, String avatarData) {
        jdbcTemplate.update(
                "UPDATE app_user SET avatar_data = ? WHERE id = ?",
                avatarData,
                id
        );
    }

    /**
     * 根据用户名更新加密后的密码。
     */
    public int updatePasswordByUsername(String username, String password) {
        return jdbcTemplate.update(
                "UPDATE app_user SET password = ? WHERE username = ?",
                password,
                username
        );
    }

    /**
     * 创建新用户并回填数据库生成的主键。
     */
    public AppUser create(String username, String password) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO app_user (username, password) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, username);
            ps.setString(2, password);
            return ps;
        }, keyHolder);
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(password);
        Number key = generatedId(keyHolder);
        if (key != null) {
            user.setId(key.longValue());
        }
        return user;
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
