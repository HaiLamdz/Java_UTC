package com.bookstore.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Thực thể Sách (Book) - Đại diện cho một đầu sách trong cửa hàng.
 * Một cuốn sách có thể thuộc về một Thể loại (Category)
 * và chứa danh sách các Biến thể (BookVariant).
 */
@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(length = 100)
    private String publisher; // Nhà xuất bản

    @Column(name = "publish_year")
    private Integer publishYear; // Năm xuất bản

    @Column(length = 30)
    private String isbn;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * Quan hệ 1 - N: Một cuốn sách có nhiều biến thể (Bìa mềm, bìa cứng, bản đặc biệt...)
     * CascadeType.ALL: Khi lưu/xóa sách thì tự động lưu/xóa các biến thể tương ứng.
     */
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BookVariant> variants = new ArrayList<>();

    @Column(name = "is_featured")
    private Boolean featured = false; // Sách nổi bật hiển thị ở trang chủ

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Book() {
        this.createdAt = LocalDateTime.now();
    }

    public Book(String title, String author, String publisher, Integer publishYear, String isbn, String description, String imageUrl, Category category, Boolean featured) {
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.publishYear = publishYear;
        this.isbn = isbn;
        this.description = description;
        this.imageUrl = imageUrl;
        this.category = category;
        this.featured = featured;
        this.createdAt = LocalDateTime.now();
    }

    // ================= Quản lý biến thể liên kết =================
    public void addVariant(BookVariant variant) {
        variants.add(variant);
        variant.setBook(this);
    }

    public void removeVariant(BookVariant variant) {
        variants.remove(variant);
        variant.setBook(null);
    }

    /**
     * Lấy biến thể mặc định (hoặc biến thể đầu tiên nếu không có biến thể mặc định)
     */
    public BookVariant getDefaultVariant() {
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        for (BookVariant v : variants) {
            if (v.getIsDefault()) {
                return v;
            }
        }
        return variants.get(0);
    }

    /**
     * Lấy giá thấp nhất trong các biến thể
     */
    public Double getMinPrice() {
        if (variants == null || variants.isEmpty()) {
            return 0.0;
        }
        return variants.stream()
                .map(BookVariant::getPrice)
                .min(Double::compare)
                .orElse(0.0);
    }

    /**
     * Lấy giá cao nhất trong các biến thể
     */
    public Double getMaxPrice() {
        if (variants == null || variants.isEmpty()) {
            return 0.0;
        }
        return variants.stream()
                .map(BookVariant::getPrice)
                .max(Double::compare)
                .orElse(0.0);
    }

    /**
     * Tính tổng số lượng hàng tồn kho của tất cả các biến thể
     */
    public int getTotalStock() {
        if (variants == null || variants.isEmpty()) {
            return 0;
        }
        return variants.stream()
                .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                .sum();
    }

    public boolean isInStock() {
        return getTotalStock() > 0;
    }

    // ================= Getters & Setters =================
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public Integer getPublishYear() {
        return publishYear;
    }

    public void setPublishYear(Integer publishYear) {
        this.publishYear = publishYear;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<BookVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<BookVariant> variants) {
        this.variants = variants;
    }

    public Boolean getFeatured() {
        return featured != null && featured;
    }

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
