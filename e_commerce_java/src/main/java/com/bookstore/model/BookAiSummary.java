package com.bookstore.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Thực thể Lưu trữ Bản Tóm Tắt & Điểm Sách Bằng AI (BookAiSummary).
 * Đóng vai trò Database Cache giúp tiết kiệm chi phí gọi API AI và tăng tốc độ phản hồi trang chi tiết sách.
 */
@Entity
@Table(name = "book_ai_summaries")
public class BookAiSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false, unique = true)
    private Book book;

    /**
     * Câu hỏi mở đầu khơi gợi sự tò mò (phong cách Trợ lý AI Tiki)
     */
    @Lob
    @Column(name = "hook_question")
    private String hookQuestion;

    /**
     * Tóm tắt cốt lõi trong 60 giây (3 - 5 câu mạch lạc)
     */
    @Lob
    @Column(name = "short_summary", nullable = false)
    private String shortSummary;

    /**
     * Cấu trúc các phần/chủ đề trọng tâm dạng JSON (heading, subtitle, content)
     */
    @Lob
    @Column(name = "sections_json")
    private String sectionsJson;

    /**
     * Các bài học / luận điểm đắt giá ("Tại sao bạn nên đọc" - lưu mỗi ý trên một dòng)
     */
    @Lob
    @Column(name = "key_takeaways")
    private String keyTakeaways;

    /**
     * Gợi ý nhóm độc giả phù hợp nhất với cuốn sách
     */
    @Lob
    @Column(name = "target_audience")
    private String targetAudience;

    /**
     * Giới thiệu ngắn về tác giả và phong cách viết
     */
    @Lob
    @Column(name = "author_bio")
    private String authorBio;

    /**
     * Thời lượng đọc trọn cuốn sách ước tính (tính bằng phút)
     */
    @Column(name = "reading_time_minutes")
    private Integer readingTimeMinutes = 240;

    /**
     * Nguồn mô hình sinh nội dung (ví dụ: gemini-2.5-flash, Admin-Curated, Contextual-AI)
     */
    @Column(name = "ai_model", length = 80)
    private String aiModel;

    /**
     * Số lần tạo / làm mới bản tóm tắt
     */
    @Column(name = "generated_count")
    private Integer generatedCount = 1;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public BookAiSummary() {
        this.updatedAt = LocalDateTime.now();
    }

    public BookAiSummary(Book book, String shortSummary, String keyTakeaways, String targetAudience,
                         Integer readingTimeMinutes, String aiModel) {
        this.book = book;
        this.shortSummary = shortSummary;
        this.keyTakeaways = keyTakeaways;
        this.targetAudience = targetAudience;
        this.readingTimeMinutes = readingTimeMinutes != null ? readingTimeMinutes : 240;
        this.aiModel = aiModel;
        this.generatedCount = 1;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Tách chuỗi keyTakeaways thành danh sách từng luận điểm sạch để hiển thị trên giao diện
     */
    public List<String> getTakeawayList() {
        List<String> items = new ArrayList<>();
        if (keyTakeaways == null || keyTakeaways.isBlank()) {
            return items;
        }
        String[] lines = keyTakeaways.split("\\r?\\n");
        for (String line : lines) {
            String cleaned = line.trim()
                    .replaceFirst("^(?:[-•*]|\\d+[.)])\\s*", "")
                    .trim();
            if (!cleaned.isEmpty()) {
                items.add(cleaned);
            }
        }
        return items;
    }

    public String getFormattedReadingTime() {
        int mins = readingTimeMinutes != null && readingTimeMinutes > 0 ? readingTimeMinutes : 240;
        if (mins < 60) {
            return mins + " phút đọc sách";
        }
        int hours = mins / 60;
        int rem = mins % 60;
        return rem > 0 ? ("~" + hours + " giờ " + rem + " phút đọc sách") : ("~" + hours + " giờ đọc sách");
    }

    public String getFormattedUpdatedAt() {
        if (updatedAt == null) {
            return "";
        }
        return updatedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
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

    public String getShortSummary() {
        return shortSummary;
    }

    public void setShortSummary(String shortSummary) {
        this.shortSummary = shortSummary;
    }

    public String getHookQuestion() {
        return hookQuestion;
    }

    public void setHookQuestion(String hookQuestion) {
        this.hookQuestion = hookQuestion;
    }

    public String getSectionsJson() {
        return sectionsJson;
    }

    public void setSectionsJson(String sectionsJson) {
        this.sectionsJson = sectionsJson;
    }

    public String getAuthorBio() {
        return authorBio;
    }

    public void setAuthorBio(String authorBio) {
        this.authorBio = authorBio;
    }

    public String getKeyTakeaways() {
        return keyTakeaways;
    }

    public void setKeyTakeaways(String keyTakeaways) {
        this.keyTakeaways = keyTakeaways;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    public Integer getReadingTimeMinutes() {
        return readingTimeMinutes;
    }

    public void setReadingTimeMinutes(Integer readingTimeMinutes) {
        this.readingTimeMinutes = readingTimeMinutes;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public Integer getGeneratedCount() {
        return generatedCount;
    }

    public void setGeneratedCount(Integer generatedCount) {
        this.generatedCount = generatedCount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

