package com.bookstore.dto;

/**
 * Request gửi từ trang Quản trị (Admin) để xem trước / tự động sinh tóm tắt AI ngay trên form sách
 */
public class AiPreviewRequest {

    private String title;
    private String author;
    private String categoryName;
    private String publisher;
    private Integer publishYear;
    private String description;
    private String sampleExcerpt;

    public AiPreviewRequest() {
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

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSampleExcerpt() {
        return sampleExcerpt;
    }

    public void setSampleExcerpt(String sampleExcerpt) {
        this.sampleExcerpt = sampleExcerpt;
    }
}

