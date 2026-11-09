package com.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CheckoutRequest {

    @NotBlank(message = "Vui lòng nhập họ và tên người nhận")
    private String customerName;

    @NotBlank(message = "Vui lòng nhập số điện thoại nhận hàng")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không hợp lệ (10 chữ số)")
    private String customerPhone;

    @NotBlank(message = "Vui lòng nhập địa chỉ giao hàng chi tiết")
    private String shippingAddress;

    private String note;

    private String paymentMethod = "COD"; // COD: Thanh toán khi nhận hàng, BANK_TRANSFER: Chuyển khoản

    public CheckoutRequest() {
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
