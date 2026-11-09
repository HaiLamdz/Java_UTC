package com.bookstore.model;

import jakarta.persistence.*;

/**
 * Thực thể Chi tiết đơn hàng (OrderItem) - Mỗi dòng sản phẩm trong một đơn hàng.
 * Lưu giữ cả tên sách và tên biến thể tại thời điểm khách hàng đặt mua.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private BookVariant variant;

    @Column(name = "book_title", nullable = false)
    private String bookTitle;

    @Column(name = "variant_name", nullable = false)
    private String variantName;

    @Column(name = "book_image")
    private String bookImage;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "subtotal", nullable = false)
    private Double subtotal;

    public OrderItem() {
    }

    public OrderItem(Order order, Book book, BookVariant variant, Integer quantity) {
        this.order = order;
        this.book = book;
        this.variant = variant;
        this.bookTitle = book != null ? book.getTitle() : "";
        this.variantName = variant != null ? variant.getVariantName() : "";
        this.bookImage = book != null ? book.getImageUrl() : "";
        this.price = variant != null ? variant.getPrice() : 0.0;
        this.quantity = quantity;
        this.subtotal = this.price * (quantity != null ? quantity : 1);
    }

    // ================= Getters & Setters =================
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public BookVariant getVariant() {
        return variant;
    }

    public void setVariant(BookVariant variant) {
        this.variant = variant;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public String getBookImage() {
        return bookImage;
    }

    public void setBookImage(String bookImage) {
        this.bookImage = bookImage;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        if (this.price != null && this.quantity != null) {
            this.subtotal = this.price * this.quantity;
        }
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }
}
