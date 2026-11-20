package com.bookstore.repository;

import com.bookstore.model.StockReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockReceiptRepository extends JpaRepository<StockReceipt, Long> {

    List<StockReceipt> findAllByOrderByCreatedAtDesc();
}

