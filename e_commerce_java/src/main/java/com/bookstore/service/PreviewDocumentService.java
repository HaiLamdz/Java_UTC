package com.bookstore.service;

import com.bookstore.config.WebConfig;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Dịch vụ xử lý Upload File Đọc Thử (.pdf, .txt, .md):
 * 1. Lưu file vật lý vào thư mục uploads/previews/ để khách hàng mở đọc thử trực tiếp trên web.
 * 2. Tự động bóc tách văn bản (Text Extraction) bằng Apache PDFBox làm ngữ liệu đầu vào cho OpenAI tóm tắt.
 */
@Service
public class PreviewDocumentService {

    private static final int MAX_PREVIEW_PAGES = 25;
    private static final int MAX_EXTRACTED_CHARS = 15000;
    private final Path previewStorageDir = WebConfig.getUploadPath().resolve("previews").toAbsolutePath().normalize();
    private final Path coverStorageDir = WebConfig.getUploadPath().resolve("covers").toAbsolutePath().normalize();

    /**
     * Lưu ảnh bìa sách tải lên từ máy tính (.jpg, .jpeg, .png, .webp, .gif, .avif) vào uploads/covers/
     */
    public String storeCoverImage(MultipartFile imageFile) throws IOException {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn một file ảnh bìa hợp lệ.");
        }

        String originalName = imageFile.getOriginalFilename() != null ? imageFile.getOriginalFilename().trim() : "cover.jpg";
        String lowerName = originalName.toLowerCase(Locale.ROOT);

        if (!lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg") && !lowerName.endsWith(".png")
                && !lowerName.endsWith(".webp") && !lowerName.endsWith(".gif") && !lowerName.endsWith(".avif")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ định dạng ảnh bìa .jpg, .jpeg, .png, .webp hoặc .gif!");
        }

        Files.createDirectories(coverStorageDir);

        String safeSuffix = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String storedFileName = UUID.randomUUID().toString().substring(0, 8) + "_" + safeSuffix;
        Path targetPath = coverStorageDir.resolve(storedFileName).normalize();
        if (!targetPath.startsWith(coverStorageDir)) {
            throw new IllegalArgumentException("Tên file ảnh không hợp lệ.");
        }

        try (InputStream in = imageFile.getInputStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        return "/uploads/covers/" + storedFileName;
    }

    public ProcessedPreview processUploadedFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn một file đọc thử (.pdf hoặc .txt).");
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename().trim() : "preview.pdf";
        String lowerName = originalName.toLowerCase(Locale.ROOT);

        if (!lowerName.endsWith(".pdf") && !lowerName.endsWith(".txt") && !lowerName.endsWith(".md")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ định dạng file đọc thử .pdf, .txt hoặc .md!");
        }

        Files.createDirectories(previewStorageDir);

        String safeSuffix = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String storedFileName = UUID.randomUUID().toString().substring(0, 8) + "_" + safeSuffix;
        Path targetPath = previewStorageDir.resolve(storedFileName).normalize();
        if (!targetPath.startsWith(previewStorageDir)) {
            throw new IllegalArgumentException("Tên file không hợp lệ.");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        String publicUrl = "/uploads/previews/" + storedFileName;
        String extractedText;
        int pageCount = 1;

        if (lowerName.endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(targetPath.toFile())) {
                if (document.isEncrypted()) {
                    throw new IllegalArgumentException("File PDF đang đặt mật khẩu bảo vệ, không thể trích xuất văn bản.");
                }
                int totalPages = document.getNumberOfPages();
                pageCount = Math.min(totalPages, MAX_PREVIEW_PAGES);

                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(1);
                stripper.setEndPage(pageCount);
                extractedText = normalizeExtractedText(stripper.getText(document));
            }
        } else {
            String raw = Files.readString(targetPath, StandardCharsets.UTF_8);
            extractedText = normalizeExtractedText(raw);
        }

        if (extractedText.length() > MAX_EXTRACTED_CHARS) {
            extractedText = extractedText.substring(0, MAX_EXTRACTED_CHARS) + "\n...[Đã lược bớt phần sau để tối ưu ngữ cảnh AI]";
        }

        return new ProcessedPreview(
                publicUrl,
                originalName,
                extractedText,
                pageCount,
                extractedText.length()
        );
    }

    private String normalizeExtractedText(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\u0000", "")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\r?\\n{3,}", "\n\n")
                .trim();
    }

    public static class ProcessedPreview {
        private final String fileUrl;
        private final String originalFileName;
        private final String extractedText;
        private final int pageCount;
        private final int charCount;

        public ProcessedPreview(String fileUrl, String originalFileName, String extractedText, int pageCount, int charCount) {
            this.fileUrl = fileUrl;
            this.originalFileName = originalFileName;
            this.extractedText = extractedText;
            this.pageCount = pageCount;
            this.charCount = charCount;
        }

        public String getFileUrl() {
            return fileUrl;
        }

        public String getOriginalFileName() {
            return originalFileName;
        }

        public String getExtractedText() {
            return extractedText;
        }

        public int getPageCount() {
            return pageCount;
        }

        public int getCharCount() {
            return charCount;
        }
    }
}

