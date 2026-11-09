package com.bookstore.controller;

import com.bookstore.dto.AddToCartRequest;
import com.bookstore.dto.CartDto;
import com.bookstore.service.CartService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Xem trang Giỏ hàng
     */
    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        CartDto cart = cartService.getCart(session);
        model.addAttribute("cart", cart);
        return "cart/view";
    }

    /**
     * Thêm sản phẩm kèm biến thể vào giỏ hàng
     */
    @PostMapping("/add")
    public String addToCart(@Valid @ModelAttribute AddToCartRequest request,
                            BindingResult bindingResult,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu thêm vào giỏ không hợp lệ!");
            return "redirect:/books/" + request.getBookId();
        }

        try {
            cartService.addToCart(session, request.getBookId(), request.getVariantId(), request.getQuantity());
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sách vào giỏ hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/books/" + request.getBookId();
        }

        return "redirect:/cart";
    }

    /**
     * Cập nhật số lượng của biến thể trong giỏ
     */
    @PostMapping("/update")
    public String updateQuantity(@RequestParam("variantId") Long variantId,
                                 @RequestParam("quantity") Integer quantity,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        try {
            cartService.updateQuantity(session, variantId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật số lượng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cart";
    }

    /**
     * Xóa một biến thể khỏi giỏ hàng
     */
    @GetMapping("/remove/{variantId}")
    public String removeFromCart(@PathVariable("variantId") Long variantId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        cartService.removeFromCart(session, variantId);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng!");
        return "redirect:/cart";
    }

    /**
     * Xóa sạch toàn bộ giỏ hàng
     */
    @GetMapping("/clear")
    public String clearCart(HttpSession session, RedirectAttributes redirectAttributes) {
        cartService.clearCart(session);
        redirectAttributes.addFlashAttribute("successMessage", "Đã dọn sạch giỏ hàng!");
        return "redirect:/cart";
    }
}
