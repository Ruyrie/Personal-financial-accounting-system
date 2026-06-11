package com.finance.service;

import com.finance.dao.CategoryDao;
import com.finance.dao.UserDao;
import com.finance.entity.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Set;

@Service
/**
 * 用户业务服务，负责登录认证数据加载、注册、密码管理、头像上传和当前用户定位。
 */
public class UserService implements UserDetailsService {
    private static final long MAX_AVATAR_BYTES = 512 * 1024;
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    private final UserDao userDao;
    private final CategoryDao categoryDao;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserDao userDao, CategoryDao categoryDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.categoryDao = categoryDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    /**
     * Spring Security 登录时根据用户名加载账号和加密密码。
     */
    public UserDetails loadUserByUsername(String username) {
        AppUser user = userDao.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .roles("USER")
                .build();
    }

    @Transactional
    /**
     * 注册新用户，并在同一事务中创建默认收支分类。
     */
    public AppUser register(String username, String password, String confirmPassword) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(password, confirmPassword);
        if (userDao.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        AppUser user = userDao.create(normalizedUsername, passwordEncoder.encode(password));
        categoryDao.createDefaultsForUser(user.getId());
        return user;
    }

    /**
     * 通过用户名重置密码，用于找回密码页面。
     */
    public void resetPassword(String username, String password, String confirmPassword) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(password, confirmPassword);
        if (userDao.updatePasswordByUsername(normalizedUsername, passwordEncoder.encode(password)) == 0) {
            throw new IllegalArgumentException("用户不存在");
        }
    }

    /**
     * 修改当前登录用户密码，并禁止新密码与旧密码相同。
     */
    public void changePassword(String password, String confirmPassword) {
        AppUser user = currentUser();
        validatePassword(password, confirmPassword);
        if (passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("新密码不能和旧密码相同");
        }
        userDao.updatePasswordByUsername(user.getUsername(), passwordEncoder.encode(password));
    }

    /**
     * 获取当前登录用户的数据库主键，供分类和收支记录实现用户隔离。
     */
    public Long currentUserId() {
        return currentUser().getId();
    }

    /**
     * 从 Spring Security 上下文读取当前登录用户，并查询完整用户信息。
     */
    public AppUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("请先登录");
        }
        return userDao.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
    }

    /**
     * 校验头像文件类型和大小，并将图片转为 Data URL 保存到数据库。
     */
    public void updateAvatar(MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            throw new IllegalArgumentException("请选择头像图片");
        }
        if (avatar.getSize() > MAX_AVATAR_BYTES) {
            throw new IllegalArgumentException("头像不能超过512KB");
        }
        String contentType = avatar.getContentType();
        if (!ALLOWED_AVATAR_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("头像仅支持PNG、JPG或WebP格式");
        }
        try {
            String encoded = Base64.getEncoder().encodeToString(avatar.getBytes());
            userDao.updateAvatar(currentUserId(), "data:" + contentType + ";base64," + encoded);
        } catch (IOException exception) {
            throw new IllegalStateException("头像上传失败", exception);
        }
    }

    /**
     * 标准化并校验用户名格式。
     */
    private String normalizeUsername(String username) {
        String normalizedUsername = username == null ? "" : username.trim();
        if (!StringUtils.hasText(normalizedUsername)) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (normalizedUsername.length() < 3 || normalizedUsername.length() > 30) {
            throw new IllegalArgumentException("用户名长度需为3-30位");
        }
        if (!normalizedUsername.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("用户名只能包含字母、数字和下划线");
        }
        return normalizedUsername;
    }

    /**
     * 校验密码长度和两次输入是否一致。
     */
    private void validatePassword(String password, String confirmPassword) {
        if (!StringUtils.hasText(password) || password.length() < 6) {
            throw new IllegalArgumentException("密码至少6位");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
    }
}
