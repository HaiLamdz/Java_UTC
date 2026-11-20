package com.bookstore.controller;

import com.bookstore.config.WebConfig;
import com.bookstore.dto.BookForm;
import com.bookstore.dto.LoginForm;
import com.bookstore.dto.UserForm;
import com.bookstore.dto.VariantForm;
import com.bookstore.model.*;
import com.bookstore.repository.BookVariantRepository;
import com.bookstore.service.BookService;
import com.bookstore.service.CategoryService;
import com.bookstore.service.InventoryService;
import com.bookstore.service.OrderService;
import com.bookstore.service.PreviewDocumentService;
import com.bookstore.service.UserService;
import com.bookstore.service.VoucherService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final BookService bookService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final BookVariantRepository bookVariantRepository;
    private final UserService userService;
    private final PreviewDocumentService previewDocumentService;
    private final VoucherService voucherService;
    private final InventoryService inventoryService;

    public AdminController(BookService bookService,
                           CategoryService categoryService,
                           OrderService orderService,
                           BookVariantRepository bookVariantRepository,
                           UserService userService,
                           PreviewDocumentService previewDocumentService,
                           VoucherService voucherService,
                           InventoryService inventoryService) {
        this.bookService = bookService;
        this.categoryService = categoryService;
        this.orderService = orderService;
        this.bookVariantRepository = bookVariantRepository;
        this.userService = userService;
        this.previewDocumentService = previewDocumentService;
        this.voucherService = voucherService;
        this.inventoryService = inventoryService;
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

        Double totalOrderValue = orders.stream()
                .filter(o -> o.getOrderStatus() != OrderStatus.CANCELLED)
                .mapToDouble(Order::getTotalAmount)
                .sum();

        int totalStockUnits = books.stream()
                .mapToInt(Book::getTotalStock)
                .sum();

        List<BookVariant> lowStockVariants = bookVariantRepository.findAll().stream()
                .filter(v -> v.getStockQuantity() != null && v.getStockQuantity() <= 15)
                .sorted((a, b) -> Integer.compare(a.getStockQuantity(), b.getStockQuantity()))
                .limit(6)
                .toList();

        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("totalVariants", totalVariants);
        model.addAttribute("totalStockUnits", totalStockUnits);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalOrderValue", totalOrderValue);
        model.addAttribute("lowStockVariants", lowStockVariants);
        model.addAttribute("dashboardChartsJson", orderService.buildDashboardChartsJson());
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
        bookForm.setSampleExcerpt(book.getSampleExcerpt());
        bookForm.setSamplePdfUrl(book.getSamplePdfUrl());
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
                    v.getIsDefault(),
                    v.getImageUrl()
            );
            vf.setId(v.getId());
            variantForms.add(vf);
        }
        bookForm.setVariants(variantForms);

        model.addAttribute("bookForm", bookForm);
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/book-form";
    }

    /**
     * API Upload ảnh bìa sách và ảnh biến thể (hỗ trợ kéo thả và chọn file)
     */
    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file,
                                                          HttpSession session) {
        if (!checkAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Bạn cần đăng nhập tài khoản quản trị viên"));
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Tệp tải lên không được để trống"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Vui lòng chỉ chọn tệp hình ảnh (JPG, PNG, WEBP, GIF,...)"));
        }

        try {
            Path uploadPath = WebConfig.getUploadPath();
            String originalFilename = file.getOriginalFilename();
            String extension = ".jpg";
            if (originalFilename != null && originalFilename.lastIndexOf('.') >= 0) {
                extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
            }

            String newFilename = "book_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6) + extension;
            Path targetFile = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            // Tự động sao chép sang target/classes/static/uploads nếu có để đồng bộ ngay lập tức
            try {
                Path targetClassesPath = uploadPath.getParent().resolve("target").resolve("classes").resolve("static").resolve("uploads");
                if (Files.exists(targetClassesPath.getParent())) {
                    if (!Files.exists(targetClassesPath)) {
                        Files.createDirectories(targetClassesPath);
                    }
                    Files.copy(targetFile, targetClassesPath.resolve(newFilename), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception ignored) {
            }

            String fileUrl = "/uploads/" + newFilename;
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "url", fileUrl,
                    "fileName", newFilename
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Lỗi khi lưu tệp: " + e.getMessage()));
        }
    }

    @PostMapping("/books/save")
    public String saveBook(@Valid @ModelAttribute("bookForm") BookForm bookForm,
                           BindingResult bindingResult,
                           @RequestParam(value = "previewFile", required = false) MultipartFile previewFile,
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
            if (previewFile != null && !previewFile.isEmpty()) {
                PreviewDocumentService.ProcessedPreview processed = previewDocumentService.processUploadedFile(previewFile);
                bookForm.setSamplePdfUrl(processed.getFileUrl());
                if (bookForm.getSampleExcerpt() == null || bookForm.getSampleExcerpt().trim().isEmpty()) {
                    bookForm.setSampleExcerpt(processed.getExtractedText());
                }
            }
            bookService.saveBookFromForm(bookForm);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu thông tin sách, biến thể và bản đọc thử thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/books";
    }

    @PostMapping("/api/books/upload-preview")
    @ResponseBody
    public ResponseEntity<?> uploadAndExtractPreview(@RequestParam("file") MultipartFile file, HttpSession session) {
        if (!checkAdmin(session)) {
            return ResponseEntity.status(401).body(Map.of("error", "Phiên quản trị không hợp lệ."));
        }
        try {
            PreviewDocumentService.ProcessedPreview result = previewDocumentService.processUploadedFile(file);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("fileUrl", result.getFileUrl());
            body.put("fileName", result.getOriginalFileName());
            body.put("extractedText", result.getExtractedText());
            body.put("pageCount", result.getPageCount());
            body.put("charCount", result.getCharCount());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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
    public String listOrders(@RequestParam(value = "userId", required = false) Long userId,
                             HttpSession session,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }
        if (userId != null) {
            model.addAttribute("orders", orderService.getOrdersByUser(userId));
            model.addAttribute("filteredUser", userService.getUserById(userId).orElse(null));
        } else {
            model.addAttribute("orders", orderService.getAllOrders());
            model.addAttribute("filteredUser", null);
        }
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("paymentStatuses", PaymentStatus.values());
        return "admin/orders";
    }

    @GetMapping("/orders/{id}")
    public String adminOrderDetail(@PathVariable("id") Long id, HttpSession session, Model model) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }
        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + id));
        model.addAttribute("order", order);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("paymentStatuses", PaymentStatus.values());
        return "admin/order-detail";
    }

    @GetMapping("/orders/{id}/invoice")
    public String printOrderInvoice(@PathVariable("id") Long id, HttpSession session, Model model) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }
        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + id));
        model.addAttribute("order", order);
        return "admin/order-invoice";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable("id") Long id,
                                    @RequestParam("status") OrderStatus status,
                                    @RequestParam(value = "returnToDetail", defaultValue = "false") boolean returnToDetail,
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
        return returnToDetail ? ("redirect:/admin/orders/" + id) : "redirect:/admin/orders";
    }

    @PostMapping("/orders/{id}/payment-status")
    public String updateOrderPaymentStatus(@PathVariable("id") Long id,
                                           @RequestParam("paymentStatus") PaymentStatus paymentStatus,
                                           @RequestParam(value = "returnToDetail", defaultValue = "true") boolean returnToDetail,
                                           HttpSession session,
                                           RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }
        try {
            orderService.updatePaymentStatus(id, paymentStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái thanh toán: " + paymentStatus.getDisplayName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật thanh toán: " + e.getMessage());
        }
        return returnToDetail ? ("redirect:/admin/orders/" + id) : "redirect:/admin/orders";
    }

    // ================= QUẢN LÝ MÃ GIẢM GIÁ / VOUCHERS =================

    @GetMapping("/vouchers")
    public String listVouchers(HttpSession session, Model model) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }
        model.addAttribute("vouchers", voucherService.getAllVouchers());
        model.addAttribute("newVoucher", new Voucher());
        model.addAttribute("discountTypes", DiscountType.values());
        return "admin/vouchers";
    }

    @PostMapping("/vouchers/save")
    public String saveVoucher(@ModelAttribute("newVoucher") Voucher voucher,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }
        try {
            voucherService.saveVoucher(voucher);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu mã giảm giá '" + voucher.getCode() + "' thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/vouchers";
    }

    @PostMapping("/vouchers/{id}/toggle")
    public String toggleVoucher(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }
        voucherService.toggleActive(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã thay đổi trạng thái hoạt động của mã giảm giá!");
        return "redirect:/admin/vouchers";
    }

    @GetMapping("/vouchers/delete/{id}")
    public String deleteVoucher(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }
        voucherService.deleteVoucher(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa mã giảm giá!");
        return "redirect:/admin/vouchers";
    }

    // ================= QUẢN LÝ NHẬP KHO SÁCH (INVENTORY) =================

    @GetMapping("/inventory")
    public String inventoryPage(@RequestParam(value = "variantId", required = false) Long preselectedVariantId,
                                HttpSession session,
                                Model model) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }
        List<BookVariant> allVariants = bookVariantRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(
                        a.getStockQuantity() != null ? a.getStockQuantity() : 0,
                        b.getStockQuantity() != null ? b.getStockQuantity() : 0))
                .toList();

        model.addAttribute("variants", allVariants);
        model.addAttribute("receipts", inventoryService.getAllReceipts());
        model.addAttribute("preselectedVariantId", preselectedVariantId);
        return "admin/inventory";
    }

    @PostMapping("/inventory/import")
    public String createInventoryReceipt(@RequestParam("variantId") Long variantId,
                                         @RequestParam("quantityAdded") Integer quantityAdded,
                                         @RequestParam(value = "importPrice", required = false) Double importPrice,
                                         @RequestParam(value = "supplierName", required = false) String supplierName,
                                         @RequestParam(value = "note", required = false) String note,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }
        try {
            User admin = (User) session.getAttribute("ADMIN_USER");
            StockReceipt receipt = inventoryService.importStock(
                    variantId, quantityAdded, importPrice, supplierName, note,
                    admin != null ? admin.getUsername() : "admin"
            );
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã lập phiếu nhập kho " + receipt.getReceiptCode() + " (+ " + quantityAdded + " cuốn cho " + receipt.getBookTitle() + ")!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi nhập kho: " + e.getMessage());
        }
        return "redirect:/admin/inventory";
    }

    // ================= QUẢN LÝ THỂ LOẠI =================

    @GetMapping("/categories")
    public String listCategories(@RequestParam(value = "editId", required = false) Long editId,
                                 HttpSession session,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }
        model.addAttribute("categories", categoryService.getAllCategories());

        Category categoryForm = new Category();
        boolean isEdit = false;
        if (editId != null) {
            Optional<Category> catOpt = categoryService.getCategoryById(editId);
            if (catOpt.isPresent()) {
                categoryForm = catOpt.get();
                isEdit = true;
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thể loại ID: " + editId);
                return "redirect:/admin/categories";
            }
        }
        model.addAttribute("newCategory", categoryForm);
        model.addAttribute("isEdit", isEdit);
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
            boolean isEdit = category.getId() != null;
            categoryService.saveCategory(category);
            redirectAttributes.addFlashAttribute("successMessage",
                    isEdit ? "Cập nhật thể loại thành công!" : "Thêm mới thể loại thành công!");
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
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa thể loại thành công! Các sách thuộc thể loại này đã được chuyển về Chưa phân loại.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xóa thể loại: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    // ================= QUẢN LÝ NGƯỜI DÙNG =================

    @GetMapping("/users")
    public String listUsers(@RequestParam(value = "keyword", required = false) String keyword,
                            HttpSession session,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập với quyền Admin!");
            return "redirect:/admin/login";
        }

        List<User> users = (keyword != null && !keyword.trim().isEmpty())
                ? userService.searchUsers(keyword)
                : userService.getAllUsers();

        long adminCount = users.stream().filter(User::isAdmin).count();
        long customerCount = users.size() - adminCount;

        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword);
        model.addAttribute("adminCount", adminCount);
        model.addAttribute("customerCount", customerCount);
        model.addAttribute("totalUsers", users.size());

        return "admin/users";
    }

    @GetMapping("/users/new")
    public String newUserForm(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }
        model.addAttribute("userForm", new UserForm());
        model.addAttribute("roles", Role.values());
        return "admin/user-form";
    }

    @GetMapping("/users/edit/{id}")
    public String editUserForm(@PathVariable("id") Long id,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }

        User user = userService.getUserById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng ID: " + id));

        UserForm form = new UserForm();
        form.setId(user.getId());
        form.setUsername(user.getUsername());
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setPhone(user.getPhone());
        form.setAddress(user.getAddress());
        form.setRole(user.getRole());

        model.addAttribute("userForm", form);
        model.addAttribute("roles", Role.values());
        model.addAttribute("userOrders", orderService.getOrdersByUser(id));
        return "admin/user-form";
    }

    @PostMapping("/users/save")
    public String saveUser(@Valid @ModelAttribute("userForm") UserForm form,
                           BindingResult bindingResult,
                           HttpSession session,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }

        // Bắt buộc mật khẩu nếu tạo mới
        if (form.getId() == null && (form.getPassword() == null || form.getPassword().trim().isEmpty())) {
            bindingResult.rejectValue("password", "error.userForm", "Mật khẩu không được để trống khi tạo mới");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "admin/user-form";
        }

        try {
            boolean isEdit = form.getId() != null;
            userService.saveUserFromAdmin(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    isEdit ? "Cập nhật tài khoản người dùng thành công!" : "Tạo mới tài khoản người dùng thành công!");
            return "redirect:/admin/users";
        } catch (Exception e) {
            model.addAttribute("roles", Role.values());
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/user-form";
        }
    }

    @PostMapping("/users/{id}/role")
    public String changeUserRole(@PathVariable("id") Long id,
                                 @RequestParam("role") Role role,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }

        User currentAdmin = (User) session.getAttribute("ADMIN_USER");
        try {
            userService.changeUserRole(id, role, currentAdmin.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi vai trò người dùng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/admin/login";
        }

        User currentAdmin = (User) session.getAttribute("ADMIN_USER");
        try {
            userService.deleteUser(id, currentAdmin.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa tài khoản người dùng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
