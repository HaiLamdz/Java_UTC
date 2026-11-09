package com.bookstore.service;

import com.bookstore.dto.BookForm;
import com.bookstore.dto.VariantForm;
import com.bookstore.model.Book;
import com.bookstore.model.BookVariant;
import com.bookstore.model.Category;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.BookVariantRepository;
import com.bookstore.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BookService {

    private final BookRepository bookRepository;
    private final BookVariantRepository bookVariantRepository;
    private final CategoryRepository categoryRepository;

    public BookService(BookRepository bookRepository,
                       BookVariantRepository bookVariantRepository,
                       CategoryRepository categoryRepository) {
        this.bookRepository = bookRepository;
        this.bookVariantRepository = bookVariantRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Book> getFeaturedBooks() {
        return bookRepository.findByFeaturedTrue();
    }

    @Transactional(readOnly = true)
    public List<Book> getLatestBooks() {
        return bookRepository.findTop8ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Book> filterBooks(Long categoryId, String keyword) {
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }
        return bookRepository.filterBooks(categoryId, keyword);
    }

    @Transactional(readOnly = true)
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<BookVariant> getVariantById(Long variantId) {
        return bookVariantRepository.findById(variantId);
    }

    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }

    /**
     * Lưu hoặc cập nhật Sách kèm danh sách các Biến thể từ BookForm trong trang Admin
     */
    public Book saveBookFromForm(BookForm form) {
        Book book;
        if (form.getId() != null) {
            book = bookRepository.findById(form.getId()).orElse(new Book());
        } else {
            book = new Book();
        }

        book.setTitle(form.getTitle());
        book.setAuthor(form.getAuthor());
        book.setPublisher(form.getPublisher());
        book.setPublishYear(form.getPublishYear());
        book.setIsbn(form.getIsbn());
        book.setDescription(form.getDescription());
        book.setImageUrl(form.getImageUrl());
        book.setFeatured(form.getFeatured());

        if (form.getCategoryId() != null) {
            Category category = categoryRepository.findById(form.getCategoryId()).orElse(null);
            book.setCategory(category);
        }

        // Xử lý các biến thể
        book.getVariants().clear();
        if (form.getVariants() != null && !form.getVariants().isEmpty()) {
            boolean hasDefault = false;
            for (VariantForm vf : form.getVariants()) {
                if (vf.getVariantName() != null && !vf.getVariantName().trim().isEmpty()) {
                    BookVariant variant = new BookVariant();
                    variant.setId(vf.getId());
                    variant.setVariantName(vf.getVariantName().trim());
                    variant.setSku(vf.getSku());
                    variant.setPrice(vf.getPrice() != null ? vf.getPrice() : 0.0);
                    variant.setOriginalPrice(vf.getOriginalPrice());
                    variant.setStockQuantity(vf.getStockQuantity() != null ? vf.getStockQuantity() : 0);
                    
                    if (vf.getIsDefault() && !hasDefault) {
                        variant.setIsDefault(true);
                        hasDefault = true;
                    } else {
                        variant.setIsDefault(false);
                    }
                    book.addVariant(variant);
                }
            }
            // Nếu chưa có biến thể mặc định thì gán biến thể đầu tiên làm mặc định
            if (!hasDefault && !book.getVariants().isEmpty()) {
                book.getVariants().get(0).setIsDefault(true);
            }
        }

        return bookRepository.save(book);
    }

    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }
}
