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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
/**
 * 处理分类管理页面和分类 API，包括分类列表、新增、编辑和删除。
 */
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    /**
     * 展示分类管理页面，并根据 type 参数决定默认打开收入或支出分类。
     */
    public String list(@RequestParam(defaultValue = "1") Integer type, Model model) {
        int activeType = Integer.valueOf(2).equals(type) ? 2 : 1;
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("category", new Category());
        model.addAttribute("activeType", activeType);
        return "category/list";
    }

    @PostMapping("/categories")
    /**
     * 保存页面表单提交的分类；有 id 时更新，无 id 时新增。
     */
    public String save(@ModelAttribute Category category, RedirectAttributes redirectAttributes) {
        categoryService.save(category);
        redirectAttributes.addFlashAttribute("message", "分类已保存");
        return "redirect:/categories";
    }

    @PostMapping("/categories/{id}/delete")
    /**
     * 删除页面中的分类，并保持当前收入/支出标签页状态。
     */
    public String deletePage(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer type,
            RedirectAttributes redirectAttributes
    ) {
        int activeType = Integer.valueOf(2).equals(type) ? 2 : 1;
        try {
            categoryService.delete(id);
            redirectAttributes.addFlashAttribute("message", "分类已删除");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/categories?type=" + activeType;
    }

    @GetMapping("/categories/{id}/delete")
    /**
     * 阻止直接 GET 删除分类，引导用户回到分类管理页确认删除。
     */
    public String deletePageFallback(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "请在分类管理页面确认删除分类");
        return "redirect:/categories";
    }

    @GetMapping("/api/categories")
    @ResponseBody
    /**
     * 返回当前用户的全部分类列表，供前端或接口调用。
     */
    public List<Category> apiList() {
        return categoryService.findAll();
    }

    @PostMapping("/api/categories")
    @ResponseBody
    /**
     * API 新增分类。
     */
    public Category apiCreate(@RequestBody Category category) {
        return categoryService.save(category);
    }

    @PutMapping("/api/categories/{id}")
    @ResponseBody
    /**
     * API 更新指定分类。
     */
    public Category apiUpdate(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        return categoryService.save(category);
    }

    @DeleteMapping("/api/categories/{id}")
    @ResponseBody
    /**
     * API 删除指定分类。
     */
    public void apiDelete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
