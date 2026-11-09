package com.bookstore.service;

import com.bookstore.dto.CartDto;
import com.bookstore.dto.CartItemDto;
import com.bookstore.dto.CheckoutRequest;
import com.bookstore.model.*;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.BookVariantRepository;
import com.bookstore.repository.OrderRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final BookVariantRepository bookVariantRepository;
    private final CartService cartService;

    public OrderService(OrderRepository orderRepository,
                        BookRepository bookRepository,
                        BookVariantRepository bookVariantRepository,
                        CartService cartService) {
        this.orderRepository = orderRepository;
        this.bookRepository = bookRepository;
        this.bookVariantRepository = bookVariantRepository;
        this.cartService = cartService;
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
        order.setPaymentMethod(request.getPaymentMethod());
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
     * Cập nhật trạng thái đơn hàng (Dành cho Quản trị viên)
     */
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + orderId));
        order.setOrderStatus(newStatus);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public long countPendingOrders() {
        return orderRepository.countByOrderStatus(OrderStatus.PENDING);
    }
}
