package com.bookstore;

import com.bookstore.dto.AiPreviewRequest;
import com.bookstore.dto.AiSummaryDto;
import com.bookstore.model.Book;
import com.bookstore.model.BookVariant;
import com.bookstore.repository.BookAiSummaryRepository;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.BookVariantRepository;
import com.bookstore.service.AiBookSummaryService;
import com.bookstore.service.InventoryService;
import com.bookstore.service.PreviewDocumentService;
import com.bookstore.service.VoucherService;
import com.bookstore.model.StockReceipt;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AiBookSummaryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookVariantRepository bookVariantRepository;

    @Autowired
    private BookAiSummaryRepository summaryRepository;

    @Autowired
    private AiBookSummaryService aiBookSummaryService;

    @Autowired
    private PreviewDocumentService previewDocumentService;

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private InventoryService inventoryService;

    @Test
    void shouldRenderTikiStyleAiDrawerAndSupportSummaryChatAndQuickCart() throws Exception {
        List<Book> books = bookRepository.findAll();
        assertThat(books).hasSizeGreaterThanOrEqualTo(8);

        Book uncachedBook = books.stream()
                .filter(b -> b.getTitle().contains("Tuổi Trẻ Đáng Giá Bao Nhiêu"))
                .findFirst()
                .orElseThrow();

        // 1. Trang chi tiết sách hiển thị nút "Tóm tắt nội dung sách" và cửa sổ chat "Trợ lý AI" kiểu Tiki
        mockMvc.perform(get("/books/" + uncachedBook.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tóm tắt nội dung sách")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trợ lý AI")));

        // 2. Gọi API Tóm tắt sách kiểu Tiki -> trả về hookQuestion, sections, takeaways, authorBio
        mockMvc.perform(post("/api/books/" + uncachedBook.getId() + "/ai-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hookQuestion").isNotEmpty())
                .andExpect(jsonPath("$.sections").isArray())
                .andExpect(jsonPath("$.takeaways").isArray())
                .andExpect(jsonPath("$.authorBio").isNotEmpty());

        assertThat(summaryRepository.findByBookId(uncachedBook.getId())).isPresent();

        // 3. Gọi API Chat hỏi đáp tiếp nối trong cửa sổ Trợ lý AI
        String chatPayload = "{\"message\":\"Tư vấn chọn phiên bản bìa và giá hiện tại\",\"history\":[]}";
        mockMvc.perform(post("/api/books/" + uncachedBook.getId() + "/ai-chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chatPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").isNotEmpty())
                .andExpect(jsonPath("$.recommendedBooks").isArray());

        // 4. Thêm nhanh sách vào giỏ ngay từ thẻ sản phẩm trong khung chat Trợ lý AI
        BookVariant defVariant = bookVariantRepository.findAll().stream()
                .filter(v -> v.getBook().getId().equals(uncachedBook.getId()))
                .findFirst()
                .orElseThrow();
        mockMvc.perform(post("/cart/api/add")
                        .param("bookId", String.valueOf(uncachedBook.getId()))
                        .param("variantId", String.valueOf(defVariant.getId()))
                        .param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.cartCount").value(1));
    }

    @Test
    void shouldGenerateTikiStyleSummaryPreview() {
        AiPreviewRequest req = new AiPreviewRequest();
        req.setTitle("Lập Trình Spring Boot Thực Chiến");
        req.setAuthor("Nguyễn Văn B");
        req.setCategoryName("Công nghệ thông tin");
        req.setDescription("Hướng dẫn xây dựng hệ thống thương mại điện tử với Spring Boot 3, JPA Hibernate và cơ sở dữ liệu Oracle.");
        req.setSampleExcerpt("Chương 1: Kiến trúc MVC trong Spring Boot. Chương 2: Tối ưu truy vấn JPA và quản lý giao dịch Transaction.");

        AiSummaryDto preview = aiBookSummaryService.generatePreview(req);
        assertThat(preview.getHookQuestion()).isNotBlank();
        assertThat(preview.getSections()).hasSize(3);
        assertThat(preview.getTakeaways()).isNotEmpty();
    }

    @Test
    void shouldExtractTextFromUploadedPdfPreview() throws Exception {
        byte[] pdfBytes;
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("Chapter 1: Clean Architecture and Domain Driven Design in Java Spring Boot.");
                stream.endText();
            }
            doc.save(out);
            pdfBytes = out.toByteArray();
        }

        MockMultipartFile mockPdf = new MockMultipartFile(
                "file",
                "sample-chapter.pdf",
                "application/pdf",
                pdfBytes
        );

        PreviewDocumentService.ProcessedPreview result = previewDocumentService.processUploadedFile(mockPdf);
        assertThat(result.getFileUrl()).startsWith("/uploads/previews/");
        assertThat(result.getPageCount()).isEqualTo(1);
        assertThat(result.getExtractedText()).contains("Clean Architecture");
    }

    @Test
    void shouldValidateVoucherAndCreateStockReceipt() {
        // Kiểm tra tính toán mã giảm giá mẫu SACHMOI15 (15% tối đa 50k cho đơn từ 100k)
        VoucherService.VoucherPreviewResult preview = voucherService.validateAndPreview("sachmoi15", 200000.0);
        assertThat(preview.code()).isEqualTo("SACHMOI15");
        assertThat(preview.discountAmount()).isEqualTo(30000.0);
        assertThat(preview.finalTotal()).isEqualTo(170000.0);

        // Kiểm tra nhập kho biến thể sách và cộng dồn tồn kho
        BookVariant variant = bookVariantRepository.findAll().get(0);
        int beforeStock = variant.getStockQuantity();
        StockReceipt receipt = inventoryService.importStock(
                variant.getId(), 15, 55000.0, "NXB Trẻ", "Nhập đợt mới", "admin"
        );
        assertThat(receipt.getReceiptCode()).startsWith("NK-");
        assertThat(receipt.getStockAfter()).isEqualTo(beforeStock + 15);
    }
}
