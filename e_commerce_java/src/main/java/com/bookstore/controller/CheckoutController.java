package com.bookstore.controller;

import com.bookstore.dto.CartDto;
import com.bookstore.dto.CheckoutRequest;
import com.bookstore.model.Order;
import com.bookstore.model.User;
import com.bookstore.service.CartService;
import com.bookstore.service.OrderService;
import com.bookstore.service.VoucherService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CheckoutController {

    private final OrderService orderService;
    private final CartService cartService;
    private final VoucherService voucherService;

    public CheckoutController(OrderService orderService,
                              CartService cartService,
                              VoucherService voucherService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.voucherService = voucherService;
    }

    /**
     * Hiển thị trang Thanh toán (Checkout) kèm danh sách Voucher khuyến mãi đang mở
     */
    @GetMapping("/checkout")
    public String checkoutPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        CartDto cart = cartService.getCart(session);
        if (cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đang trống! Hãy chọn sách trước khi thanh toán.");
            return "redirect:/books";
        }

        User currentUser = (User) session.getAttribute("CLIENT_USER");
        CheckoutRequest checkoutRequest = new CheckoutRequest();
        if (currentUser != null) {
            checkoutRequest.setCustomerName(currentUser.getFullName());
            checkoutRequest.setCustomerPhone(currentUser.getPhone());
            checkoutRequest.setShippingAddress(currentUser.getAddress());
        }

        model.addAttribute("cart", cart);
        model.addAttribute("checkoutRequest", checkoutRequest);
        model.addAttribute("availableVouchers", voucherService.getActiveAvailableVouchers());
        return "checkout/index";
    }

    /**
     * API AJAX: Kiểm tra và tính số tiền giảm của Voucher ngay trên trang Checkout
     */
    @PostMapping("/api/vouchers/apply")
    @ResponseBody
    public ResponseEntity<?> applyVoucherApi(@RequestParam("code") String code, HttpSession session) {
        CartDto cart = cartService.getCart(session);
        if (cart.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "Giỏ hàng đang trống."));
        }
        try {
            VoucherService.VoucherPreviewResult res = voucherService.validateAndPreview(code, cart.getTotalAmount());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("valid", true);
            body.put("code", res.code());
            body.put("summaryLabel", res.summaryLabel());
            body.put("subtotal", cart.getTotalAmount());
            body.put("discountAmount", res.discountAmount());
            body.put("finalTotal", res.finalTotal());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", e.getMessage()));
        }
    }

    /**
     * Xử lý xác nhận đặt hàng
     */
    @PostMapping("/checkout")
    public String processCheckout(@Valid @ModelAttribute("checkoutRequest") CheckoutRequest request,
                                  BindingResult bindingResult,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        CartDto cart = cartService.getCart(session);
        if (cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng đã trống!");
            return "redirect:/books";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("cart", cart);
            model.addAttribute("availableVouchers", voucherService.getActiveAvailableVouchers());
            return "checkout/index";
        }

        try {
            User currentUser = (User) session.getAttribute("CLIENT_USER");
            Order createdOrder = orderService.createOrder(session, request, currentUser);
            return "redirect:/order-success/" + createdOrder.getOrderCode();
        } catch (Exception e) {
            model.addAttribute("cart", cart);
            model.addAttribute("availableVouchers", voucherService.getActiveAvailableVouchers());
            model.addAttribute("errorMessage", e.getMessage());
            return "checkout/index";
        }
    }

    /**
     * Trang thông báo Đặt hàng thành công (Tự sinh mã VietQR động nếu khách chọn chuyển khoản)
     */
    @GetMapping("/order-success/{orderCode}")
    public String orderSuccess(@PathVariable("orderCode") String orderCode, Model model) {
        Order order = orderService.getOrderByCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderCode));
        populateVietQrAttributes(order, model);
        return "checkout/success";
    }

    /**
     * Khách hàng bấm xác nhận "Tôi đã chuyển khoản thanh toán" trên trang VietQR
     */
    @PostMapping("/orders/{orderCode}/confirm-transfer")
    public String confirmBankTransfer(@PathVariable("orderCode") String orderCode,
                                      @RequestParam(value = "from", defaultValue = "success") String from,
                                      RedirectAttributes redirectAttributes) {
        orderService.markCustomerTransferred(orderCode);
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã ghi nhận thông báo chuyển khoản! Hệ thống sẽ kiểm tra đối soát và xác nhận đơn hàng của bạn sớm nhất.");
        if ("detail".equalsIgnoreCase(from)) {
            return "redirect:/orders/" + orderCode;
        }
        return "redirect:/order-success/" + orderCode;
    }

    /**
     * Xem danh sách đơn hàng đã mua của khách hàng
     */
    @GetMapping("/my-orders")
    public String myOrders(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("CLIENT_USER");
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để xem lịch sử mua hàng!");
            return "redirect:/login";
        }

        List<Order> orders = orderService.getOrdersByUser(currentUser.getId());
        model.addAttribute("orders", orders);
        return "checkout/my-orders";
    }

    /**
     * Xem chi tiết đơn hàng theo ID (tương thích link /my-orders/{id})
     */
    @GetMapping("/my-orders/{id}")
    public String myOrderDetailById(@PathVariable("id") Long id, Model model) {
        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + id));
        populateVietQrAttributes(order, model);
        return "checkout/detail";
    }

    /**
     * Xem chi tiết đơn hàng theo Mã đơn
     */
    @GetMapping("/orders/{orderCode}")
    public String orderDetail(@PathVariable("orderCode") String orderCode, Model model) {
        Order order = orderService.getOrderByCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderCode));
        populateVietQrAttributes(order, model);
        return "checkout/detail";
    }

    private void populateVietQrAttributes(Order order, Model model) {
        model.addAttribute("order", order);
        if (order.isVietQrPayment()) {
            model.addAttribute("vietQrUrl", orderService.buildVietQrImageUrl(order));
            model.addAttribute("bankId", orderService.getVietQrBankId());
            model.addAttribute("accountNo", orderService.getVietQrAccountNo());
            model.addAttribute("accountName", orderService.getVietQrAccountName());
            model.addAttribute("transferMemo", "BOOKSTORE " + order.getOrderCode());
        }
    }
}
