package com.bookstore.service;

import com.bookstore.dto.CartDto;
import com.bookstore.dto.CartItemDto;
import com.bookstore.dto.CheckoutRequest;
import com.bookstore.model.*;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.BookVariantRepository;
import com.bookstore.repository.CategoryRepository;
import com.bookstore.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final BookVariantRepository bookVariantRepository;
    private final CategoryRepository categoryRepository;
    private final CartService cartService;
    private final VoucherService voucherService;
    private final ObjectMapper objectMapper;

    @Value("${payment.vietqr.bank-id:MB}")
    private String vietQrBankId;

    @Value("${payment.vietqr.account-no:0388889999}")
    private String vietQrAccountNo;

    @Value("${payment.vietqr.account-name:BOOKSTORE AN BAN TRI THUC}")
    private String vietQrAccountName;

    public OrderService(OrderRepository orderRepository,
                        BookRepository bookRepository,
                        BookVariantRepository bookVariantRepository,
                        CategoryRepository categoryRepository,
                        CartService cartService,
                        VoucherService voucherService,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.bookRepository = bookRepository;
        this.bookVariantRepository = bookVariantRepository;
        this.categoryRepository = categoryRepository;
        this.cartService = cartService;
        this.voucherService = voucherService;
        this.objectMapper = objectMapper;
    }

    /**
     * Tạo đơn hàng mới từ giỏ hàng hiện tại
     */
    public Order createOrder(HttpSession session, CheckoutRequest request, User currentUser) {
        CartDto cart = cartService.getCart(session);
        if (cart.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng của bạn đang trống, không thể đặt hàng!");
        }

        // Tạo mã đơn hàng duy nhất, ví dụ: ORD-20261031-4829
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomSuffix = ThreadLocalRandom.current().nextInt(1000, 9999);
        String orderCode = "ORD-" + dateStr + "-" + randomSuffix;

        Order order = new Order();
        order.setOrderCode(orderCode);
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setShippingAddress(request.getShippingAddress());
        order.setNote(request.getNote());
        order.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "COD");
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setUser(currentUser);

        // Duyệt từng mặt hàng trong giỏ hàng để tạo OrderItem và trừ kho
        for (CartItemDto cartItem : cart.getItems()) {
            Book book = bookRepository.findById(cartItem.getBookId()).orElse(null);
            BookVariant variant = bookVariantRepository.findById(cartItem.getVariantId())
                    .orElseThrow(() -> new IllegalArgumentException("Biến thể không tồn tại: " + cartItem.getVariantId()));

            // Kiểm tra số lượng tồn kho
            if (variant.getStockQuantity() < cartItem.getQuantity()) {
                throw new IllegalStateException("Sách \"" + cartItem.getBookTitle() + "\" phiên bản \""
                        + cartItem.getVariantName() + "\" chỉ còn lại " + variant.getStockQuantity() + " cuốn trong kho.");
            }

            // Trừ tồn kho biến thể
            variant.setStockQuantity(variant.getStockQuantity() - cartItem.getQuantity());
            bookVariantRepository.save(variant);

            // Tạo chi tiết đơn hàng
            OrderItem orderItem = new OrderItem(order, book, variant, cartItem.getQuantity());
            order.addItem(orderItem);
        }

        // Áp dụng mã giảm giá (nếu khách hàng có nhập)
        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            VoucherService.VoucherPreviewResult voucherRes =
                    voucherService.consumeVoucherForOrder(request.getVoucherCode(), cart.getTotalAmount());
            order.setVoucherCode(voucherRes.code());
            order.setDiscountAmount(voucherRes.discountAmount());
        } else {
            order.setDiscountAmount(0.0);
        }

        order.calculateTotal();
        Order savedOrder = orderRepository.save(order);

        // Xóa giỏ hàng sau khi đặt thành công
        cartService.clearCart(session);

        return savedOrder;
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderByCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode);
    }

    /**
     * Cập nhật trạng thái đơn hàng (Tự động hoàn lại tồn kho nếu Hủy đơn, hoặc đánh dấu Đã thu tiền khi Hoàn thành COD)
     */
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + orderId));
        OrderStatus oldStatus = order.getOrderStatus();

        // Nếu hủy đơn hàng chưa hủy trước đó -> hoàn lại số lượng tồn kho cho từng biến thể
        if (newStatus == OrderStatus.CANCELLED && oldStatus != OrderStatus.CANCELLED && order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (item.getVariant() != null && item.getQuantity() != null) {
                    BookVariant variant = item.getVariant();
                    int current = variant.getStockQuantity() != null ? variant.getStockQuantity() : 0;
                    variant.setStockQuantity(current + item.getQuantity());
                    bookVariantRepository.save(variant);
                }
            }
        }

        if (newStatus == OrderStatus.COMPLETED && order.getPaymentStatus() == PaymentStatus.UNPAID) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }

        order.setOrderStatus(newStatus);
        return orderRepository.save(order);
    }

    public Order updatePaymentStatus(Long orderId, PaymentStatus newPaymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + orderId));
        order.setPaymentStatus(newPaymentStatus);
        if (newPaymentStatus == PaymentStatus.PAID && order.getOrderStatus() == OrderStatus.PENDING) {
            order.setOrderStatus(OrderStatus.CONFIRMED);
        }
        return orderRepository.save(order);
    }

    public Order markCustomerTransferred(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã đơn: " + orderCode));
        if (order.getPaymentStatus() == PaymentStatus.UNPAID) {
            order.setPaymentStatus(PaymentStatus.WAITING_CONFIRM);
            return orderRepository.save(order);
        }
        return order;
    }

    public String buildVietQrImageUrl(Order order) {
        long amount = Math.round(order.getTotalAmount() != null ? order.getTotalAmount() : 0.0);
        String memo = "BOOKSTORE " + order.getOrderCode();
        String encodedMemo = URLEncoder.encode(memo, StandardCharsets.UTF_8);
        String encodedName = URLEncoder.encode(vietQrAccountName, StandardCharsets.UTF_8);
        return "https://img.vietqr.io/image/" + vietQrBankId + "-" + vietQrAccountNo
                + "-compact2.png?amount=" + amount
                + "&addInfo=" + encodedMemo
                + "&accountName=" + encodedName;
    }

    public String getVietQrBankId() { return vietQrBankId; }
    public String getVietQrAccountNo() { return vietQrAccountNo; }
    public String getVietQrAccountName() { return vietQrAccountName; }

    @Transactional(readOnly = true)
    public long countPendingOrders() {
        return orderRepository.countByOrderStatus(OrderStatus.PENDING);
    }

    /**
     * Tổng hợp dữ liệu thống kê biểu đồ trực quan cho Bảng Điều Khiển Quản Trị (Admin Dashboard)
     */
    @Transactional(readOnly = true)
    public String buildDashboardChartsJson() {
        List<Order> allOrders = orderRepository.findAllByOrderByCreatedAtDesc();
        List<Book> allBooks = bookRepository.findAll();
        List<Category> categories = categoryRepository.findAll();
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd/MM");

        // 1. Biểu đồ Doanh thu & Lượng đơn theo ngày (sắp xếp tăng dần theo mốc thời gian)
        TreeMap<LocalDate, double[]> dailyMap = new TreeMap<>();
        for (Order o : allOrders) {
            if (o.getCreatedAt() != null && o.getOrderStatus() != OrderStatus.CANCELLED) {
                LocalDate date = o.getCreatedAt().toLocalDate();
                double[] agg = dailyMap.computeIfAbsent(date, k -> new double[]{0.0, 0.0});
                agg[0] += (o.getTotalAmount() != null ? o.getTotalAmount() : 0.0);
                agg[1] += 1.0;
            }
        }
        if (dailyMap.isEmpty()) {
            LocalDate today = LocalDate.now();
            for (int i = 6; i >= 0; i--) {
                dailyMap.put(today.minusDays(i), new double[]{0.0, 0.0});
            }
        }

        List<LocalDate> dateKeys = new ArrayList<>(dailyMap.keySet());
        if (dateKeys.size() > 10) {
            dateKeys = dateKeys.subList(dateKeys.size() - 10, dateKeys.size());
        }
        List<String> revenueLabels = new ArrayList<>();
        List<Double> revenueValues = new ArrayList<>();
        List<Integer> orderCountValues = new ArrayList<>();
        for (LocalDate d : dateKeys) {
            double[] agg = dailyMap.get(d);
            revenueLabels.add(d.format(dayFmt));
            revenueValues.add(agg[0]);
            orderCountValues.add((int) agg[1]);
        }

        // 2. Biểu đồ Trạng thái đơn hàng (Doughnut)
        List<String> statusLabels = new ArrayList<>();
        List<Long> statusCounts = new ArrayList<>();
        for (OrderStatus st : OrderStatus.values()) {
            long cnt = allOrders.stream().filter(o -> o.getOrderStatus() == st).count();
            statusLabels.add(st.getDisplayName());
            statusCounts.add(cnt);
        }

        // 3. Top đầu sách bán chạy theo doanh thu thực tế từ OrderItem
        Map<String, double[]> bookSalesMap = new LinkedHashMap<>();
        for (Order o : allOrders) {
            if (o.getOrderStatus() == OrderStatus.CANCELLED || o.getItems() == null) {
                continue;
            }
            for (OrderItem item : o.getItems()) {
                String shortTitle = shortenBookTitle(item.getBookTitle());
                double[] sales = bookSalesMap.computeIfAbsent(shortTitle, k -> new double[]{0.0, 0.0});
                sales[0] += (item.getSubtotal() != null ? item.getSubtotal() : 0.0);
                sales[1] += (item.getQuantity() != null ? item.getQuantity() : 0);
            }
        }

        List<Map.Entry<String, double[]>> sortedBooks = new ArrayList<>(bookSalesMap.entrySet());
        sortedBooks.sort((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]));
        if (sortedBooks.size() > 6) {
            sortedBooks = sortedBooks.subList(0, 6);
        }

        List<String> topBookLabels = new ArrayList<>();
        List<Double> topBookRevenues = new ArrayList<>();
        List<Integer> topBookQuantities = new ArrayList<>();
        for (Map.Entry<String, double[]> entry : sortedBooks) {
            topBookLabels.add(entry.getKey());
            topBookRevenues.add(entry.getValue()[0]);
            topBookQuantities.add((int) entry.getValue()[1]);
        }

        // 4. Biểu đồ Phân bố tồn kho & số đầu sách theo Thể loại
        List<String> catLabels = new ArrayList<>();
        List<Integer> catStockTotals = new ArrayList<>();
        List<Integer> catBookCounts = new ArrayList<>();
        for (Category cat : categories) {
            int bookCnt = 0;
            int stockSum = 0;
            for (Book b : allBooks) {
                if (b.getCategory() != null && Objects.equals(b.getCategory().getId(), cat.getId())) {
                    bookCnt++;
                    stockSum += b.getTotalStock();
                }
            }
            catLabels.add(cat.getName());
            catStockTotals.add(stockSum);
            catBookCounts.add(bookCnt);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("revenueLabels", revenueLabels);
        payload.put("revenueValues", revenueValues);
        payload.put("orderCountValues", orderCountValues);
        payload.put("statusLabels", statusLabels);
        payload.put("statusCounts", statusCounts);
        payload.put("topBookLabels", topBookLabels);
        payload.put("topBookRevenues", topBookRevenues);
        payload.put("topBookQuantities", topBookQuantities);
        payload.put("categoryLabels", catLabels);
        payload.put("categoryStocks", catStockTotals);
        payload.put("categoryBookCounts", catBookCounts);

        try {
            return objectMapper.writeValueAsString(payload).replace("</", "<\\/");
        } catch (Exception e) {
            return "{}";
        }
    }

    private String shortenBookTitle(String raw) {
        if (raw == null) return "Sách";
        String clean = raw.replaceAll("\\s*\\(.*?\\)", "").trim();
        if (clean.length() > 34) {
            return clean.substring(0, 31) + "...";
        }
        return clean;
    }
}
