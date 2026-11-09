package com.bookstore.service;

import com.bookstore.dto.CartDto;
import com.bookstore.dto.CartItemDto;
import com.bookstore.model.Book;
import com.bookstore.model.BookVariant;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.BookVariantRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class CartService {

    public static final String SESSION_CART_KEY = "SHOPPING_CART";

    private final BookRepository bookRepository;
    private final BookVariantRepository bookVariantRepository;

    public CartService(BookRepository bookRepository, BookVariantRepository bookVariantRepository) {
        this.bookRepository = bookRepository;
        this.bookVariantRepository = bookVariantRepository;
    }

    /**
     * Lấy giỏ hàng từ HttpSession. Nếu chưa có thì khởi tạo mới.
     */
    public CartDto getCart(HttpSession session) {
        CartDto cart = (CartDto) session.getAttribute(SESSION_CART_KEY);
        if (cart == null) {
            cart = new CartDto();
            session.setAttribute(SESSION_CART_KEY, cart);
        }
        return cart;
    }

    /**
     * Thêm sản phẩm kèm biến thể cụ thể vào giỏ hàng
     */
    public void addToCart(HttpSession session, Long bookId, Long variantId, int quantity) {
        if (quantity <= 0) quantity = 1;

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách với ID: " + bookId));

        BookVariant variant = bookVariantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể sách với ID: " + variantId));

        // Kiểm tra biến thể có đúng thuộc sách này không
        if (!variant.getBook().getId().equals(book.getId())) {
            throw new IllegalArgumentException("Biến thể không thuộc cuốn sách này!");
        }

        CartDto cart = getCart(session);
        CartItemDto item = new CartItemDto(
                book.getId(),
                book.getTitle(),
                book.getImageUrl(),
                variant.getId(),
                variant.getVariantName(),
                variant.getPrice(),
                variant.getStockQuantity(),
                quantity
        );

        cart.addItem(item);
        session.setAttribute(SESSION_CART_KEY, cart);
    }

    /**
     * Cập nhật số lượng của một biến thể trong giỏ
     */
    public void updateQuantity(HttpSession session, Long variantId, int quantity) {
        CartDto cart = getCart(session);
        cart.updateQuantity(variantId, quantity);
        session.setAttribute(SESSION_CART_KEY, cart);
    }

    /**
     * Xóa một biến thể khỏi giỏ
     */
    public void removeFromCart(HttpSession session, Long variantId) {
        CartDto cart = getCart(session);
        cart.removeItem(variantId);
        session.setAttribute(SESSION_CART_KEY, cart);
    }

    /**
     * Xóa sạch toàn bộ giỏ hàng
     */
    public void clearCart(HttpSession session) {
        CartDto cart = getCart(session);
        cart.clear();
        session.setAttribute(SESSION_CART_KEY, cart);
    }
}
