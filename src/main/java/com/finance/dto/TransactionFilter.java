package com.finance.dto;

import java.time.YearMonth;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * 收支记录筛选条件，接收页面和 API 的类型、分类、月份、关键词与分页参数。
 */
public class TransactionFilter {
    /** 收支类型：1 表示收入，2 表示支出，null 表示全部。 */
    private Integer type;
    /** 分类 id，null 表示不过滤分类。 */
    private Long categoryId;
    /** 按 yyyy-MM 接收月份筛选条件。 */
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth month;
    /** 备注或分类名称关键词。 */
    private String keyword;
    /** 当前页码，从 1 开始。 */
    private int page = 1;
    /** 每页条数，最大限制为 100。 */
    private int size = 10;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public YearMonth getMonth() {
        return month;
    }

    public void setMonth(YearMonth month) {
        this.month = month;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(1, page);
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = Math.min(100, Math.max(1, size));
    }

    /**
     * 计算 SQL 分页查询的 OFFSET。
     */
    public int offset() {
        return (page - 1) * size;
    }
}
