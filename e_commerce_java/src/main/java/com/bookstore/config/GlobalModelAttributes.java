package com.bookstore.config;

import com.bookstore.dto.CartDto;
import com.bookstore.model.Category;
import com.bookstore.model.User;
import com.bookstore.service.CartService;
import com.bookstore.service.CategoryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * ControllerAdvice tự động cung cấp các thuộc tính dùng chung cho MỌI trang giao diện:
 * - currentUser: Thông tin người dùng đang đăng nhập (hoặc null nếu chưa đăng nhập)
 * - cartCount: Tổng số lượng sản phẩm trong giỏ hàng
 * - globalCategories: Danh sách thể loại sách để hiển thị trên thanh menu điều hướng
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final CategoryService categoryService;
    private final CartService cartService;

    public GlobalModelAttributes(CategoryService categoryService, CartService cartService) {
        this.categoryService = categoryService;
        this.cartService = cartService;
    }

    @ModelAttribute("currentUser")
    public User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("CLIENT_USER");
    }

    @ModelAttribute("adminUser")
    public User getAdminUser(HttpSession session) {
        return (User) session.getAttribute("ADMIN_USER");
    }

    @ModelAttribute("cartCount")
    public int getCartCount(HttpSession session) {
        CartDto cart = (CartDto) session.getAttribute(CartService.SESSION_CART_KEY);
        return cart != null ? cart.getTotalQuantity() : 0;
    }

    @ModelAttribute("globalCategories")
    public List<Category> getGlobalCategories() {
        return categoryService.getAllCategories();
    }
}
