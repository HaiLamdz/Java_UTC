package com.bookstore.model;

/**
 * Trạng thái thanh toán của đơn hàng (hỗ trợ đối soát COD & VietQR)
 */
public enum PaymentStatus {
    UNPAID("Chưa thanh toán", "badge-pending"),
    WAITING_CONFIRM("Chờ đối soát CK", "badge-confirmed"),
    PAID("Đã thanh toán", "badge-completed"),
    REFUNDED("Đã hoàn tiền", "badge-cancelled");

    private final String displayName;
    private final String badgeClass;

    PaymentStatus(String displayName, String badgeClass) {
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

