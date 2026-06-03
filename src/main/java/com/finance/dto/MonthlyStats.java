package com.finance.dto;

import java.math.BigDecimal;

public record MonthlyStats(BigDecimal income, BigDecimal expense, BigDecimal balance) {
}
