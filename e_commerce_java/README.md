# 📚 BOOKSTORE — Hệ Thống Thương Mại Điện Tử & Quản Lý Nhà Sách Đa Biến Thể

> **Đề tài Bài tập lớn:** Xây dựng Website Thương mại điện tử bán sách kết hợp Hệ quản trị Cơ sở dữ liệu Oracle  
> **Kiến trúc:** Spring Boot 3.3.5 (MVC) &bull; Spring Data JPA (Hibernate 6) &bull; Thymeleaf SSR  
> **Cơ sở dữ liệu:** Oracle Database (23ai / 21c / 19c / XE) &bull; Hỗ trợ H2 In-Memory & MySQL  

---

## 1. Giới Thiệu Dự Án

**BookStore (Ấn Bản & Tri Thức)** là hệ thống web thương mại điện tử chuyên biệt cho mô hình nhà sách trực tuyến, giải quyết bài toán thực tế về **quản lý sách đa biến thể (Multi-Variant Books)** và **tách biệt độc lập giữa phân hệ Khách hàng (Client Storefront) với phân hệ Quản trị nội bộ (Admin Backoffice CMS)**.

### ✨ Các tính năng trọng tâm

1. **Mô hình Sách Đa Biến Thể (`Book` 1 &mdash; N `BookVariant`):**
   - Một đầu sách (*Đắc Nhân Tâm*, *Nhà Giả Kim*, *Clean Code*...) có thể phát hành nhiều ấn bản khác nhau: *Bìa mềm phổ thông*, *Bìa cứng cao cấp*, *Bản đặc biệt giới hạn / Boxset*.
   - Mỗi biến thể quản lý độc lập: **Mã SKU**, **Giá bán**, **Giá gốc (tự động tính % chiết khấu)**, **Số lượng tồn kho** và **Biến thể mặc định**.
   - Trang chi tiết tác phẩm cập nhật tức thời giá bán, mã SKU và trạng thái tồn kho ngay khi người đọc chuyển đổi giữa các phiên bản.

2. **Tách biệt hoàn toàn Cổng Khách Hàng (Client) và Cổng Quản Trị (Admin):**
   - **Định tuyến & Giao diện độc lập:** Giao diện cửa hàng (`/`) mang phong cách biên tập nhà sách (*Editorial Bookstore*), hoàn toàn không chứa bất kỳ liên kết nào dẫn sang trang quản trị. Hệ thống quản trị (`/admin`) sử dụng giao diện Backoffice riêng biệt.
   - **Phiên đăng nhập (Session) & Cổng xác thực tách rời:**
     - Khách hàng đăng nhập tại `/login` (lưu phiên `CLIENT_USER`).
     - Quản trị viên đăng nhập tại `/admin/login` (lưu phiên `ADMIN_USER`).
     - Việc đăng nhập/đăng xuất ở phân hệ này không ảnh hưởng đến phân hệ kia.

3. **Nghiệp vụ Mua sắm & Quản trị Kho sách đầy đủ:**
   - **Phân hệ Khách hàng:** Trang chủ với kệ sách trượt ngang, tìm kiếm từ khóa & lọc theo danh mục, chọn ấn bản sách, quản lý giỏ hàng, đặt hàng (COD / Chuyển khoản) và tra cứu lịch sử đơn hàng cá nhân.
   - **Phân hệ Quản trị (CMS):** Thống kê tổng quan (doanh thu, đơn hàng, đầu sách, tồn kho), CRUD danh mục, CRUD đầu sách cùng danh sách biến thể động, duyệt và cập nhật trạng thái đơn hàng (tự động kiểm tra và trừ tồn kho theo đúng biến thể).

---

## 2. Công Nghệ Sử Dụng

| Thành phần | Công nghệ / Thư viện |
| :--- | :--- |
| **Ngôn ngữ & Nền tảng** | Java 17+ (tương thích JDK 17, 21) |
| **Backend Framework** | Spring Boot 3.3.5 (`spring-boot-starter-web`, `spring-boot-starter-validation`) |
| **ORM & Truy vấn CSDL** | Spring Data JPA, Hibernate 6 |
| **Hệ quản trị CSDL** | **Oracle Database** (`ojdbc11`), hỗ trợ **H2 In-Memory** & **MySQL** |
| **Template Engine (SSR)** | Thymeleaf + HTML5 + Vanilla CSS3 (`Lora` & `Plus Jakarta Sans`) |
| **Công cụ Build** | Maven (tích hợp sẵn Maven Wrapper `mvnw` / `mvnw.cmd`) |

---

## 3. Hướng Dẫn Cài Đặt Cơ Sở Dữ Liệu (Database Setup)

Dự án hỗ trợ **2 chế độ cơ sở dữ liệu**. Mặc định dự án được cấu hình kết nối tới **Oracle Database**. Nếu máy bạn chưa cài Oracle, bạn có thể chuyển sang **H2 In-Memory** để chạy thử ngay trong 30 giây.

### Cách 1: Cài đặt với Oracle Database (Mặc định cho Bài tập lớn)

#### Bước 1: Tạo Tablespace và User trên Oracle
Mở **SQL*Plus** hoặc **Oracle SQL Developer**, đăng nhập bằng quyền `SYS AS SYSDBA` vào Pluggable Database (`FREEPDB1` hoặc `XEPDB1`) và chạy đoạn lệnh sau (có sẵn trong file [`oracle_scripts.sql`](./oracle_scripts.sql)):

```sql
ALTER SESSION SET CONTAINER = FREEPDB1;

-- 1. Tạo Tablespace riêng cho dự án (chỉnh lại đường dẫn thư mục chứa datafile phù hợp với máy của bạn)
CREATE TABLESPACE BOOKSTORE_TBS
DATAFILE 'BOOKSTORE_DATA01.DBF' SIZE 50M
AUTOEXTEND ON NEXT 10M MAXSIZE 500M;

-- 2. Tạo User mới gán vào Tablespace vừa tạo
CREATE USER bookstore IDENTIFIED BY "123456"
DEFAULT TABLESPACE BOOKSTORE_TBS
TEMPORARY TABLESPACE temp
QUOTA UNLIMITED ON BOOKSTORE_TBS;

-- 3. Cấp quyền cho User
GRANT CONNECT, RESOURCE, CREATE VIEW, CREATE PROCEDURE, CREATE SEQUENCE, CREATE TRIGGER TO bookstore;
GRANT UNLIMITED TABLESPACE TO bookstore;
```

#### Bước 2: Kiểm tra chuỗi kết nối trong `application.properties`
Mở file `src/main/resources/application.properties` và kiểm tra thông số kết nối Oracle của máy bạn:

```properties
spring.datasource.url=jdbc:oracle:thin:@localhost:1521/FREEPDB1
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
spring.datasource.username=bookstore
spring.datasource.password=123456
spring.jpa.database-platform=org.hibernate.dialect.OracleDialect
spring.jpa.hibernate.ddl-auto=update
```

> **Lưu ý:** Khi ứng dụng khởi chạy lần đầu, Hibernate (`ddl-auto=update`) sẽ tự động khởi tạo 6 bảng (`USERS`, `CATEGORIES`, `BOOKS`, `BOOK_VARIANTS`, `ORDERS`, `ORDER_ITEMS`) và class `DataInitializer` sẽ tự động nạp sẵn dữ liệu mẫu (tài khoản, 5 thể loại, 8 đầu sách kèm các biến thể).  
> Ngoài ra, toàn bộ kịch bản DDL, 10 câu truy vấn SQL báo cáo và các lệnh quản trị Instance/Tablespace/Datafile/Role/Profile/DataPump được đặt tại file [`oracle_scripts.sql`](./oracle_scripts.sql).

---

### Cách 2: Chạy nhanh bằng H2 Database (Không cần cài đặt Oracle/MySQL)

Nếu muốn chạy thử code ngay mà chưa cài Oracle trên máy:
1. Mở file `src/main/resources/application.properties`.
2. Thêm dấu `#` (comment) vào khối **LỰA CHỌN 1 (ORACLE DATABASE)**.
3. Bỏ dấu `#` (uncomment) ở khối **LỰA CHỌN 2 (H2 IN-MEMORY)**:
   ```properties
   spring.datasource.url=jdbc:h2:mem:bookstoredb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
   spring.datasource.driverClassName=org.h2.Driver
   spring.datasource.username=sa
   spring.datasource.password=
   spring.h2.console.enabled=true
   spring.h2.console.path=/h2-console
   spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
   ```

---

## 4. Hướng Dẫn Setup & Chạy Dự Án Local

### Yêu cầu môi trường
- **Java JDK 17** trở lên (`java -version`).
- **Git** (để clone dự án).
- *Không bắt buộc cài đặt Maven* vì dự án đã tích hợp sẵn **Maven Wrapper (`mvnw` / `mvnw.cmd`)**.

### Các bước chạy bằng Terminal / PowerShell

1. **Clone kho mã nguồn về máy và di chuyển vào thư mục dự án `e_commerce_java`:**
   ```bash
   git clone https://github.com/HaiLamdz/Java_UTC.git
   cd Java_UTC/e_commerce_java
   ```

2. **Biên dịch và khởi chạy ứng dụng Spring Boot:**
   - **Trên Windows (PowerShell / CMD):**
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```
   - **Trên macOS / Linux:**
     ```bash
     ./mvnw spring-boot:run
     ```

3. **Chạy bằng IDE (IntelliJ IDEA / Eclipse):**
   - Mở thư mục dự án trong **IntelliJ IDEA** hoặc **Eclipse**.
   - Đợi Maven tải các thư viện phụ thuộc (`dependencies`).
   - Mở file `src/main/java/com/bookstore/BookstoreApplication.java` và nhấn **Run**.

---

## 5. Đường Dẫn Truy Cập & Tài Khoản Thử Nghiệm

Sau khi ứng dụng khởi động thành công tại cổng `8080`, truy cập 2 phân hệ hoàn toàn độc lập:

| Phân hệ | Đường dẫn trang chính | Đường dẫn Đăng nhập | Tài khoản mẫu (`Username` / `Password`) |
| :--- | :--- | :--- | :--- |
| **1. Trang Khách Hàng (Client)** | `http://localhost:8080/` | `http://localhost:8080/login` | `nguyenvana` / `123456`<br>*(hoặc `khachhang` / `123456`)* |
| **2. Trang Quản Trị (Admin CMS)** | `http://localhost:8080/admin` | `http://localhost:8080/admin/login` | `admin` / `123456` |

---

## 6. Cấu Trúc Thư Mục Dự Án

```text
├── src/main/java/com/bookstore
│   ├── BookstoreApplication.java         # Điểm khởi chạy ứng dụng Spring Boot
│   ├── config/
│   │   ├── DataInitializer.java          # Tự động khởi tạo dữ liệu mẫu khi DB trống
│   │   └── GlobalModelAttributes.java    # Gắn thông tin giỏ hàng, danh mục & user vào View
│   ├── controller/
│   │   ├── HomeController.java           # Trang chủ & Tìm kiếm / Lọc sách
│   │   ├── BookController.java           # Chi tiết sách & API biến thể
│   │   ├── CartController.java           # Quản lý giỏ hàng trong Session
│   │   ├── CheckoutController.java       # Đặt hàng, thanh toán & lịch sử đơn khách hàng
│   │   ├── AuthController.java           # Đăng nhập / Đăng ký / Đăng xuất của Client (/login)
│   │   └── AdminController.java          # Đăng nhập (/admin/login) & Quản trị CMS (/admin/**)
│   ├── dto/                              # Các lớp Form & Data Transfer Object
│   ├── model/                            # Các thực thể JPA (User, Category, Book, BookVariant, Order, OrderItem)
│   ├── repository/                       # Tầng giao tiếp CSDL (Spring Data JPA Repositories)
│   └── service/                          # Tầng xử lý nghiệp vụ (BookService, CartService, OrderService, UserService)
├── src/main/resources
│   ├── application.properties            # Cấu hình kết nối CSDL (Oracle / H2 / MySQL) & Server
│   ├── static/css/
│   │   ├── style.css                     # Giao diện cửa hàng (Editorial Bookstore UI)
│   │   └── admin.css                     # Giao diện quản trị nội bộ (Admin Backoffice UI)
│   └── templates/
│       ├── index.html                    # Trang chủ cửa hàng
│       ├── layout/                       # Header, Footer, Alerts của Client
│       ├── books/                        # Trang danh sách & chi tiết sách
│       ├── cart/                         # Trang giỏ hàng
│       ├── checkout/                     # Trang thanh toán & lịch sử đơn hàng
│       ├── auth/                         # Trang đăng nhập & đăng ký của Khách hàng
│       └── admin/                        # Toàn bộ giao diện Quản trị (login, dashboard, books, orders, categories)
├── oracle_scripts.sql                    # Kịch bản SQL Oracle (Tablespace, DDL, 10 Queries, DBA Scripts)
└── pom.xml                               # Cấu hình Maven & Dependencies
```
