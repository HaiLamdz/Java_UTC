package com.bookstore.controller;

import com.bookstore.dto.CartDto;
import com.bookstore.dto.CheckoutRequest;
import com.bookstore.model.Order;
import com.bookstore.model.User;
import com.bookstore.service.CartService;
import com.bookstore.service.OrderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class CheckoutController {

    private final OrderService orderService;
    private final CartService cartService;

    public CheckoutController(OrderService orderService, CartService cartService) {
        this.orderService = orderService;
        this.cartService = cartService;
    }

    /**
     * Hiển thị trang Thanh toán (Checkout)
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
        return "checkout/index";
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
            return "checkout/index";
        }

        try {
            User currentUser = (User) session.getAttribute("CLIENT_USER");
            Order createdOrder = orderService.createOrder(session, request, currentUser);
            return "redirect:/order-success/" + createdOrder.getOrderCode();
        } catch (Exception e) {
            model.addAttribute("cart", cart);
            model.addAttribute("errorMessage", e.getMessage());
            return "checkout/index";
        }
    }

    /**
     * Trang thông báo Đặt hàng thành công
     */
    @GetMapping("/order-success/{orderCode}")
    public String orderSuccess(@PathVariable("orderCode") String orderCode, Model model) {
        Order order = orderService.getOrderByCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderCode));
        model.addAttribute("order", order);
        return "checkout/success";
    }

    /**
     * Xem danh sách đơn hàng đã mua của khách hàng (Lịch sử mua hàng)
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
     * Xem chi tiết một đơn hàng theo mã đơn
     */
    @GetMapping("/orders/{orderCode}")
    public String orderDetail(@PathVariable("orderCode") String orderCode, Model model) {
        Order order = orderService.getOrderByCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderCode));
        model.addAttribute("order", order);
        return "checkout/detail";
    }
}
