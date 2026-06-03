package com.finance.dto;

import java.math.BigDecimal;

public record CategoryStats(Long categoryId, String categoryName, BigDecimal amount, BigDecimal percent) {
}
