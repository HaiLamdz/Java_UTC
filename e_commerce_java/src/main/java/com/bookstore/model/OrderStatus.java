package com.bookstore.model;

/**
 * Trạng thái của Đơn hàng:
 * - PENDING: Chờ xác nhận
 * - CONFIRMED: Đã xác nhận
 * - SHIPPING: Đang giao hàng
 * - COMPLETED: Đã hoàn thành (Giao thành công)
 * - CANCELLED: Đã hủy đơn
 */
public enum OrderStatus {
    PENDING("Chờ xác nhận", "badge-warning"),
    CONFIRMED("Đã xác nhận", "badge-info"),
    SHIPPING("Đang giao hàng", "badge-primary"),
    COMPLETED("Hoàn thành", "badge-success"),
    CANCELLED("Đã hủy", "badge-danger");

    private final String displayName;
    private final String badgeClass;

    OrderStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
