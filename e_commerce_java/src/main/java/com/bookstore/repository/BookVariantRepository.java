package com.bookstore.repository;

import com.bookstore.model.BookVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookVariantRepository extends JpaRepository<BookVariant, Long> {

    List<BookVariant> findByBookId(Long bookId);

    Optional<BookVariant> findByBookIdAndIsDefaultTrue(Long bookId);

    Optional<BookVariant> findBySku(String sku);
}
