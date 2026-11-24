package com.bookstore.config;

import com.bookstore.model.*;
import com.bookstore.repository.BookAiSummaryRepository;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CategoryRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.repository.VoucherRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Khởi tạo dữ liệu mẫu ban đầu (Data Seeder).
 * Tự động chạy khi khởi động ứng dụng nếu cơ sở dữ liệu chưa có dữ liệu.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final BookAiSummaryRepository bookAiSummaryRepository;
    private final VoucherRepository voucherRepository;
    private final OrderRepository orderRepository;

    public DataInitializer(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           BookRepository bookRepository,
                           BookAiSummaryRepository bookAiSummaryRepository,
                           VoucherRepository voucherRepository,
                           OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.bookRepository = bookRepository;
        this.bookAiSummaryRepository = bookAiSummaryRepository;
        this.voucherRepository = voucherRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedInitialVouchersIfEmpty();
        if (categoryRepository.count() > 0) {
            seedInitialAiSummariesForExistingDb();
            return; // Đã có dữ liệu thì chỉ bổ sung AI Summary & Voucher mẫu nếu bảng mới còn trống
        }

        System.out.println(">>> Đang nạp dữ liệu mẫu cho Cửa hàng sách...");

        // 1. Tạo tài khoản mẫu
        User admin = new User(
                "admin",
                "123456",
                "Quản Trị Viên Hệ Thống",
                "admin@bookstore.com",
                "0988888888",
                "Phòng Quản Trị, Hà Nội",
                Role.ROLE_ADMIN
        );
        userRepository.save(admin);

        User customer = new User(
                "nguyenvana",
                "123456",
                "Nguyễn Văn A",
                "nguyenvana@gmail.com",
                "0912345678",
                "Số 123 Đường Cầu Giấy, Hà Nội",
                Role.ROLE_CUSTOMER
        );
        userRepository.save(customer);

        User customer2 = new User(
                "khachhang",
                "123456",
                "Trần Thị B",
                "tranthib@gmail.com",
                "0987654321",
                "Số 45 Nguyễn Trãi, Thanh Xuân, Hà Nội",
                Role.ROLE_CUSTOMER
        );
        userRepository.save(customer2);

        // 2. Tạo Thể loại sách
        Category catVanHoc = categoryRepository.save(new Category("Văn học - Tiểu thuyết", "Tiểu thuyết kinh điển và văn học đương đại"));
        Category catKinhTe = categoryRepository.save(new Category("Kinh tế - Khởi nghiệp", "Sách kinh doanh, tài chính, đầu tư và khởi nghiệp"));
        Category catCNTT = categoryRepository.save(new Category("Công nghệ thông tin", "Sách lập trình, thuật toán, kiến trúc phần mềm"));
        Category catKyNang = categoryRepository.save(new Category("Kỹ năng sống", "Phát triển bản thân, tư duy tích cực và quản lý thời gian"));
        Category catManga = categoryRepository.save(new Category("Truyện tranh - Light Novel", "Truyện tranh hấp dẫn, hình vẽ sinh động"));

        // 3. Tạo Sách kèm các Biến thể (Variants) thực tế

        // Sách 1: Đắc Nhân Tâm (Kỹ năng sống)
        Book book1 = new Book(
                "Đắc Nhân Tâm (How to Win Friends and Influence People)",
                "Dale Carnegie",
                "NXB Tổng Hợp TP.HCM",
                2022,
                "978-604-58-9123-1",
                "Đắc Nhân Tâm là cuốn sách kinh điển về nghệ thuật đối nhân xử thế, giúp bạn thu phục lòng người, tạo dựng mối quan hệ chân thành và thành công vượt bậc trong công việc lẫn cuộc sống.",
                "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=600&q=80",
                catKyNang,
                true
        );
        book1.setSampleExcerpt("Phần 1: Nghệ thuật ứng xử căn bản — Nguyên tắc 1: Không chỉ trích, oán trách hay than phiền. Nguyên tắc 2: Thành thật khen ngợi và biết ơn người khác. Nguyên tắc 3: Khơi gợi ở người khác ý muốn thực hiện điều bạn đề xuất.");
        book1.addVariant(new BookVariant(book1, "Bìa mềm phổ thông", "DNT-SOFT-01", 76000.0, 88000.0, 50, true));
        book1.addVariant(new BookVariant(book1, "Bìa cứng kỷ niệm", "DNT-HARD-02", 128000.0, 150000.0, 30, false));
        book1.addVariant(new BookVariant(book1, "Bản đặc biệt bọc da kèm Bookmark kim loại", "DNT-VIP-03", 260000.0, 300000.0, 15, false));
        book1.setAiSummary(createDacNhanTamSummary(book1));
        bookRepository.save(book1);

        // Sách 2: Nhà Giả Kim (Văn học)
        Book book2 = new Book(
                "Nhà Giả Kim (The Alchemist)",
                "Paulo Coelho",
                "NXB Hội Nhà Văn",
                2021,
                "978-604-53-8472-5",
                "Tất cả những trải nghiệm trong chuyến phiêu lưu theo đuổi vận mệnh đã giúp Santiago thấu hiểu được ý nghĩa sâu xa nhất của hạnh phúc, sự hòa hợp giữa con người và vũ trụ.",
                "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=600&q=80",
                catVanHoc,
                true
        );
        book2.setSampleExcerpt("Khi bạn thực sự khao khát một điều gì đó, cả vũ trụ sẽ hợp lực giúp bạn đạt được điều đó. Chàng chăn cừu Santiago rời quê hương Andalusia để băng qua sa mạc Sahara tìm kiếm kho báu dưới chân Kim Tự Tháp Ai Cập.");
        book2.addVariant(new BookVariant(book2, "Bìa mềm", "NGK-SOFT-01", 69000.0, 79000.0, 45, true));
        book2.addVariant(new BookVariant(book2, "Bìa cứng cao cấp", "NGK-HARD-02", 125000.0, 145000.0, 20, false));
        book2.addVariant(new BookVariant(book2, "Bản minh họa màu đặc biệt kỷ niệm 25 năm", "NGK-VIP-03", 195000.0, 220000.0, 10, false));
        book2.setAiSummary(createNhaGiaKimSummary(book2));
        bookRepository.save(book2);

        // Sách 3: Clean Code - Nghệ Thuật Viết Mã Sạch (CNTT)
        Book book3 = new Book(
                "Clean Code - Nghệ Thuật Viết Mã Sạch",
                "Robert C. Martin (Uncle Bob)",
                "Prentice Hall",
                2020,
                "978-013-23-5088-4",
                "Cuốn cẩm nang gối đầu giường của mọi lập trình viên chuyên nghiệp. Hướng dẫn cách phân biệt code tốt và code xấu, các quy tắc đặt tên, viết hàm, xử lý lỗi và cấu trúc lớp trong Java.",
                "https://images.unsplash.com/photo-1532012164546-f432f2e37272?auto=format&fit=crop&w=600&q=80",
                catCNTT,
                true
        );
        book3.setSampleExcerpt("Chương 1: Mã sạch — Viết code để con người đọc hiểu quan trọng hơn nhiều so với chỉ để máy biên dịch. Chương 2: Đặt tên có ý nghĩa (Meaningful Names). Chương 3: Hàm (Functions) nên nhỏ gọn và chỉ làm duy nhất một việc.");
        book3.addVariant(new BookVariant(book3, "Bản dịch Tiếng Việt (Bìa mềm)", "CC-VN-SOFT", 215000.0, 250000.0, 35, true));
        book3.addVariant(new BookVariant(book3, "Bản Tiếng Anh gốc (Hardcover USA)", "CC-EN-HARD", 420000.0, 480000.0, 12, false));
        book3.setAiSummary(createCleanCodeSummary(book3));
        bookRepository.save(book3);

        // Sách 4: Tuổi Trẻ Đáng Giá Bao Nhiêu (Kỹ năng sống) - Để trống AI Summary để demo bấm nút tạo trực tiếp
        Book book4 = new Book(
                "Tuổi Trẻ Đáng Giá Bao Nhiêu?",
                "Rosie Nguyễn",
                "NXB Nhã Nam",
                2022,
                "978-604-98-3112-0",
                "Bạn hối tiếc vì không nắm bắt lấy một cơ hội nào đó, chẳng có ai phải mất ngủ. Bạn trải qua những ngày tháng nhạt nhẽo với công việc bạn căm ghét, người ta chẳng hề bận lòng. Cuốn sách thắp lên ngọn lửa hành động.",
                "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?auto=format&fit=crop&w=600&q=80",
                catKyNang,
                true
        );
        book4.setSampleExcerpt("Phần 1: Tôi đã học như thế nào? Phần 2: Học đi đôi với hành. Phần 3: Đi là một cách tự học. Đọc sách, rèn thói quen kỷ luật và dũng cảm bước ra khỏi vùng an toàn.");
        book4.addVariant(new BookVariant(book4, "Bìa mềm truyền thống", "TTDGBN-BM", 80000.0, 95000.0, 60, true));
        book4.addVariant(new BookVariant(book4, "Bản đặc biệt kèm Sổ tay kế hoạch cá nhân", "TTDGBN-BOX", 135000.0, 160000.0, 25, false));
        bookRepository.save(book4);

        // Sách 5: Tư Duy Nhanh Và Chậm (Kinh tế)
        Book book5 = new Book(
                "Tư Duy Nhanh Và Chậm (Thinking, Fast and Slow)",
                "Daniel Kahneman",
                "NXB Thế Giới",
                2021,
                "978-604-77-5120-1",
                "Kiệt tác đoạt giải Nobel kinh tế giải thích hai hệ thống thúc đẩy cách chúng ta suy nghĩ: Hệ thống 1 (nhanh, trực giác, cảm xúc) và Hệ thống 2 (chậm, cân nhắc, logic).",
                "https://images.unsplash.com/photo-1495640388908-05fa85288e61?auto=format&fit=crop&w=600&q=80",
                catKinhTe,
                false
        );
        book5.setSampleExcerpt("Phần I: Hai hệ thống tư duy. Phần II: Suy nghiệm và Thiên kiến (Heuristics and Biases). Phần III: Sự tự tin thái quá. Phần IV: Các lựa chọn và Lý thuyết Triển vọng.");
        book5.addVariant(new BookVariant(book5, "Bìa mềm", "TDNC-BM", 195000.0, 230000.0, 30, true));
        book5.addVariant(new BookVariant(book5, "Bìa cứng gáy vải cao cấp", "TDNC-BC", 290000.0, 340000.0, 15, false));
        bookRepository.save(book5);

        // Sách 6: Cây Cam Ngọt Của Tôi (Văn học)
        Book book6 = new Book(
                "Cây Cam Ngọt Của Tôi",
                "José Mauro de Vasconcelos",
                "NXB Hội Nhà Văn",
                2022,
                "978-604-53-9092-2",
                "Một tác phẩm kinh điển của văn học Brazil khiến hàng triệu độc giả trên toàn thế giới rơi nước mắt vì vẻ đẹp của tình thương và nỗi đau thơ ấu của cậu bé Zezé.",
                "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?auto=format&fit=crop&w=600&q=80",
                catVanHoc,
                false
        );
        book6.setSampleExcerpt("Tại sao người lớn lại hay quên rằng mình cũng từng là trẻ con? Dưới tán cây cam ngọt nhỏ sau vườn nhà mới, cậu bé Zezé tìm thấy một người bạn biết lắng nghe mọi ước mơ thơ trẻ.");
        book6.addVariant(new BookVariant(book6, "Bìa mềm", "CCNCT-BM", 92000.0, 108000.0, 40, true));
        book6.addVariant(new BookVariant(book6, "Bìa cứng kèm Bookmark & Postcard độc quyền", "CCNCT-VIP", 165000.0, 190000.0, 18, false));
        bookRepository.save(book6);

        // Sách 7: Thiết Kế Mẫu Trong Lập Trình (Design Patterns) (CNTT)
        Book book7 = new Book(
                "Design Patterns - 23 Mẫu Thiết Kế Hướng Đối Tượng",
                "Gang of Four (GoF)",
                "Addison-Wesley",
                2021,
                "978-020-16-3361-0",
                "Mọi lập trình viên Java muốn viết code hướng đối tượng bài bản và mở rộng tốt đều cần nắm vững 23 mẫu thiết kế: Singleton, Factory, Observer, Decorator, Strategy...",
                "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=600&q=80",
                catCNTT,
                false
        );
        book7.setSampleExcerpt("Nhóm Creational Patterns: Abstract Factory, Builder, Factory Method, Prototype, Singleton. Nhóm Structural Patterns: Adapter, Bridge, Composite, Decorator, Facade, Proxy. Nhóm Behavioral Patterns: Command, Iterator, Observer, State, Strategy, Template Method.");
        book7.addVariant(new BookVariant(book7, "Bìa mềm Tiếng Việt", "DP-VN-SOFT", 240000.0, 280000.0, 30, true));
        book7.addVariant(new BookVariant(book7, "Bản Bìa Cứng Chuyên Gia", "DP-HARD-VIP", 380000.0, 420000.0, 10, false));
        bookRepository.save(book7);

        // Sách 8: Cha Giàu Cha Nghèo (Kinh tế)
        Book book8 = new Book(
                "Cha Giàu Cha Nghèo (Rich Dad Poor Dad)",
                "Robert T. Kiyosaki",
                "NXB Trẻ",
                2022,
                "978-604-1-18239-0",
                "Cuốn sách quản lý tài chính cá nhân bán chạy số 1 thế giới mọi thời đại, chỉ ra sự khác biệt sâu sắc giữa tư duy người giàu và người nghèo về tài sản và tiêu sản.",
                "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=600&q=80",
                catKinhTe,
                true
        );
        book8.setSampleExcerpt("Bài học số 1: Người giàu không làm việc vì tiền, họ bắt tiền làm việc cho mình. Bài học số 2: Tại sao phải dạy kiến thức tài chính và phân biệt rạch ròi Tài sản với Tiêu sản.");
        book8.addVariant(new BookVariant(book8, "Bìa mềm Tập 1", "CGCN-T1", 85000.0, 105000.0, 50, true));
        book8.addVariant(new BookVariant(book8, "Trọn bộ 13 tập Hộp quà sang trọng (Boxset)", "CGCN-BOX", 890000.0, 1100000.0, 8, false));
        book8.setAiSummary(createChaGiauChaNgheoSummary(book8));
        bookRepository.save(book8);

        // 4. Tạo sẵn 4 đơn hàng mẫu trải đều các ngày để Biểu đồ Dashboard Admin hiển thị sinh động ngay khi deploy Cloud
        seedSampleOrdersForFreshDb(customer, customer2, book1, book2, book3, book8);

        System.out.println(">>> Nạp dữ liệu mẫu hoàn tất thành công! Sẵn sàng sử dụng.");
    }

    private void seedSampleOrdersForFreshDb(User c1, User c2, Book b1, Book b2, Book b3, Book b8) {
        if (orderRepository.count() > 0) {
            return;
        }
        Order o1 = new Order("ORD-DEMO-1001", c1.getFullName(), c1.getPhone(), c1.getAddress(), "Giao giờ hành chính", "COD", c1);
        o1.setOrderStatus(OrderStatus.COMPLETED);
        o1.setPaymentStatus(PaymentStatus.PAID);
        o1.setCreatedAt(LocalDateTime.now().minusDays(4));
        o1.addItem(new OrderItem(o1, b1, b1.getVariants().get(1), 1));
        o1.addItem(new OrderItem(o1, b2, b2.getVariants().get(0), 1));
        o1.calculateTotal();
        orderRepository.save(o1);

        Order o2 = new Order("ORD-DEMO-1002", c2.getFullName(), c2.getPhone(), c2.getAddress(), "Bọc xốp kỹ làm quà tặng", "VIETQR", c2);
        o2.setOrderStatus(OrderStatus.COMPLETED);
        o2.setPaymentStatus(PaymentStatus.PAID);
        o2.setCreatedAt(LocalDateTime.now().minusDays(3));
        o2.addItem(new OrderItem(o2, b3, b3.getVariants().get(1), 1));
        o2.calculateTotal();
        orderRepository.save(o2);

        Order o3 = new Order("ORD-DEMO-1003", c1.getFullName(), c1.getPhone(), c1.getAddress(), "Gọi trước khi giao", "VIETQR", c1);
        o3.setOrderStatus(OrderStatus.SHIPPING);
        o3.setPaymentStatus(PaymentStatus.PAID);
        o3.setVoucherCode("SACHMOI15");
        o3.setDiscountAmount(32250.0);
        o3.setCreatedAt(LocalDateTime.now().minusDays(1));
        o3.addItem(new OrderItem(o3, b3, b3.getVariants().get(0), 1));
        o3.calculateTotal();
        orderRepository.save(o3);

        Order o4 = new Order("ORD-DEMO-1004", c2.getFullName(), c2.getPhone(), c2.getAddress(), "", "COD", c2);
        o4.setOrderStatus(OrderStatus.PENDING);
        o4.setPaymentStatus(PaymentStatus.UNPAID);
        o4.setCreatedAt(LocalDateTime.now());
        o4.addItem(new OrderItem(o4, b8, b8.getVariants().get(0), 2));
        o4.calculateTotal();
        orderRepository.save(o4);
    }

    private void seedInitialVouchersIfEmpty() {
        if (voucherRepository.count() > 0) {
            return;
        }
        voucherRepository.save(new Voucher(
                "SACHMOI15",
                "Giảm 15% (tối đa 50.000đ) cho đơn sách từ 100.000đ",
                DiscountType.PERCENT,
                15.0,
                50000.0,
                100000.0,
                100,
                LocalDate.now().plusMonths(6)
        ));
        voucherRepository.save(new Voucher(
                "GIAM30K",
                "Giảm trực tiếp 30.000đ cho đơn hàng từ 150.000đ",
                DiscountType.FIXED,
                30000.0,
                null,
                150000.0,
                50,
                LocalDate.now().plusMonths(3)
        ));
        voucherRepository.save(new Voucher(
                "VIPBOOK60K",
                "Ưu đãi giảm 60.000đ dành cho đơn sách sưu tầm từ 350.000đ",
                DiscountType.FIXED,
                60000.0,
                null,
                350000.0,
                30,
                LocalDate.now().plusMonths(6)
        ));
    }

    private void seedInitialAiSummariesForExistingDb() {
        if (bookAiSummaryRepository.count() > 0) {
            return;
        }
        for (Book book : bookRepository.findAll()) {
            String lower = book.getTitle() != null ? book.getTitle().toLowerCase() : "";
            if (lower.contains("đắc nhân tâm")) {
                bookAiSummaryRepository.save(createDacNhanTamSummary(book));
            } else if (lower.contains("nhà giả kim")) {
                bookAiSummaryRepository.save(createNhaGiaKimSummary(book));
            } else if (lower.contains("clean code")) {
                bookAiSummaryRepository.save(createCleanCodeSummary(book));
            } else if (lower.contains("cha giàu cha nghèo")) {
                bookAiSummaryRepository.save(createChaGiauChaNgheoSummary(book));
            }
        }
    }

    private BookAiSummary createDacNhanTamSummary(Book book) {
        return new BookAiSummary(
                book,
                "\"Đắc Nhân Tâm\" của Dale Carnegie là tác phẩm nền tảng về nghệ thuật giao tiếp và thấu hiểu tâm lý con người. Thay vì các mẹo thao túng bề nổi, cuốn sách chỉ ra rằng sức ảnh hưởng bền vững nhất xuất phát từ lòng chân thành, sự trân trọng giá trị của đối phương và khả năng đặt mình vào góc nhìn của người khác.",
                String.join("\n",
                        "Kiềm chế sự chỉ trích, oán trách; thay vào đó hãy tìm hiểu động cơ ẩn sau hành động của người đối diện.",
                        "Ai cũng khao khát được công nhận — lời khen ngợi chân thành, đúng lúc có sức khích lệ mạnh hơn mọi mệnh lệnh.",
                        "Lắng nghe chăm chú và quan tâm thật sự đến câu chuyện của người khác là con đường ngắn nhất tạo dựng thiện cảm.",
                        "Khi cần góp ý sửa sai, hãy giữ thể diện cho đối phương và khơi gợi để họ tự hào hứng thay đổi."
                ),
                "Phù hợp với mọi độc giả từ sinh viên, nhân sự kinh doanh, quản lý đội nhóm đến bất kỳ ai muốn cải thiện kỹ năng giao tiếp, thuyết phục và xây dựng mối quan hệ bền chặt.",
                240,
                "Editorial-AI-Cache"
        );
    }

    private BookAiSummary createNhaGiaKimSummary(Book book) {
        return new BookAiSummary(
                book,
                "\"Nhà Giả Kim\" kể về hành trình vượt sa mạc Sahara của chàng chăn cừu trẻ Santiago đi tìm kho báu dưới chân Kim Tự Tháp Ai Cập. Mang màu sắc ngụ ngôn triết lý sâu lắng, Paulo Coelho gửi gắm thông điệp về lòng can đảm theo đuổi \"Vận mệnh cá nhân\" và nhận ra kho báu quý giá nhất nằm ngay trong sự trưởng thành trên suốt chặng đường đi.",
                String.join("\n",
                        "Nỗi sợ thất bại thường là rào cản duy nhất ngăn con người bước ra khỏi vùng an toàn để sống đúng ước mơ của mình.",
                        "Khi bạn toàn tâm theo đuổi mục tiêu chính đáng, những thử thách dọc đường chính là phép thử giúp bạn rèn giũa bản lĩnh.",
                        "Hạnh phúc không chỉ nằm ở đích đến cuối cùng mà hiện diện trong từng khoảnh khắc hiện tại khi ta biết quan sát và yêu thương.",
                        "Lắng nghe tiếng nói của trái tim giúp ta nhận diện được những dấu hiệu dẫn lối giữa ngã rẽ cuộc đời."
                ),
                "Dành cho người đọc đang đứng trước những lựa chọn lớn của tuổi trẻ, người yêu tiểu thuyết triết lý nhẹ nhàng và những ai cần tiếp thêm niềm tin vào hành trình riêng.",
                180,
                "Editorial-AI-Cache"
        );
    }

    private BookAiSummary createCleanCodeSummary(Book book) {
        return new BookAiSummary(
                book,
                "\"Clean Code\" của Robert C. Martin (Uncle Bob) đặt ra chuẩn mực nghề nghiệp cho kỹ sư phần mềm hiện đại: mã nguồn chạy đúng mới chỉ là một nửa công việc, mã nguồn sạch, dễ đọc và dễ mở rộng mới quyết định sự sống còn của dự án. Thông qua các ví dụ tái cấu trúc (refactoring) trực quan trong Java, cuốn sách rèn luyện tư duy kỷ luật trên từng dòng lệnh.",
                String.join("\n",
                        "Đặt tên biến, hàm và lớp mang tính tự giải thích rõ ràng để giảm tối đa nhu cầu viết chú thích rườm rà.",
                        "Mỗi hàm (Function) nên ngắn gọn, ở cùng một mức độ trừu tượng và chỉ đảm nhiệm duy nhất một trách nhiệm (Single Responsibility).",
                        "Tách bạch rõ ràng luồng xử lý ngoại lệ (Exception Handling) khỏi logic nghiệp vụ chính, tránh trả về null tùy tiện.",
                        "Áp dụng quy tắc Hướng đạo sinh (Boy Scout Rule): luôn để lại đoạn code sạch hơn lúc bạn mới mở file ra đọc."
                ),
                "Cực kỳ cần thiết cho sinh viên Công nghệ thông tin, lập trình viên Java / Spring Boot và các nhóm phát triển phần mềm muốn nâng cao chất lượng code khi làm việc nhóm.",
                390,
                "Editorial-AI-Cache"
        );
    }

    private BookAiSummary createChaGiauChaNgheoSummary(Book book) {
        return new BookAiSummary(
                book,
                "Thông qua câu chuyện đối lập giữa người cha ruột học vị cao nhưng chật vật tài chính và người cha nuôi kinh doanh nhạy bén, Robert Kiyosaki thách thức lối mòn \"học giỏi để tìm một công việc lương ổn định\". Tác phẩm đặt nền móng tư duy tài chính cá nhân thông qua việc tích lũy tài sản tạo dòng tiền thay vì chạy theo tiêu sản.",
                String.join("\n",
                        "Phân biệt cốt lõi: Tài sản là thứ bỏ tiền vào túi bạn, còn Tiêu sản là thứ rút tiền ra khỏi túi bạn mỗi tháng.",
                        "Thu nhập cao không đồng nghĩa với giàu có nếu chi tiêu tăng tương ứng theo vòng xoáy \"Rat Race\".",
                        "Người giàu ưu tiên xây dựng danh mục tài sản sinh lời trước, sau đó mới dùng dòng tiền thu được để chi trả cho nhu cầu hưởng thụ.",
                        "Giáo dục tài chính, hiểu biết về đầu tư, kế toán và thị trường là kỹ năng sinh tồn cần trang bị càng sớm càng tốt."
                ),
                "Phù hợp với sinh viên, người mới đi làm và độc giả muốn bắt đầu quản lý tài chính cá nhân, thoát khỏi áp lực tiền bạc và xây dựng tư duy đầu tư cơ bản.",
                220,
                "Editorial-AI-Cache"
        );
    }
}
