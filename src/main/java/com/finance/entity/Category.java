package com.finance.entity;

import java.time.LocalDateTime;

/**
 * 收支分类实体，对应 category 表。
 */
public class Category {
    /** 分类主键。 */
    private Long id;
    /** 所属用户 id。 */
    private Long userId;
    /** 分类名称，如工资、餐饮。 */
    private String name;
    /** 分类类型：1 表示收入，2 表示支出。 */
    private Integer type;
    /** 分类图标标识。 */
    private String icon;
    /** 分类排序值。 */
    private Integer sortOrder;
    /** 分类创建时间。 */
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
