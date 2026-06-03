package com.finance.controller;

import com.finance.dto.CategoryStats;
import com.finance.dto.MonthlyStats;
import com.finance.service.TransactionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
public class StatsController {
    private final TransactionService transactionService;

    public StatsController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/api/stats/monthly")
    public MonthlyStats monthly(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return transactionService.monthlyStats(month == null ? YearMonth.now() : month);
    }

    @GetMapping("/api/stats/by-category")
    public List<CategoryStats> byCategory(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return transactionService.expenseStatsByCategory(month == null ? YearMonth.now() : month);
    }
}
