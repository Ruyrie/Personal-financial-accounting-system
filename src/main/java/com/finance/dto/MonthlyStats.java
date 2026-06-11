package com.finance.dto;

import java.math.BigDecimal;

/**
 * 月度汇总结果：收入、支出和结余。
 */
public record MonthlyStats(BigDecimal income, BigDecimal expense, BigDecimal balance) {
}
