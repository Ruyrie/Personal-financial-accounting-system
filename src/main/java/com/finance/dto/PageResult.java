package com.finance.dto;

import java.util.List;

/**
 * 通用分页结果，封装当前页数据、页码、页大小、总数和总页数。
 */
public record PageResult<T>(List<T> items, int page, int size, long total, int totalPages) {
}
