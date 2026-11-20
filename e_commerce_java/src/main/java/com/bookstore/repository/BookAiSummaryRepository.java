package com.bookstore.repository;

import com.bookstore.model.BookAiSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookAiSummaryRepository extends JpaRepository<BookAiSummary, Long> {

    Optional<BookAiSummary> findByBookId(Long bookId);
}

