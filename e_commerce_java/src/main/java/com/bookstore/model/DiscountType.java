package com.bookstore.model;

public enum DiscountType {
    PERCENT("Giảm theo %"),
    FIXED("Giảm tiền mặt (VNĐ)");

    private final String displayName;

    DiscountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

