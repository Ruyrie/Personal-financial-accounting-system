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
/**
 * 收支记录表 transaction 的数据库访问对象，封装列表、筛选、统计和增删改 SQL。
 */
public class TransactionDao {
    private final JdbcTemplate jdbcTemplate;

    /**
     * 将 transaction 与 category 联查结果映射为 Transaction 实体。
     */
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

    /**
     * 查询用户最近的收支记录。
     */
    public List<Transaction> findRecent(Long userId, int limit) {
        return jdbcTemplate.query(
                baseSelect() + " WHERE t.user_id = ? ORDER BY t.record_date DESC, t.id DESC LIMIT ?",
                rowMapper,
                userId,
                limit
        );
    }

    /**
     * 根据用户和记录 id 查询单条收支记录。
     */
    public Optional<Transaction> findById(Long userId, Long id) {
        List<Transaction> results = jdbcTemplate.query(
                baseSelect() + " WHERE t.user_id = ? AND t.id = ?",
                rowMapper,
                userId,
                id
        );
        return results.stream().findFirst();
    }

    /**
     * 按筛选条件分页查询收支记录。
     */
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

    /**
     * 按筛选条件查询导出用的全部收支记录，不分页。
     */
    public List<Transaction> findForExport(Long userId, TransactionFilter filter) {
        QueryParts parts = buildWhere(userId, filter);
        return jdbcTemplate.query(
                baseSelect() + parts.where() + " ORDER BY t.record_date DESC, t.id DESC",
                rowMapper,
                parts.params().toArray()
        );
    }

    /**
     * 统计筛选条件下的记录总数，用于分页。
     */
    public long count(Long userId, TransactionFilter filter) {
        QueryParts parts = buildWhere(userId, filter);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `transaction` t JOIN category c ON c.id = t.category_id AND c.user_id = t.user_id" + parts.where(),
                Long.class,
                parts.params().toArray()
        );
        return total == null ? 0 : total;
    }

    /**
     * 新增收支记录并回填数据库生成的主键。
     */
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

    /**
     * 更新用户自己的收支记录。
     */
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

    /**
     * 删除用户自己的收支记录。
     */
    public int delete(Long userId, Long id) {
        return jdbcTemplate.update("DELETE FROM `transaction` WHERE user_id = ? AND id = ?", userId, id);
    }

    /**
     * 汇总指定月份的收入、支出和结余。
     */
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

    /**
     * 查询指定月份支出分类统计。
     */
    public List<CategoryStats> expenseStatsByCategory(Long userId, YearMonth month) {
        return statsByCategory(userId, month, 2);
    }

    /**
     * 查询指定月份收入分类统计。
     */
    public List<CategoryStats> incomeStatsByCategory(Long userId, YearMonth month) {
        return statsByCategory(userId, month, 1);
    }

    /**
     * 按分类汇总指定月份金额，并计算各分类占比。
     */
    private List<CategoryStats> statsByCategory(Long userId, YearMonth month, int type) {
        // 月初作为闭区间起点。
        LocalDate start = month.atDay(1);
        // 下个月月初作为开区间终点，避免不同年份同月数据混在一起。
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

        // 先计算当前类型下的总金额，后续用于百分比换算。
        BigDecimal total = raw.stream()
                .map(CategoryStats::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return raw;
        }
        // 将每个分类金额除以总金额，得到该分类在本月收入/支出中的占比。
        return raw.stream()
                .map(stat -> new CategoryStats(
                        stat.categoryId(),
                        stat.categoryName(),
                        stat.amount(),
                        stat.amount().multiply(BigDecimal.valueOf(100)).divide(total, 2, java.math.RoundingMode.HALF_UP)
                ))
                .toList();
    }

    /**
     * 统一收支记录列表查询的基础联表 SQL。
     */
    private String baseSelect() {
        return """
                SELECT t.*, c.name AS category_name
                FROM `transaction` t
                JOIN category c ON c.id = t.category_id AND c.user_id = t.user_id
                """;
    }

    /**
     * 根据筛选条件构建 WHERE 子句和参数列表。
     */
    private QueryParts buildWhere(Long userId, TransactionFilter filter) {
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        // 所有查询都必须带 user_id，保证用户只能看到自己的收支记录。
        conditions.add("t.user_id = ?");
        // 与上面的 ? 占位符一一对应，JdbcTemplate 会做参数绑定，避免 SQL 注入。
        params.add(userId);
        if (filter.getType() != null) {
            // 按收支类型过滤：1 为收入，2 为支出。
            conditions.add("t.type = ?");
            params.add(filter.getType());
        }
        if (filter.getCategoryId() != null) {
            // 按分类过滤，例如只查看“工资”或“餐饮”分类。
            conditions.add("t.category_id = ?");
            params.add(filter.getCategoryId());
        }
        if (filter.getMonth() != null) {
            // 月份筛选使用 [本月1号, 下月1号) 范围，能精确区分年份和月份。
            conditions.add("t.record_date >= ? AND t.record_date < ?");
            params.add(Date.valueOf(filter.getMonth().atDay(1)));
            params.add(Date.valueOf(filter.getMonth().plusMonths(1).atDay(1)));
        }
        if (StringUtils.hasText(filter.getKeyword())) {
            // 关键词同时匹配备注和分类名称，支持用户按“午餐”“工资”等内容搜索。
            conditions.add("(t.remark LIKE ? OR c.name LIKE ?)");
            // trim 去掉前后空格；两侧加 % 表示模糊匹配。
            String keyword = "%" + filter.getKeyword().trim() + "%";
            params.add(keyword);
            params.add(keyword);
        }
        // 把条件用 AND 拼接成 WHERE 子句，并和参数一起返回给查询方法复用。
        return new QueryParts(" WHERE " + String.join(" AND ", conditions), params);
    }

    /**
     * 保存动态 SQL 条件片段和对应参数。
     */
    private record QueryParts(String where, List<Object> params) {
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
