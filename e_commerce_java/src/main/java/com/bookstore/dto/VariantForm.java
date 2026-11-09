package com.bookstore.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class VariantForm {

    private Long id;

    @NotBlank(message = "Tên biến thể không được để trống (ví dụ: Bìa mềm, Bìa cứng)")
    private String variantName;

    private String sku;

    @NotNull(message = "Giá bán của biến thể không được để trống")
    @Min(value = 0, message = "Giá bán không thể âm")
    private Double price;

    private Double originalPrice;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Tồn kho không thể âm")
    private Integer stockQuantity = 10;

    private Boolean isDefault = false;

    public VariantForm() {
    }

    public VariantForm(String variantName, String sku, Double price, Double originalPrice, Integer stockQuantity, Boolean isDefault) {
        this.variantName = variantName;
        this.sku = sku;
        this.price = price;
        this.originalPrice = originalPrice;
        this.stockQuantity = stockQuantity;
        this.isDefault = isDefault;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(Double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Boolean getIsDefault() {
        return isDefault != null && isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}
