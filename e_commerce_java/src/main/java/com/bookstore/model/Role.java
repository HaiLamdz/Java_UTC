package com.bookstore.model;

/**
 * Phân quyền tài khoản trong hệ thống:
 * - ROLE_CUSTOMER: Khách hàng mua sách
 * - ROLE_ADMIN: Quản trị viên cửa hàng
 */
public enum Role {
    ROLE_CUSTOMER("Khách hàng"),
    ROLE_ADMIN("Quản trị viên");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
