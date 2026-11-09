package com.bookstore.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AddToCartRequest {

    @NotNull(message = "Thiếu thông tin sách")
    private Long bookId;

    @NotNull(message = "Vui lòng chọn biến thể sách (Bìa mềm, bìa cứng, v.v.)")
    private Long variantId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng mua tối thiểu là 1")
    private Integer quantity = 1;

    public AddToCartRequest() {
    }

    public AddToCartRequest(Long bookId, Long variantId, Integer quantity) {
        this.bookId = bookId;
        this.variantId = variantId;
        this.quantity = quantity;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
