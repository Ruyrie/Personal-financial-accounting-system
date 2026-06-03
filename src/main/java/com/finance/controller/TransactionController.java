package com.finance.controller;

import com.finance.dto.PageResult;
import com.finance.dto.TransactionFilter;
import com.finance.entity.Transaction;
import com.finance.service.CategoryService;
import com.finance.service.TransactionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;

@Controller
public class TransactionController {
    private final TransactionService transactionService;
    private final CategoryService categoryService;

    public TransactionController(TransactionService transactionService, CategoryService categoryService) {
        this.transactionService = transactionService;
        this.categoryService = categoryService;
    }

    @GetMapping("/transactions")
    public String list(@ModelAttribute TransactionFilter filter, Model model) {
        PageResult<Transaction> page = transactionService.findPage(filter);
        model.addAttribute("page", page);
        model.addAttribute("filter", filter);
        model.addAttribute("categories", categoryService.findAll());
        return "transaction/list";
    }

    @GetMapping("/transactions/export")
    public void export(@ModelAttribute TransactionFilter filter, HttpServletResponse response) throws IOException {
        byte[] bytes = transactionService.exportExcel(filter);
        String filename = URLEncoder.encode("收支记录.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }

    @GetMapping("/transactions/new")
    public String newForm(Model model) {
        Transaction transaction = new Transaction();
        transaction.setType(2);
        transaction.setRecordDate(LocalDate.now());
        model.addAttribute("transaction", transaction);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("mode", "create");
        return "transaction/form";
    }

    @GetMapping("/transactions/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("transaction", transactionService.findById(id));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("mode", "edit");
        return "transaction/form";
    }

    @PostMapping("/transactions")
    public String create(@ModelAttribute Transaction transaction, RedirectAttributes redirectAttributes) {
        transactionService.save(transaction);
        redirectAttributes.addFlashAttribute("message", "收支记录已新增");
        return "redirect:/transactions";
    }

    @PostMapping("/transactions/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Transaction transaction, RedirectAttributes redirectAttributes) {
        transaction.setId(id);
        transactionService.save(transaction);
        redirectAttributes.addFlashAttribute("message", "收支记录已更新");
        return "redirect:/transactions";
    }

    @PostMapping("/transactions/{id}/delete")
    public String deletePage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        transactionService.delete(id);
        redirectAttributes.addFlashAttribute("message", "收支记录已删除");
        return "redirect:/transactions";
    }

    @GetMapping("/api/transactions")
    @ResponseBody
    public PageResult<Transaction> apiList(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        TransactionFilter filter = new TransactionFilter();
        filter.setType(type);
        filter.setCategoryId(categoryId);
        filter.setMonth(month);
        filter.setKeyword(keyword);
        filter.setPage(page);
        filter.setSize(size);
        return transactionService.findPage(filter);
    }

    @PostMapping("/api/transactions")
    @ResponseBody
    public Transaction apiCreate(@RequestBody Transaction transaction) {
        return transactionService.save(transaction);
    }

    @PutMapping("/api/transactions/{id}")
    @ResponseBody
    public Transaction apiUpdate(@PathVariable Long id, @RequestBody Transaction transaction) {
        transaction.setId(id);
        return transactionService.save(transaction);
    }

    @DeleteMapping("/api/transactions/{id}")
    @ResponseBody
    public void apiDelete(@PathVariable Long id) {
        transactionService.delete(id);
    }

    @ModelAttribute("today")
    public LocalDate today() {
        return LocalDate.now();
    }
}
