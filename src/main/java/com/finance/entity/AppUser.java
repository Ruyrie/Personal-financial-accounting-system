package com.finance.entity;

import java.time.LocalDateTime;

/**
 * 用户实体，对应 app_user 表。
 */
public class AppUser {
    /** 用户主键。 */
    private Long id;
    /** 登录用户名。 */
    private String username;
    /** Spring Security 使用的加密密码。 */
    private String password;
    /** 头像 Data URL。 */
    private String avatarData;
    /** 用户创建时间。 */
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAvatarData() {
        return avatarData;
    }

    public void setAvatarData(String avatarData) {
        this.avatarData = avatarData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
