package com.finance.controller;

import com.finance.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.register(username, password, confirmPassword);
            redirectAttributes.addFlashAttribute("message", "注册成功，请登录");
            return "redirect:/login";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute("username", username);
            return "register";
        }
    }

    @PostMapping("/forgot-password")
    public String resetPassword(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.resetPassword(username, password, confirmPassword);
            redirectAttributes.addFlashAttribute("message", "密码已重置，请使用新密码登录");
            return "redirect:/login";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute("username", username);
            return "forgot-password";
        }
    }

    @PostMapping("/profile/avatar")
    public String updateAvatar(@RequestParam("avatar") MultipartFile avatar, RedirectAttributes redirectAttributes) {
        try {
            userService.updateAvatar(avatar);
            redirectAttributes.addFlashAttribute("message", "头像已更新");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("message", exception.getMessage());
        }
        return "redirect:/";
    }
}
