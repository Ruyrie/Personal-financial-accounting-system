package com.finance.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.Statement;

@Component
/**
 * 简单数据库迁移组件，用于兼容旧表结构缺少 avatar_data 字段的情况。
 */
public class DatabaseMigration {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    /**
     * 应用启动后检查 app_user 表，缺少头像字段时自动补列。
     */
    public void migrate() {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (ResultSet columns = connection.getMetaData().getColumns(
                    connection.getCatalog(),
                    null,
                    "app_user",
                    "avatar_data"
            )) {
                if (!columns.next()) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute("ALTER TABLE app_user ADD COLUMN avatar_data MEDIUMTEXT");
                    }
                }
            }
            return null;
        });
    }
}
