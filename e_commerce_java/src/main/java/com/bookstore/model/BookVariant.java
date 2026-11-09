package com.bookstore.model;

import jakarta.persistence.*;

/**
 * Thực thể Biến thể Sách (BookVariant) - YÊU CẦU QUAN TRỌNG CỦA BÀI TẬP LỚN!
 * Một cuốn sách có thể có nhiều biến thể:
 * - Bìa mềm (Paperback)
 * - Bìa cứng (Hardcover)
 * - Bản đặc biệt / Giới hạn (Collector's / Gift Edition)
 * - Ngôn ngữ: Bản Tiếng Việt / Bản Tiếng Anh
 *
 * Mỗi biến thể có mã SKU, giá bán, giá bìa và số lượng tồn kho riêng.
 */
@Entity
@Table(name = "book_variants")
public class BookVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    /**
     * Tên biến thể: Ví dụ "Bìa mềm", "Bìa cứng mạ vàng", "Bản đặc biệt kèm Bookmark"
     */
    @Column(name = "variant_name", nullable = false, length = 100)
    private String variantName;

    /**
     * Mã SKU riêng cho từng biến thể để quản lý kho: ví dụ "DNT-SOFT-01"
     */
    @Column(length = 50)
    private String sku;

    /**
     * Giá bán thực tế của biến thể (VNĐ)
     */
    @Column(nullable = false)
    private Double price;

    /**
     * Giá bìa / Giá gốc (dùng để hiển thị giá gạch ngang / giảm giá)
     */
    @Column(name = "original_price")
    private Double originalPrice;

    /**
     * Số lượng hàng tồn kho của riêng biến thể này
     */
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    /**
     * Có phải biến thể mặc định được chọn ban đầu không?
     */
    @Column(name = "is_default")
    private Boolean isDefault = false;

    public BookVariant() {
    }

    public BookVariant(Book book, String variantName, String sku, Double price, Double originalPrice, Integer stockQuantity, Boolean isDefault) {
        this.book = book;
        this.variantName = variantName;
        this.sku = sku;
        this.price = price;
        this.originalPrice = originalPrice;
        this.stockQuantity = stockQuantity;
        this.isDefault = isDefault;
    }

    // ================= Helper Methods =================
    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }

    /**
     * Tính tỷ lệ phần trăm giảm giá nếu có originalPrice > price
     */
    public int getDiscountPercent() {
        if (originalPrice != null && originalPrice > price && originalPrice > 0) {
            return (int) Math.round(((originalPrice - price) / originalPrice) * 100);
        }
        return 0;
    }

    // ================= Getters & Setters =================
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
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
