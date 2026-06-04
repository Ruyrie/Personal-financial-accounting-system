package com.finance.controller;

import com.finance.service.TransactionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;

@Controller
public class DashboardController {
    private final TransactionService transactionService;

    public DashboardController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/")
    public String index(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month, Model model) {
        YearMonth selectedMonth = month == null ? YearMonth.now() : month;
        model.addAttribute("month", selectedMonth);
        model.addAttribute("stats", transactionService.monthlyStats(selectedMonth));
        model.addAttribute("recentTransactions", transactionService.findRecent(5));
        model.addAttribute("categoryStats", transactionService.expenseStatsByCategory(selectedMonth));
        model.addAttribute("incomeCategoryStats", transactionService.incomeStatsByCategory(selectedMonth));
        return "index";
    }
}
