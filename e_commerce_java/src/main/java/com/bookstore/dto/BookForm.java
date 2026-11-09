package com.bookstore.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public class BookForm {

    private Long id;

    @NotBlank(message = "Tên sách không được để trống")
    private String title;

    @NotBlank(message = "Tên tác giả không được để trống")
    private String author;

    private String publisher;
    private Integer publishYear;
    private String isbn;
    private String description;
    private String imageUrl;

    @NotNull(message = "Vui lòng chọn thể loại sách")
    private Long categoryId;

    private Boolean featured = false;

    /**
     * Danh sách các biến thể của sách trong form
     */
    @Valid
    private List<VariantForm> variants = new ArrayList<>();

    public BookForm() {
        // Mặc định tạo sẵn 1 biến thể bìa mềm
        variants.add(new VariantForm("Bìa mềm", "SKU-BM-01", 100000.0, 120000.0, 20, true));
    }

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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Boolean getFeatured() {
        return featured != null && featured;
    }

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }

    public List<VariantForm> getVariants() {
        return variants;
    }

    public void setVariants(List<VariantForm> variants) {
        this.variants = variants;
    }
}
