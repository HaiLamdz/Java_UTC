package com.bookstore.service;

import com.bookstore.model.BookVariant;
import com.bookstore.model.StockReceipt;
import com.bookstore.repository.BookVariantRepository;
import com.bookstore.repository.StockReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class InventoryService {

    private final StockReceiptRepository stockReceiptRepository;
    private final BookVariantRepository bookVariantRepository;

    public InventoryService(StockReceiptRepository stockReceiptRepository,
                            BookVariantRepository bookVariantRepository) {
        this.stockReceiptRepository = stockReceiptRepository;
        this.bookVariantRepository = bookVariantRepository;
    }

    @Transactional(readOnly = true)
    public List<StockReceipt> getAllReceipts() {
        return stockReceiptRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Lập phiếu nhập kho: Cộng dồn số lượng tồn kho cho biến thể sách và lưu lịch sử phiếu nhập
     */
    public StockReceipt importStock(Long variantId, Integer quantityAdded, Double importPrice,
                                    String supplierName, String note, String createdBy) {
        if (variantId == null) {
            throw new IllegalArgumentException("Vui lòng chọn phiên bản sách cần nhập kho.");
        }
        if (quantityAdded == null || quantityAdded <= 0) {
            throw new IllegalArgumentException("Số lượng nhập kho phải lớn hơn 0.");
        }

        BookVariant variant = bookVariantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên bản sách ID: " + variantId));

        int currentStock = variant.getStockQuantity() != null ? variant.getStockQuantity() : 0;
        int updatedStock = currentStock + quantityAdded;
        variant.setStockQuantity(updatedStock);
        bookVariantRepository.save(variant);

        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String code = "NK-" + datePart + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);

        StockReceipt receipt = new StockReceipt();
        receipt.setReceiptCode(code);
        receipt.setVariant(variant);
        receipt.setBookTitle(variant.getBook() != null ? variant.getBook().getTitle() : "Sách");
        receipt.setVariantName(variant.getVariantName());
        receipt.setSku(variant.getSku());
        receipt.setQuantityAdded(quantityAdded);
        receipt.setStockAfter(updatedStock);
        receipt.setImportPrice(importPrice != null && importPrice >= 0 ? importPrice : (variant.getPrice() * 0.65));
        receipt.setSupplierName(supplierName != null && !supplierName.isBlank() ? supplierName.trim() : "Nhà phát hành chính thức");
        receipt.setNote(note != null ? note.trim() : "");
        receipt.setCreatedBy(createdBy != null ? createdBy : "admin");
        receipt.setCreatedAt(LocalDateTime.now());

        return stockReceiptRepository.save(receipt);
    }
}

