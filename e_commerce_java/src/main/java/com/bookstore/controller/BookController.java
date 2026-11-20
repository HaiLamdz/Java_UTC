package com.bookstore.controller;

import com.bookstore.dto.AiChatRequest;
import com.bookstore.dto.AiChatResponse;
import com.bookstore.dto.AiSummaryDto;
import com.bookstore.model.Book;
import com.bookstore.model.BookVariant;
import com.bookstore.service.AiBookSummaryService;
import com.bookstore.service.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class BookController {

    private final BookService bookService;
    private final AiBookSummaryService aiBookSummaryService;

    public BookController(BookService bookService, AiBookSummaryService aiBookSummaryService) {
        this.bookService = bookService;
        this.aiBookSummaryService = aiBookSummaryService;
    }

    /**
     * Trang chi tiết sách: Hiển thị thông tin sách, các biến thể và Trợ lý AI kiểu Tiki
     */
    @GetMapping("/books/{id}")
    public String bookDetails(@PathVariable("id") Long id, Model model) {
        Book book = bookService.getBookById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách với ID: " + id));

        BookVariant defaultVariant = book.getDefaultVariant();

        model.addAttribute("book", book);
        model.addAttribute("defaultVariant", defaultVariant);
        return "books/detail";
    }

    /**
     * REST API: Trợ lý AI tóm tắt nội dung cuốn sách khi người đọc bấm "Xem thêm Tóm tắt nội dung sách"
     */
    @PostMapping("/api/books/{id}/ai-summary")
    @ResponseBody
    public ResponseEntity<?> generateAiSummaryApi(@PathVariable("id") Long id,
                                                  @RequestParam(value = "refresh", defaultValue = "false") boolean refresh) {
        try {
            AiSummaryDto dto = aiBookSummaryService.getOrGenerateSummary(id, refresh);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * REST API: Trợ lý AI trả lời hội thoại tiếp nối của người đọc trong khung chat (Multi-turn Q&A)
     */
    @PostMapping("/api/books/{id}/ai-chat")
    @ResponseBody
    public ResponseEntity<?> chatWithBookAiApi(@PathVariable("id") Long id,
                                               @RequestBody AiChatRequest request) {
        try {
            AiChatResponse response = aiBookSummaryService.chatAboutBook(id, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * REST API: Lấy thông tin biến thể theo ID (Dùng cho Javascript AJAX cập nhật giá & tồn kho tức thì trên giao diện)
     */
    @GetMapping("/api/variants/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getVariantApi(@PathVariable("id") Long id) {
        return bookService.getVariantById(id)
                .map(variant -> {
                    Map<String, Object> res = new HashMap<>();
                    res.put("id", variant.getId());
                    res.put("variantName", variant.getVariantName());
                    res.put("sku", variant.getSku());
                    res.put("price", variant.getPrice());
                    res.put("originalPrice", variant.getOriginalPrice());
                    res.put("stockQuantity", variant.getStockQuantity());
                    res.put("inStock", variant.isInStock());
                    res.put("discountPercent", variant.getDiscountPercent());
                    return ResponseEntity.ok(res);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
