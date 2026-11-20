package com.bookstore.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Thực thể Mã giảm giá / Khuyến mãi (Voucher) - Bảng thứ 8 trong CSDL.
 */
@Entity
@Table(name = "vouchers")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType = DiscountType.PERCENT;

    @Column(name = "discount_value", nullable = false)
    private Double discountValue = 0.0;

    @Column(name = "max_discount_amount")
    private Double maxDiscountAmount; // Mức giảm tối đa khi áp dụng PERCENT

    @Column(name = "min_order_amount")
    private Double minOrderAmount = 0.0; // Giá trị đơn tối thiểu để áp dụng

    @Column(name = "usage_limit")
    private Integer usageLimit = 100;

    @Column(name = "used_count")
    private Integer usedCount = 0;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Voucher() {
        this.createdAt = LocalDateTime.now();
    }

    public Voucher(String code, String description, DiscountType discountType, Double discountValue,
                   Double maxDiscountAmount, Double minOrderAmount, Integer usageLimit, LocalDate endDate) {
        this.code = code != null ? code.trim().toUpperCase() : "";
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minOrderAmount = minOrderAmount != null ? minOrderAmount : 0.0;
        this.usageLimit = usageLimit != null ? usageLimit : 100;
        this.usedCount = 0;
        this.startDate = LocalDate.now();
        this.endDate = endDate;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isValidNow() {
        if (active != null && !active) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (startDate != null && today.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && today.isAfter(endDate)) {
            return false;
        }
        int used = usedCount != null ? usedCount : 0;
        if (usageLimit != null && usageLimit > 0 && used >= usageLimit) {
            return false;
        }
        return true;
    }

    public int getRemainingUses() {
        if (usageLimit == null || usageLimit <= 0) {
            return 999;
        }
        int used = usedCount != null ? usedCount : 0;
        return Math.max(0, usageLimit - used);
    }

    public double calculateDiscount(double subtotal) {
        if (!isValidNow()) {
            return 0.0;
        }
        double minReq = minOrderAmount != null ? minOrderAmount : 0.0;
        if (subtotal < minReq) {
            return 0.0;
        }
        double discount;
        if (discountType == DiscountType.PERCENT) {
            discount = Math.round(subtotal * (discountValue / 100.0));
            if (maxDiscountAmount != null && maxDiscountAmount > 0 && discount > maxDiscountAmount) {
                discount = maxDiscountAmount;
            }
        } else {
            discount = discountValue != null ? discountValue : 0.0;
        }
        return Math.min(discount, subtotal);
    }

    // ================= Getters & Setters =================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code != null ? code.trim().toUpperCase() : null; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }

    public Double getDiscountValue() { return discountValue; }
    public void setDiscountValue(Double discountValue) { this.discountValue = discountValue; }

    public Double getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(Double maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }

    public Double getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(Double minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }

    public Integer getUsedCount() { return usedCount != null ? usedCount : 0; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Boolean getActive() { return active == null || active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

