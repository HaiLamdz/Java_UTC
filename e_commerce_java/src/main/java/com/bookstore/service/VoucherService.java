package com.bookstore.service;

import com.bookstore.model.DiscountType;
import com.bookstore.model.Voucher;
import com.bookstore.repository.VoucherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional
public class VoucherService {

    private final VoucherRepository voucherRepository;

    public VoucherService(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    @Transactional(readOnly = true)
    public List<Voucher> getAllVouchers() {
        return voucherRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Voucher> getActiveAvailableVouchers() {
        return voucherRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(Voucher::isValidNow)
                .toList();
    }

    public Voucher saveVoucher(Voucher voucher) {
        if (voucher.getCode() == null || voucher.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã giảm giá không được để trống!");
        }
        String normalizedCode = voucher.getCode().trim().toUpperCase(Locale.ROOT);
        Optional<Voucher> existing = voucherRepository.findByCodeIgnoreCase(normalizedCode);
        if (existing.isPresent() && !existing.get().getId().equals(voucher.getId())) {
            throw new IllegalArgumentException("Mã giảm giá '" + normalizedCode + "' đã tồn tại!");
        }
        voucher.setCode(normalizedCode);
        if (voucher.getUsedCount() == null) {
            voucher.setUsedCount(0);
        }
        if (voucher.getStartDate() == null) {
            voucher.setStartDate(LocalDate.now());
        }
        return voucherRepository.save(voucher);
    }

    public void toggleActive(Long id) {
        Voucher v = voucherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã giảm giá!"));
        v.setActive(!v.getActive());
        voucherRepository.save(v);
    }

    public void deleteVoucher(Long id) {
        voucherRepository.deleteById(id);
    }

    /**
     * Kiểm tra điều kiện mã giảm giá và tính số tiền được giảm trên tổng tạm tính giỏ hàng
     */
    @Transactional(readOnly = true)
    public VoucherPreviewResult validateAndPreview(String rawCode, double subtotal) {
        if (rawCode == null || rawCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mã giảm giá.");
        }
        String code = rawCode.trim().toUpperCase(Locale.ROOT);
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá '" + code + "' không tồn tại!"));

        if (!voucher.getActive()) {
            throw new IllegalArgumentException("Mã giảm giá '" + code + "' hiện đang tạm ngưng áp dụng.");
        }
        LocalDate today = LocalDate.now();
        if (voucher.getStartDate() != null && today.isBefore(voucher.getStartDate())) {
            throw new IllegalArgumentException("Mã giảm giá '" + code + "' chưa đến ngày mở hiệu lực.");
        }
        if (voucher.getEndDate() != null && today.isAfter(voucher.getEndDate())) {
            throw new IllegalArgumentException("Mã giảm giá '" + code + "' đã hết hạn sử dụng.");
        }
        if (voucher.getUsageLimit() != null && voucher.getUsageLimit() > 0
                && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new IllegalArgumentException("Mã giảm giá '" + code + "' đã hết lượt sử dụng.");
        }

        double minReq = voucher.getMinOrderAmount() != null ? voucher.getMinOrderAmount() : 0.0;
        if (subtotal < minReq) {
            NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
            throw new IllegalArgumentException("Mã '" + code + "' yêu cầu đơn hàng tối thiểu từ " + nf.format(minReq) + " đ.");
        }

        double discount = voucher.calculateDiscount(subtotal);
        double finalTotal = Math.max(0.0, subtotal - discount);

        String label = voucher.getDiscountType() == DiscountType.PERCENT
                ? ("Giảm " + voucher.getDiscountValue().intValue() + "%")
                : ("Giảm " + NumberFormat.getInstance(new Locale("vi", "VN")).format(voucher.getDiscountValue()) + " đ");

        return new VoucherPreviewResult(voucher.getCode(), label, discount, finalTotal);
    }

    /**
     * Tiêu thụ 1 lượt dùng của mã giảm giá khi xác nhận tạo đơn hàng
     */
    public VoucherPreviewResult consumeVoucherForOrder(String rawCode, double subtotal) {
        VoucherPreviewResult preview = validateAndPreview(rawCode, subtotal);
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(preview.code()).orElseThrow();
        voucher.setUsedCount(voucher.getUsedCount() + 1);
        voucherRepository.save(voucher);
        return preview;
    }

    public record VoucherPreviewResult(String code, String summaryLabel, double discountAmount, double finalTotal) {
    }
}

