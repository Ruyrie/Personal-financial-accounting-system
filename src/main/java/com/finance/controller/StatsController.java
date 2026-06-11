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
/**
 * 提供统计类 JSON API，方便图表或外部调用获取月度统计数据。
 */
public class StatsController {
    private final TransactionService transactionService;

    public StatsController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/api/stats/monthly")
    /**
     * 返回指定月份的收入、支出和结余统计。
     */
    public MonthlyStats monthly(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return transactionService.monthlyStats(month == null ? YearMonth.now() : month);
    }

    @GetMapping("/api/stats/by-category")
    /**
     * 返回指定月份的支出分类占比数据。
     */
    public List<CategoryStats> byCategory(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return transactionService.expenseStatsByCategory(month == null ? YearMonth.now() : month);
    }
}
