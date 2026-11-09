package com.bookstore.config;

import com.bookstore.model.*;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CategoryRepository;
import com.bookstore.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Khởi tạo dữ liệu mẫu ban đầu (Data Seeder).
 * Tự động chạy khi khởi động ứng dụng nếu cơ sở dữ liệu chưa có dữ liệu.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;

    public DataInitializer(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           BookRepository bookRepository) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.bookRepository = bookRepository;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return; // Đã có dữ liệu thì không tạo lại
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
        book1.addVariant(new BookVariant(book1, "Bìa mềm phổ thông", "DNT-SOFT-01", 76000.0, 88000.0, 50, true));
        book1.addVariant(new BookVariant(book1, "Bìa cứng kỷ niệm", "DNT-HARD-02", 128000.0, 150000.0, 30, false));
        book1.addVariant(new BookVariant(book1, "Bản đặc biệt bọc da kèm Bookmark kim loại", "DNT-VIP-03", 260000.0, 300000.0, 15, false));
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
        book2.addVariant(new BookVariant(book2, "Bìa mềm", "NGK-SOFT-01", 69000.0, 79000.0, 45, true));
        book2.addVariant(new BookVariant(book2, "Bìa cứng cao cấp", "NGK-HARD-02", 125000.0, 145000.0, 20, false));
        book2.addVariant(new BookVariant(book2, "Bản minh họa màu đặc biệt kỷ niệm 25 năm", "NGK-VIP-03", 195000.0, 220000.0, 10, false));
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
        book3.addVariant(new BookVariant(book3, "Bản dịch Tiếng Việt (Bìa mềm)", "CC-VN-SOFT", 215000.0, 250000.0, 35, true));
        book3.addVariant(new BookVariant(book3, "Bản Tiếng Anh gốc (Hardcover USA)", "CC-EN-HARD", 420000.0, 480000.0, 12, false));
        bookRepository.save(book3);

        // Sách 4: Tuổi Trẻ Đáng Giá Bao Nhiêu (Kỹ năng sống)
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
        book8.addVariant(new BookVariant(book8, "Bìa mềm Tập 1", "CGCN-T1", 85000.0, 105000.0, 50, true));
        book8.addVariant(new BookVariant(book8, "Trọn bộ 13 tập Hộp quà sang trọng (Boxset)", "CGCN-BOX", 890000.0, 1100000.0, 8, false));
        bookRepository.save(book8);

        System.out.println(">>> Nạp dữ liệu mẫu hoàn tất thành công! Sẵn sàng sử dụng.");
    }
}
