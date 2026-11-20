package com.bookstore.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Phản hồi từ Trợ lý AI khi khách hàng đặt câu hỏi trong khung chat
 */
public class AiChatResponse {

    private String answer;
    private List<String> suggestedQuestions = new ArrayList<>();
    private List<MiniBookCard> recommendedBooks = new ArrayList<>();
    private String aiModel;

    public AiChatResponse() {
    }

    public static class MiniBookCard {
        private Long bookId;
        private String title;
        private String author;
        private String imageUrl;
        private Long variantId;
        private String variantName;
        private Double price;
        private boolean inStock;

        public MiniBookCard() {
        }

        public MiniBookCard(Long bookId, String title, String author, String imageUrl,
                            Long variantId, String variantName, Double price, boolean inStock) {
            this.bookId = bookId;
            this.title = title;
            this.author = author;
            this.imageUrl = imageUrl;
            this.variantId = variantId;
            this.variantName = variantName;
            this.price = price;
            this.inStock = inStock;
        }

        public Long getBookId() {
            return bookId;
        }

        public void setBookId(Long bookId) {
            this.bookId = bookId;
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

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public Long getVariantId() {
            return variantId;
        }

        public void setVariantId(Long variantId) {
            this.variantId = variantId;
        }

        public String getVariantName() {
            return variantName;
        }

        public void setVariantName(String variantName) {
            this.variantName = variantName;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public boolean isInStock() {
            return inStock;
        }

        public void setInStock(boolean inStock) {
            this.inStock = inStock;
        }
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<String> getSuggestedQuestions() {
        return suggestedQuestions;
    }

    public void setSuggestedQuestions(List<String> suggestedQuestions) {
        this.suggestedQuestions = suggestedQuestions;
    }

    public List<MiniBookCard> getRecommendedBooks() {
        return recommendedBooks;
    }

    public void setRecommendedBooks(List<MiniBookCard> recommendedBooks) {
        this.recommendedBooks = recommendedBooks;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }
}

