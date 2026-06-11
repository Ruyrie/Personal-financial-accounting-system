package com.finance.dto;

import java.math.BigDecimal;

/**
 * 分类统计结果：分类 id、名称、金额和该分类占总额的百分比。
 */
public record CategoryStats(Long categoryId, String categoryName, BigDecimal amount, BigDecimal percent) {
}
