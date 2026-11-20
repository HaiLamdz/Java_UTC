package com.bookstore.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO trả về dữ liệu Tóm tắt sách phong cách Trợ lý AI (Tiki-style Conversational Summary)
 */
public class AiSummaryDto {

    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookImage;
    private Long defaultVariantId;
    private String defaultVariantName;
    private Double defaultPrice;
    private boolean inStock;

    private String hookQuestion;
    private String shortSummary;
    private List<SectionItem> sections = new ArrayList<>();
    private List<String> takeaways = new ArrayList<>(); // "Tại sao bạn nên đọc"
    private String targetAudience;
    private String authorBio;
    private Integer readingTimeMinutes;
    private String formattedReadingTime;
    private String aiModel;
    private String updatedAt;
    private boolean cached;

    public AiSummaryDto() {
    }

    public static class SectionItem {
        private String heading;
        private String subtitle;
        private String content;

        public SectionItem() {
        }

        public SectionItem(String heading, String subtitle, String content) {
            this.heading = heading;
            this.subtitle = subtitle;
            this.content = content;
        }

        public String getHeading() {
            return heading;
        }

        public void setHeading(String heading) {
            this.heading = heading;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(String subtitle) {
            this.subtitle = subtitle;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

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

    public String getBookAuthor() {
        return bookAuthor;
    }

    public void setBookAuthor(String bookAuthor) {
        this.bookAuthor = bookAuthor;
    }

    public String getBookImage() {
        return bookImage;
    }

    public void setBookImage(String bookImage) {
        this.bookImage = bookImage;
    }

    public Long getDefaultVariantId() {
        return defaultVariantId;
    }

    public void setDefaultVariantId(Long defaultVariantId) {
        this.defaultVariantId = defaultVariantId;
    }

    public String getDefaultVariantName() {
        return defaultVariantName;
    }

    public void setDefaultVariantName(String defaultVariantName) {
        this.defaultVariantName = defaultVariantName;
    }

    public Double getDefaultPrice() {
        return defaultPrice;
    }

    public void setDefaultPrice(Double defaultPrice) {
        this.defaultPrice = defaultPrice;
    }

    public boolean isInStock() {
        return inStock;
    }

    public void setInStock(boolean inStock) {
        this.inStock = inStock;
    }

    public String getHookQuestion() {
        return hookQuestion;
    }

    public void setHookQuestion(String hookQuestion) {
        this.hookQuestion = hookQuestion;
    }

    public String getShortSummary() {
        return shortSummary;
    }

    public void setShortSummary(String shortSummary) {
        this.shortSummary = shortSummary;
    }

    public List<SectionItem> getSections() {
        return sections;
    }

    public void setSections(List<SectionItem> sections) {
        this.sections = sections;
    }

    public List<String> getTakeaways() {
        return takeaways;
    }

    public void setTakeaways(List<String> takeaways) {
        this.takeaways = takeaways;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    public String getAuthorBio() {
        return authorBio;
    }

    public void setAuthorBio(String authorBio) {
        this.authorBio = authorBio;
    }

    public Integer getReadingTimeMinutes() {
        return readingTimeMinutes;
    }

    public void setReadingTimeMinutes(Integer readingTimeMinutes) {
        this.readingTimeMinutes = readingTimeMinutes;
    }

    public String getFormattedReadingTime() {
        return formattedReadingTime;
    }

    public void setFormattedReadingTime(String formattedReadingTime) {
        this.formattedReadingTime = formattedReadingTime;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }
}
