package com.finance.controller;

import com.finance.entity.Category;
import com.finance.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public String list(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("category", new Category());
        return "category/list";
    }

    @PostMapping("/categories")
    public String save(@ModelAttribute Category category, RedirectAttributes redirectAttributes) {
        categoryService.save(category);
        redirectAttributes.addFlashAttribute("message", "分类已保存");
        return "redirect:/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deletePage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
            redirectAttributes.addFlashAttribute("message", "分类已删除");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/categories";
    }

    @GetMapping("/categories/{id}/delete")
    public String deletePageFallback(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "请在分类管理页面确认删除分类");
        return "redirect:/categories";
    }

    @GetMapping("/api/categories")
    @ResponseBody
    public List<Category> apiList() {
        return categoryService.findAll();
    }

    @PostMapping("/api/categories")
    @ResponseBody
    public Category apiCreate(@RequestBody Category category) {
        return categoryService.save(category);
    }

    @PutMapping("/api/categories/{id}")
    @ResponseBody
    public Category apiUpdate(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        return categoryService.save(category);
    }

    @DeleteMapping("/api/categories/{id}")
    @ResponseBody
    public void apiDelete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
