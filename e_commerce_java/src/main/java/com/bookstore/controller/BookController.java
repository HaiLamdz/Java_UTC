package com.bookstore.controller;

import com.bookstore.model.Book;
import com.bookstore.model.BookVariant;
import com.bookstore.service.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * Trang chi tiết sách: Hiển thị thông tin sách và các biến thể (Bìa mềm, bìa cứng, bản đặc biệt)
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
