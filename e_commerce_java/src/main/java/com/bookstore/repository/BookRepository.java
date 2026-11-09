package com.bookstore.repository;

import com.bookstore.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * Lấy danh sách sách nổi bật
     */
    List<Book> findByFeaturedTrue();

    /**
     * Lấy danh sách sách theo thể loại
     */
    List<Book> findByCategoryId(Long categoryId);

    /**
     * Tìm kiếm sách theo tiêu đề hoặc tác giả (không phân biệt hoa thường)
     */
    List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(String title, String author);

    /**
     * Tìm kiếm kết hợp theo thể loại và từ khóa
     */
    @Query("SELECT b FROM Book b WHERE (:categoryId IS NULL OR b.category.id = :categoryId) " +
           "AND (:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Book> filterBooks(@Param("categoryId") Long categoryId, @Param("keyword") String keyword);

    /**
     * Lấy top sách mới nhất
     */
    List<Book> findTop8ByOrderByCreatedAtDesc();
}
