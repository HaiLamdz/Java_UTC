package com.bookstore.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO đại diện cho Giỏ hàng (Cart) của khách hàng.
 * Thường được lưu trữ trong HttpSession để không phụ thuộc vào cơ sở dữ liệu nếu chưa đăng nhập.
 */
public class CartDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<CartItemDto> items = new ArrayList<>();

    public CartDto() {
    }

    /**
     * Thêm sản phẩm (kèm biến thể) vào giỏ hàng.
     * Nếu cùng mã biến thể đã có trong giỏ, tăng số lượng.
     */
    public void addItem(CartItemDto newItem) {
        for (CartItemDto item : items) {
            if (item.getVariantId().equals(newItem.getVariantId())) {
                int newQty = item.getQuantity() + newItem.getQuantity();
                // Không vượt quá tồn kho nếu có
                if (item.getStockQuantity() != null && newQty > item.getStockQuantity()) {
                    newQty = item.getStockQuantity();
                }
                item.setQuantity(newQty);
                return;
            }
        }
        items.add(newItem);
    }

    /**
     * Cập nhật số lượng của một biến thể trong giỏ
     */
    public void updateQuantity(Long variantId, int quantity) {
        if (quantity <= 0) {
            removeItem(variantId);
            return;
        }
        for (CartItemDto item : items) {
            if (item.getVariantId().equals(variantId)) {
                if (item.getStockQuantity() != null && quantity > item.getStockQuantity()) {
                    quantity = item.getStockQuantity();
                }
                item.setQuantity(quantity);
                return;
            }
        }
    }

    /**
     * Xóa một biến thể khỏi giỏ
     */
    public void removeItem(Long variantId) {
        items.removeIf(item -> item.getVariantId().equals(variantId));
    }

    /**
     * Xóa sạch giỏ hàng
     */
    public void clear() {
        items.clear();
    }

    /**
     * Tổng số lượng sản phẩm trong giỏ
     */
    public int getTotalQuantity() {
        return items.stream()
                .mapToInt(CartItemDto::getQuantity)
                .sum();
    }

    /**
     * Tổng số loại sản phẩm trong giỏ
     */
    public int getItemCount() {
        return items.size();
    }

    /**
     * Tổng thành tiền của cả giỏ hàng (VNĐ)
     */
    public Double getTotalAmount() {
        return items.stream()
                .mapToDouble(CartItemDto::getSubtotal)
                .sum();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    // ================= Getters & Setters =================
    public List<CartItemDto> getItems() {
        return items;
    }

    public void setItems(List<CartItemDto> items) {
        this.items = items;
    }
}
