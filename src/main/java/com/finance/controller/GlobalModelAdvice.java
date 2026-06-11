package com.finance.controller;

import com.finance.entity.AppUser;
import com.finance.service.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
/**
 * 为所有 Thymeleaf 页面统一提供当前登录用户信息。
 */
public class GlobalModelAdvice {
    private final UserService userService;

    public GlobalModelAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("currentUser")
    /**
     * 将 currentUser 放入全局 Model，未登录或匿名用户返回 null。
     */
    public AppUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.currentUser();
    }
}
