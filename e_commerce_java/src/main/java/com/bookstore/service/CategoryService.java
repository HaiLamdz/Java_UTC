package com.bookstore.service;

import com.bookstore.model.Book;
import com.bookstore.model.Category;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;

    public CategoryService(CategoryRepository categoryRepository, BookRepository bookRepository) {
        this.categoryRepository = categoryRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public Category saveCategory(Category category) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên thể loại không được để trống");
        }
        String cleanName = category.getName().trim();

        if (category.getId() == null) {
            if (categoryRepository.existsByNameIgnoreCase(cleanName)) {
                throw new IllegalArgumentException("Thể loại '" + cleanName + "' đã tồn tại!");
            }
        } else {
            if (categoryRepository.existsByNameIgnoreCaseAndIdNot(cleanName, category.getId())) {
                throw new IllegalArgumentException("Thể loại '" + cleanName + "' đã tồn tại!");
            }
        }

        category.setName(cleanName);
        if (category.getDescription() != null) {
            category.setDescription(category.getDescription().trim());
        }
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thể loại có ID: " + id));

        // Gỡ liên kết danh mục đối với tất cả sách thuộc thể loại này
        List<Book> books = bookRepository.findByCategoryId(id);
        if (!books.isEmpty()) {
            for (Book b : books) {
                b.setCategory(null);
            }
            bookRepository.saveAll(books);
        }

        categoryRepository.delete(category);
    }
}
