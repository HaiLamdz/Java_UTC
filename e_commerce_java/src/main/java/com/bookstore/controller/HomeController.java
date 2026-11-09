package com.bookstore.controller;

import com.bookstore.model.Book;
import com.bookstore.service.BookService;
import com.bookstore.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HomeController {

    private final BookService bookService;
    private final CategoryService categoryService;

    public HomeController(BookService bookService, CategoryService categoryService) {
        this.bookService = bookService;
        this.categoryService = categoryService;
    }

    /**
     * Trang chủ: Hiển thị banner, sách nổi bật và sách mới nhất
     */
    @GetMapping("/")
    public String home(Model model) {
        List<Book> featuredBooks = bookService.getFeaturedBooks();
        List<Book> latestBooks = bookService.getLatestBooks();

        model.addAttribute("featuredBooks", featuredBooks);
        model.addAttribute("latestBooks", latestBooks);
        return "index";
    }

    /**
     * Trang danh sách tất cả sách: Có bộ lọc theo Thể loại và Tìm kiếm từ khóa
     */
    @GetMapping("/books")
    public String listBooks(@RequestParam(value = "categoryId", required = false) Long categoryId,
                            @RequestParam(value = "keyword", required = false) String keyword,
                            Model model) {
        List<Book> books = bookService.filterBooks(categoryId, keyword);

        model.addAttribute("books", books);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("keyword", keyword);
        return "books/list";
    }
}
