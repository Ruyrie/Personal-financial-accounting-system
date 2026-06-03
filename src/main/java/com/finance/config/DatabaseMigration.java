package com.finance.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.Statement;

@Component
public class DatabaseMigration {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
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
