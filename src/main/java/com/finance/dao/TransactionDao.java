package com.finance.dao;

import com.finance.dto.CategoryStats;
import com.finance.dto.MonthlyStats;
import com.finance.dto.TransactionFilter;
import com.finance.entity.Transaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class TransactionDao {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Transaction> rowMapper = (rs, rowNum) -> {
        Transaction transaction = new Transaction();
        transaction.setId(rs.getLong("id"));
        transaction.setUserId(rs.getLong("user_id"));
        transaction.setCategoryId(rs.getLong("category_id"));
        transaction.setCategoryName(rs.getString("category_name"));
        transaction.setAmount(rs.getBigDecimal("amount"));
        transaction.setType(rs.getInt("type"));
        transaction.setRecordDate(rs.getDate("record_date").toLocalDate());
        transaction.setRemark(rs.getString("remark"));
        transaction.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        transaction.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return transaction;
    };

    public TransactionDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Transaction> findRecent(Long userId, int limit) {
        return jdbcTemplate.query(
                baseSelect() + " WHERE t.user_id = ? ORDER BY t.record_date DESC, t.id DESC LIMIT ?",
                rowMapper,
                userId,
                limit
        );
    }

    public Optional<Transaction> findById(Long userId, Long id) {
        List<Transaction> results = jdbcTemplate.query(
                baseSelect() + " WHERE t.user_id = ? AND t.id = ?",
                rowMapper,
                userId,
                id
        );
        return results.stream().findFirst();
    }

    public List<Transaction> findPage(Long userId, TransactionFilter filter) {
        QueryParts parts = buildWhere(userId, filter);
        List<Object> params = new ArrayList<>(parts.params());
        params.add(filter.getSize());
        params.add(filter.offset());
        return jdbcTemplate.query(
                baseSelect() + parts.where() + " ORDER BY t.record_date DESC, t.id DESC LIMIT ? OFFSET ?",
                rowMapper,
                params.toArray()
        );
    }

    public List<Transaction> findForExport(Long userId, TransactionFilter filter) {
        QueryParts parts = buildWhere(userId, filter);
        return jdbcTemplate.query(
                baseSelect() + parts.where() + " ORDER BY t.record_date DESC, t.id DESC",
                rowMapper,
                parts.params().toArray()
        );
    }

    public long count(Long userId, TransactionFilter filter) {
        QueryParts parts = buildWhere(userId, filter);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `transaction` t JOIN category c ON c.id = t.category_id AND c.user_id = t.user_id" + parts.where(),
                Long.class,
                parts.params().toArray()
        );
        return total == null ? 0 : total;
    }

    public Transaction create(Long userId, Transaction transaction) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO `transaction` (user_id, category_id, amount, type, record_date, remark) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, userId);
            ps.setLong(2, transaction.getCategoryId());
            ps.setBigDecimal(3, transaction.getAmount());
            ps.setInt(4, transaction.getType());
            ps.setDate(5, Date.valueOf(transaction.getRecordDate()));
            ps.setString(6, transaction.getRemark());
            return ps;
        }, keyHolder);
        transaction.setUserId(userId);
        Number key = generatedId(keyHolder);
        if (key != null) {
            transaction.setId(key.longValue());
        }
        return transaction;
    }

    public int update(Long userId, Transaction transaction) {
        return jdbcTemplate.update(
                "UPDATE `transaction` SET category_id = ?, amount = ?, type = ?, record_date = ?, remark = ? WHERE user_id = ? AND id = ?",
                transaction.getCategoryId(),
                transaction.getAmount(),
                transaction.getType(),
                Date.valueOf(transaction.getRecordDate()),
                transaction.getRemark(),
                userId,
                transaction.getId()
        );
    }

    public int delete(Long userId, Long id) {
        return jdbcTemplate.update("DELETE FROM `transaction` WHERE user_id = ? AND id = ?", userId, id);
    }

    public MonthlyStats monthlyStats(Long userId, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.plusMonths(1).atDay(1);
        return jdbcTemplate.queryForObject("""
                SELECT
                    COALESCE(SUM(CASE WHEN type = 1 THEN amount ELSE 0 END), 0) AS income,
                    COALESCE(SUM(CASE WHEN type = 2 THEN amount ELSE 0 END), 0) AS expense
                FROM `transaction`
                WHERE user_id = ? AND record_date >= ? AND record_date < ?
                """, (rs, rowNum) -> {
            BigDecimal income = rs.getBigDecimal("income");
            BigDecimal expense = rs.getBigDecimal("expense");
            return new MonthlyStats(income, expense, income.subtract(expense));
        }, userId, Date.valueOf(start), Date.valueOf(end));
    }

    public List<CategoryStats> expenseStatsByCategory(Long userId, YearMonth month) {
        return statsByCategory(userId, month, 2);
    }

    public List<CategoryStats> incomeStatsByCategory(Long userId, YearMonth month) {
        return statsByCategory(userId, month, 1);
    }

    private List<CategoryStats> statsByCategory(Long userId, YearMonth month, int type) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.plusMonths(1).atDay(1);
        List<CategoryStats> raw = jdbcTemplate.query("""
                SELECT c.id AS category_id, c.name AS category_name, SUM(t.amount) AS amount
                FROM `transaction` t
                JOIN category c ON c.id = t.category_id AND c.user_id = t.user_id
                WHERE t.user_id = ? AND t.type = ? AND t.record_date >= ? AND t.record_date < ?
                GROUP BY c.id, c.name
                ORDER BY amount DESC
                """, (rs, rowNum) -> new CategoryStats(
                rs.getLong("category_id"),
                rs.getString("category_name"),
                rs.getBigDecimal("amount"),
                BigDecimal.ZERO
        ), userId, type, Date.valueOf(start), Date.valueOf(end));

        BigDecimal total = raw.stream()
                .map(CategoryStats::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return raw;
        }
        return raw.stream()
                .map(stat -> new CategoryStats(
                        stat.categoryId(),
                        stat.categoryName(),
                        stat.amount(),
                        stat.amount().multiply(BigDecimal.valueOf(100)).divide(total, 2, java.math.RoundingMode.HALF_UP)
                ))
                .toList();
    }

    private String baseSelect() {
        return """
                SELECT t.*, c.name AS category_name
                FROM `transaction` t
                JOIN category c ON c.id = t.category_id AND c.user_id = t.user_id
                """;
    }

    private QueryParts buildWhere(Long userId, TransactionFilter filter) {
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        conditions.add("t.user_id = ?");
        params.add(userId);
        if (filter.getType() != null) {
            conditions.add("t.type = ?");
            params.add(filter.getType());
        }
        if (filter.getCategoryId() != null) {
            conditions.add("t.category_id = ?");
            params.add(filter.getCategoryId());
        }
        if (filter.getMonth() != null) {
            conditions.add("t.record_date >= ? AND t.record_date < ?");
            params.add(Date.valueOf(filter.getMonth().atDay(1)));
            params.add(Date.valueOf(filter.getMonth().plusMonths(1).atDay(1)));
        }
        if (StringUtils.hasText(filter.getKeyword())) {
            conditions.add("(t.remark LIKE ? OR c.name LIKE ?)");
            String keyword = "%" + filter.getKeyword().trim() + "%";
            params.add(keyword);
            params.add(keyword);
        }
        return new QueryParts(" WHERE " + String.join(" AND ", conditions), params);
    }

    private record QueryParts(String where, List<Object> params) {
    }

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
