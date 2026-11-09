package com.bookstore.controller;

import com.bookstore.dto.BookForm;
import com.bookstore.dto.LoginForm;
import com.bookstore.dto.VariantForm;
import com.bookstore.model.*;
import com.bookstore.repository.BookVariantRepository;
import com.bookstore.service.BookService;
import com.bookstore.service.CategoryService;
import com.bookstore.service.OrderService;
import com.bookstore.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final BookService bookService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final BookVariantRepository bookVariantRepository;
    private final UserService userService;

    public AdminController(BookService bookService,
                           CategoryService categoryService,
                           OrderService orderService,
                           BookVariantRepository bookVariantRepository,
                           UserService userService) {
        this.bookService = bookService;
        this.categoryService = categoryService;
        this.orderService = orderService;
        this.bookVariantRepository = bookVariantRepository;
        this.userService = userService;
    }

    /**
     * Kiểm tra phiên đăng nhập độc lập của Quản trị viên (ADMIN_USER)
     */
    private boolean checkAdmin(HttpSession session) {
        User user = (User) session.getAttribute("ADMIN_USER");
        return user != null && user.isAdmin();
    }

    /**
     * Màn hình đăng nhập riêng biệt của trang Quản trị (/admin/login)
     */
    @GetMapping("/login")
    public String adminLoginPage(Model model, HttpSession session) {
        if (checkAdmin(session)) {
            return "redirect:/admin";
        }
        model.addAttribute("loginForm", new LoginForm());
        return "admin/login";
    }

    /**
     * Xử lý đăng nhập riêng cho Quản trị viên
     */
    @PostMapping("/login")
    public String processAdminLogin(@Valid @ModelAttribute("loginForm") LoginForm form,
                                    BindingResult bindingResult,
                                    HttpSession session,
                                    Model model) {
        if (bindingResult.hasErrors()) {
            return "admin/login";
        }

        Optional<User> userOpt = userService.authenticate(form.getUsername(), form.getPassword());
        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            model.addAttribute("errorMessage", "Tài khoản quản trị hoặc mật khẩu không hợp lệ!");
            return "admin/login";
        }

        session.setAttribute("ADMIN_USER", userOpt.get());
        return "redirect:/admin";
    }

    /**
     * Đăng xuất khỏi hệ thống Quản trị (/admin/logout)
     */
    @GetMapping("/logout")
    public String adminLogout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.removeAttribute("ADMIN_USER");
        redirectAttributes.addFlashAttribute("successMessage", "Đã đăng xuất khỏi phiên quản trị.");
        return "redirect:/admin/login";
    }

    /**
     * Bảng điều khiển tổng quan (Dashboard)
     */
    @GetMapping
    public String dashboard(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần đăng nhập tài khoản Quản trị viên để truy cập!");
            return "redirect:/admin/login";
        }

        List<Book> books = bookService.getAllBooks();
        List<Order> orders = orderService.getAllOrders();

        long totalBooks = books.size();
        long totalVariants = bookVariantRepository.count();
        long totalOrders = orders.size();
        long pendingOrders = orderService.countPendingOrders();

        Double totalRevenue = orders.stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.COMPLETED)
                .mapToDouble(Order::getTotalAmount)
                .sum();

        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("totalVariants", totalVariants);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("recentOrders", orders.stream().limit(5).toList());

        return "admin/dashboard";
    }

    // ================= QUẢN LÝ SÁCH & BIẾN THỂ =================

    @GetMapping("/books")
    public String listBooks(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập với quyền Admin!");
            return "redirect:/admin/login";
        }
        model.addAttribute("books", bookService.getAllBooks());
        return "admin/books";
    }

    @GetMapping("/books/new")
    public String newBookForm(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }

        BookForm bookForm = new BookForm();
        // Mặc định tạo sẵn 2 biến thể thông dụng: Bìa mềm & Bìa cứng
        bookForm.getVariants().clear();
        bookForm.getVariants().add(new VariantForm("Bìa mềm", "SKU-BM-01", 90000.0, 110000.0, 20, true));
        bookForm.getVariants().add(new VariantForm("Bìa cứng cao cấp", "SKU-BC-02", 150000.0, 180000.0, 10, false));

        model.addAttribute("bookForm", bookForm);
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/book-form";
    }

    @GetMapping("/books/edit/{id}")
    public String editBookForm(@PathVariable("id") Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }

        Book book = bookService.getBookById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách: " + id));

        BookForm bookForm = new BookForm();
        bookForm.setId(book.getId());
        bookForm.setTitle(book.getTitle());
        bookForm.setAuthor(book.getAuthor());
        bookForm.setPublisher(book.getPublisher());
        bookForm.setPublishYear(book.getPublishYear());
        bookForm.setIsbn(book.getIsbn());
        bookForm.setDescription(book.getDescription());
        bookForm.setImageUrl(book.getImageUrl());
        bookForm.setCategoryId(book.getCategory() != null ? book.getCategory().getId() : null);
        bookForm.setFeatured(book.getFeatured());

        List<VariantForm> variantForms = new ArrayList<>();
        for (BookVariant v : book.getVariants()) {
            VariantForm vf = new VariantForm(
                    v.getVariantName(),
                    v.getSku(),
                    v.getPrice(),
                    v.getOriginalPrice(),
                    v.getStockQuantity(),
                    v.getIsDefault()
            );
            vf.setId(v.getId());
            variantForms.add(vf);
        }
        bookForm.setVariants(variantForms);

        model.addAttribute("bookForm", bookForm);
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/book-form";
    }

    @PostMapping("/books/save")
    public String saveBook(@Valid @ModelAttribute("bookForm") BookForm bookForm,
                           BindingResult bindingResult,
                           HttpSession session,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin/book-form";
        }

        try {
            bookService.saveBookFromForm(bookForm);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu thông tin sách và các biến thể thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/books";
    }

    @GetMapping("/books/delete/{id}")
    public String deleteBook(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }
        try {
            bookService.deleteBook(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sách thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xóa sách: " + e.getMessage());
        }
        return "redirect:/admin/books";
    }

    // ================= QUẢN LÝ ĐƠN HÀNG =================

    @GetMapping("/orders")
    public String listOrders(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }
        model.addAttribute("orders", orderService.getAllOrders());
        model.addAttribute("statuses", OrderStatus.values());
        return "admin/orders";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable("id") Long id,
                                    @RequestParam("status") OrderStatus status,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }
        try {
            orderService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    // ================= QUẢN LÝ THỂ LOẠI =================

    @GetMapping("/categories")
    public String listCategories(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("newCategory", new Category());
        return "admin/categories";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute("newCategory") Category category,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }
        try {
            categoryService.saveCategory(category);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu thể loại thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi lưu thể loại: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa thể loại!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa thể loại đang có sách liên kết!");
        }
        return "redirect:/admin/categories";
    }
}
