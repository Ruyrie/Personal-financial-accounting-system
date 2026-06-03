package com.finance.dto;

import java.time.YearMonth;

import org.springframework.format.annotation.DateTimeFormat;

public class TransactionFilter {
    private Integer type;
    private Long categoryId;
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth month;
    private String keyword;
    private int page = 1;
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

    public int offset() {
        return (page - 1) * size;
    }
}
