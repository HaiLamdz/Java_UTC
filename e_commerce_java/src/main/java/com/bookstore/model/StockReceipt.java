package com.bookstore.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Thực thể Phiếu Nhập Kho Sách (StockReceipt) - Bảng thứ 9 trong CSDL.
 * Lưu lịch sử nhập thêm hàng cho từng biến thể sách (SKU) kèm đơn giá vốn và nhà cung cấp.
 */
@Entity
@Table(name = "stock_receipts")
public class StockReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_code", nullable = false, unique = true, length = 40)
    private String receiptCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private BookVariant variant;

    @Column(name = "book_title", nullable = false, length = 200)
    private String bookTitle;

    @Column(name = "variant_name", nullable = false, length = 100)
    private String variantName;

    @Column(name = "sku", length = 50)
    private String sku;

    @Column(name = "quantity_added", nullable = false)
    private Integer quantityAdded;

    @Column(name = "stock_after", nullable = false)
    private Integer stockAfter;

    @Column(name = "import_price", nullable = false)
    private Double importPrice = 0.0;

    @Column(name = "supplier_name", length = 150)
    private String supplierName;

    @Column(name = "note", length = 300)
    private String note;

    @Column(name = "created_by", length = 80)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public StockReceipt() {
        this.createdAt = LocalDateTime.now();
    }

    public Double getTotalImportCost() {
        return (quantityAdded != null ? quantityAdded : 0) * (importPrice != null ? importPrice : 0.0);
    }

    // ================= Getters & Setters =================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReceiptCode() { return receiptCode; }
    public void setReceiptCode(String receiptCode) { this.receiptCode = receiptCode; }

    public BookVariant getVariant() { return variant; }
    public void setVariant(BookVariant variant) { this.variant = variant; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getVariantName() { return variantName; }
    public void setVariantName(String variantName) { this.variantName = variantName; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Integer getQuantityAdded() { return quantityAdded; }
    public void setQuantityAdded(Integer quantityAdded) { this.quantityAdded = quantityAdded; }

    public Integer getStockAfter() { return stockAfter; }
    public void setStockAfter(Integer stockAfter) { this.stockAfter = stockAfter; }

    public Double getImportPrice() { return importPrice; }
    public void setImportPrice(Double importPrice) { this.importPrice = importPrice; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

