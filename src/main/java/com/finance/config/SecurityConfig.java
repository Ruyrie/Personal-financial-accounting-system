package com.finance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
/**
 * Spring Security 配置类，定义登录、授权、记住我、退出和密码编码策略。
 */
public class SecurityConfig {
    @Bean
    /**
     * 配置 Web 安全过滤链：公开登录注册资源，其余请求必须登录。
     */
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // 本项目页面表单未统一携带 CSRF token，因此关闭 CSRF 校验，避免 POST 表单被拦截。
                .csrf(csrf -> csrf.disable())
                // 定义请求授权规则：先声明可匿名访问的地址，再兜底要求其他请求必须登录。
                .authorizeHttpRequests(auth -> auth
                        // 登录、注册、找回密码和静态资源必须放行，否则用户未登录时无法打开登录页和样式脚本。
                        .requestMatchers("/login", "/register", "/forgot-password", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        // 除上面白名单外，首页、收支记录、分类管理、导出等业务功能都需要认证。
                        .anyRequest().authenticated()
                )
                // 启用表单登录，并指定自定义登录页。
                .formLogin(login -> login
                        // 告诉 Spring Security 使用 /login 页面作为登录入口。
                        .loginPage("/login")
                        // 登录成功后固定跳转到财务总览首页。
                        .defaultSuccessUrl("/", true)
                        // 允许所有用户访问登录相关端点，避免登录页自身被拦截。
                        .permitAll()
                )
                // 启用“记住我”功能，让用户关闭浏览器后仍可保持登录一段时间。
                .rememberMe(remember -> remember
                        // 与登录表单中的 checkbox name 对应，勾选后才启用 remember-me。
                        .rememberMeParameter("remember-me")
                        // 自定义 remember-me Cookie 名称，便于退出时清理。
                        .rememberMeCookieName("finance-remember-me")
                        // 设置记住我有效期为 14 天。
                        .tokenValiditySeconds(60 * 60 * 24 * 14)
                        // remember-me token 签名密钥，防止客户端伪造 Cookie。
                        .key("personal-finance-remember-me")
                )
                // 配置退出登录行为。
                .logout(logout -> logout
                        // Spring Security 监听的退出地址，页面退出表单提交到这里。
                        .logoutUrl("/logout")
                        // 退出成功后回到登录页，并携带 logout 参数显示提示。
                        .logoutSuccessUrl("/login?logout")
                        // 删除会话 Cookie 和 remember-me Cookie，确保退出后不会自动恢复登录。
                        .deleteCookies("JSESSIONID", "finance-remember-me")
                )
                // 根据上面的链式配置构建最终的安全过滤链 Bean。
                .build();
    }

    @Bean
    /**
     * 创建密码编码器，用于注册、重置密码和登录校验。
     */
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
