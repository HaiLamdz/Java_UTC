package com.bookstore.dto;

import java.io.Serializable;

/**
 * DTO đại diện cho một sản phẩm trong giỏ hàng.
 * Chứa thông tin sách và biến thể cụ thể đã được người dùng chọn.
 */
public class CartItemDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long bookId;
    private String bookTitle;
    private String bookImage;

    private Long variantId;
    private String variantName;
    private Double price;
    private Integer stockQuantity;

    private Integer quantity;

    public CartItemDto() {
    }

    public CartItemDto(Long bookId, String bookTitle, String bookImage, Long variantId, String variantName, Double price, Integer stockQuantity, Integer quantity) {
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.bookImage = bookImage;
        this.variantId = variantId;
        this.variantName = variantName;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.quantity = quantity;
    }

    public Double getSubtotal() {
        if (price != null && quantity != null) {
            return price * quantity;
        }
        return 0.0;
    }

    // ================= Getters & Setters =================
    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getBookImage() {
        return bookImage;
    }

    public void setBookImage(String bookImage) {
        this.bookImage = bookImage;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
