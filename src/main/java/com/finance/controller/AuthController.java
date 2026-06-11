package com.finance.controller;

import com.finance.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

@Controller
/**
 * 处理用户认证相关页面和资料操作，包括登录页、注册、找回密码、头像上传和修改密码。
 */
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    /**
     * 返回登录页面。
     */
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    /**
     * 返回注册页面。
     */
    public String registerForm() {
        return "register";
    }

    @GetMapping("/forgot-password")
    /**
     * 返回找回密码页面。
     */
    public String forgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/register")
    /**
     * 提交注册表单，注册成功后跳转登录页，失败时回显错误信息。
     */
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
            redirectAttributes.addFlashAttribute("username", username == null ? "" : username.trim());
            return "redirect:/login";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute("username", username);
            return "register";
        }
    }

    @PostMapping("/forgot-password")
    /**
     * 根据用户名重置密码，成功后提示用户使用新密码登录。
     */
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
    /**
     * 更新当前用户头像，并把头像校验结果通过 flash message 返回页面。
     */
    public String updateAvatar(@RequestParam("avatar") MultipartFile avatar, RedirectAttributes redirectAttributes) {
        try {
            userService.updateAvatar(avatar);
            redirectAttributes.addFlashAttribute("message", "头像已更新");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("message", exception.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/profile/password")
    /**
     * 修改当前用户密码；修改成功后主动退出当前会话，要求用户重新登录。
     */
    public String changePassword(
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestHeader(value = "Referer", required = false) String referer,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.changePassword(password, confirmPassword);
            logoutCurrentSession(request, response);
            return "password-changed";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:" + redirectPath(referer);
    }

    /**
     * 清理 Spring Security 会话和 remember-me Cookie。
     */
    private void logoutCurrentSession(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        Cookie rememberCookie = new Cookie("finance-remember-me", "");
        rememberCookie.setPath("/");
        rememberCookie.setMaxAge(0);
        rememberCookie.setHttpOnly(true);
        response.addCookie(rememberCookie);
    }

    /**
     * 将 Referer 转换为站内重定向路径，避免把用户重定向到外部地址。
     */
    private String redirectPath(String referer) {
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        try {
            URI uri = URI.create(referer);
            String path = uri.getPath();
            String query = uri.getQuery();
            if (path == null || path.isBlank() || path.startsWith("//")) {
                return "/";
            }
            return query == null || query.isBlank() ? path : path + "?" + query;
        } catch (IllegalArgumentException exception) {
            return "/";
        }
    }
}
