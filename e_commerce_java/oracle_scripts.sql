-- ==============================================================
-- BÀI TẬP LỚN: HỆ QUẢN TRỊ CƠ SỞ DỮ LIỆU ORACLE
-- ĐỀ TÀI: HỆ THỐNG QUẢN LÝ VÀ BÁN SÁCH ĐA BIẾN THỂ (BOOKSTORE)
-- ==============================================================

-- ==============================================================
-- PHẦN I (MỤC 2): TẠO TABLESPACE MỚI VÀ USER MỚI (CHẠY DƯỚI QUYỀN SYSDBA)
-- Yêu cầu: "Dữ liệu được lưu trong 1 tablespace mới hoàn toàn không phải tablespace có sẵn"
-- ==============================================================

ALTER SESSION SET CONTAINER = FREEPDB1;

-- 1. Tạo Tablespace mới hoàn toàn
CREATE TABLESPACE BOOKSTORE_TBS
DATAFILE 'D:\ORACLE26AI_2\ORADATA\FREE\FREEPDB1\BOOKSTORE_DATA01.DBF' SIZE 50M
AUTOEXTEND ON NEXT 10M MAXSIZE 500M;

-- 2. Tạo User mới gán mặc định vào Tablespace mới
CREATE USER bookstore IDENTIFIED BY "123456"
DEFAULT TABLESPACE BOOKSTORE_TBS
TEMPORARY TABLESPACE temp
QUOTA UNLIMITED ON BOOKSTORE_TBS;

-- 3. Phân quyền cho User
GRANT CONNECT, RESOURCE, CREATE VIEW, CREATE PROCEDURE, CREATE SEQUENCE, CREATE TRIGGER TO bookstore;
GRANT UNLIMITED TABLESPACE TO bookstore;

CONNECT bookstore/"123456"@localhost:1521/FREEPDB1;

-- ==============================================================
-- PHẦN II (MỤC 2): TẠO CÁC BẢNG (DDL), KHÓA CHÍNH, KHÓA NGOẠI, RÀNG BUỘC
-- ==============================================================

-- Bảng 1: USERS (Người dùng: Khách hàng và Quản trị viên)
CREATE TABLE users (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR2(50) NOT NULL UNIQUE,
    password VARCHAR2(255) NOT NULL,
    full_name VARCHAR2(100) NOT NULL,
    email VARCHAR2(100) NOT NULL UNIQUE,
    phone VARCHAR2(20),
    address VARCHAR2(255),
    role VARCHAR2(20) DEFAULT 'ROLE_CUSTOMER' NOT NULL CHECK (role IN ('ROLE_CUSTOMER', 'ROLE_ADMIN')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng 2: CATEGORIES (Thể loại sách)
CREATE TABLE categories (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR2(100) NOT NULL UNIQUE,
    description VARCHAR2(255)
);

-- Bảng 3: BOOKS (Đầu sách)
CREATE TABLE books (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR2(200) NOT NULL,
    author VARCHAR2(100) NOT NULL,
    publisher VARCHAR2(100),
    publish_year NUMBER(4),
    isbn VARCHAR2(30),
    description CLOB,
    image_url VARCHAR2(500),
    category_id NUMBER,
    is_featured NUMBER(1) DEFAULT 0 CHECK (is_featured IN (0, 1)),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_books_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

-- Bảng 4: BOOK_VARIANTS (Biến thể phiên bản sách: Bìa mềm, bìa cứng, VIP...)
CREATE TABLE book_variants (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    book_id NUMBER NOT NULL,
    variant_name VARCHAR2(100) NOT NULL,
    sku VARCHAR2(50),
    price NUMBER(12, 2) NOT NULL CHECK (price >= 0),
    original_price NUMBER(12, 2) CHECK (original_price >= 0),
    stock_quantity NUMBER(8) DEFAULT 0 NOT NULL CHECK (stock_quantity >= 0),
    is_default NUMBER(1) DEFAULT 0 CHECK (is_default IN (0, 1)),
    CONSTRAINT fk_variants_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
);

-- Bảng 5: ORDERS (Đơn hàng mua sách)
CREATE TABLE orders (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_code VARCHAR2(50) NOT NULL UNIQUE,
    customer_name VARCHAR2(100) NOT NULL,
    customer_phone VARCHAR2(20) NOT NULL,
    shipping_address VARCHAR2(255) NOT NULL,
    note VARCHAR2(500),
    payment_method VARCHAR2(50) DEFAULT 'COD' NOT NULL,
    order_status VARCHAR2(30) DEFAULT 'PENDING' NOT NULL CHECK (order_status IN ('PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED', 'CANCELLED')),
    total_amount NUMBER(14, 2) DEFAULT 0 NOT NULL,
    user_id NUMBER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Bảng 6: ORDER_ITEMS (Chi tiết các cuốn sách trong đơn)
CREATE TABLE order_items (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id NUMBER NOT NULL,
    book_id NUMBER,
    variant_id NUMBER,
    book_title VARCHAR2(200) NOT NULL,
    variant_name VARCHAR2(100) NOT NULL,
    book_image VARCHAR2(500),
    price NUMBER(12, 2) NOT NULL,
    quantity NUMBER(6) NOT NULL CHECK (quantity > 0),
    subtotal NUMBER(14, 2) NOT NULL,
    CONSTRAINT fk_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_items_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE SET NULL,
    CONSTRAINT fk_items_variant FOREIGN KEY (variant_id) REFERENCES book_variants(id) ON DELETE SET NULL
);

-- ==============================================================
-- PHẦN III (MỤC 3): TẬP HỢP 10 CÂU TRUY VẤN SQL BẮT BUỘC
-- ==============================================================

-- Câu 1 (Truy vấn cơ bản): Lấy danh sách các đầu sách xuất bản từ năm 2020 trở lại đây kèm thể loại
SELECT b.id, b.title, b.author, b.publish_year, c.name AS category_name
FROM books b
JOIN categories c ON b.category_id = c.id
WHERE b.publish_year >= 2020
ORDER BY b.publish_year DESC;

-- Câu 2 (Truy vấn cơ bản có sắp xếp): Danh sách tất cả biến thể sách có giá bán trên 100.000 đ
SELECT v.sku, v.variant_name, b.title AS book_title, v.price, v.stock_quantity
FROM book_variants v
JOIN books b ON v.book_id = b.id
WHERE v.price >= 100000
ORDER BY v.price DESC;

-- Câu 3 (Gom nhóm & Thống kê): Thống kê số lượng đầu sách và giá trung bình theo từng thể loại
SELECT c.name AS category_name, 
       COUNT(DISTINCT b.id) AS total_books, 
       ROUND(AVG(v.price), 0) AS avg_variant_price
FROM categories c
LEFT JOIN books b ON c.id = b.category_id
LEFT JOIN book_variants v ON b.id = v.book_id
GROUP BY c.id, c.name
ORDER BY total_books DESC;

-- Câu 4 (Gom nhóm có HAVING): Tìm những đầu sách có từ 3 phiên bản biến thể trở lên
SELECT b.id, b.title, COUNT(v.id) AS variant_count, MIN(v.price) AS min_price, MAX(v.price) AS max_price
FROM books b
JOIN book_variants v ON b.id = v.book_id
GROUP BY b.id, b.title
HAVING COUNT(v.id) >= 3;

-- Câu 5 (Truy vấn lồng nhau - Subquery): Tìm cuốn sách có biến thể đắt nhất trong toàn bộ cửa hàng
SELECT b.title, v.variant_name, v.price
FROM books b
JOIN book_variants v ON b.id = v.book_id
WHERE v.price = (SELECT MAX(price) FROM book_variants);

-- Câu 6 (Truy vấn lồng nhau với IN): Tìm các khách hàng đã từng đặt ít nhất 1 đơn hàng trạng thái 'COMPLETED'
SELECT id, username, full_name, email, phone
FROM users
WHERE id IN (
    SELECT DISTINCT user_id 
    FROM orders 
    WHERE order_status = 'COMPLETED' AND user_id IS NOT NULL
);

-- Câu 7 (Hàm thống kê tổng hợp): Báo cáo tổng doanh thu và số lượng đơn hàng theo từng trạng thái
SELECT order_status,
       COUNT(id) AS total_orders,
       NVL(SUM(total_amount), 0) AS total_revenue,
       ROUND(NVL(AVG(total_amount), 0), 0) AS avg_order_value
FROM orders
GROUP BY order_status;

-- Câu 8 (Truy vấn nhiều bảng): Chi tiết các mặt hàng trong đơn hàng kèm tên người nhận
SELECT o.order_code, o.customer_name, o.customer_phone, 
       i.book_title, i.variant_name, i.price, i.quantity, i.subtotal
FROM orders o
JOIN order_items i ON o.id = i.order_id
ORDER BY o.id DESC, i.id ASC;

-- Câu 9 (Truy vấn lồng NOT EXISTS): Tìm những cuốn sách chưa từng có khách hàng nào đặt mua
SELECT b.id, b.title, b.author
FROM books b
WHERE NOT EXISTS (
    SELECT 1 FROM order_items oi WHERE oi.book_id = b.id
);

-- Câu 10 (Thống kê xếp hạng RANK): Xếp hạng các phiên bản sách theo số lượng tồn kho trong mỗi đầu sách
SELECT b.title, v.variant_name, v.stock_quantity,
       DENSE_RANK() OVER (PARTITION BY b.id ORDER BY v.stock_quantity DESC) AS stock_rank
FROM books b
JOIN book_variants v ON b.id = v.book_id;

-- ==============================================================
-- PHẦN IV (MỤC 4): QUẢN TRỊ CSDL ORACLE (5 ĐIỂM)
-- ==============================================================

-- --------------------------------------------------------------
-- 4.1 QUẢN LÝ INSTANCE (Thực hiện bằng sqlplus / as sysdba)
-- --------------------------------------------------------------
/*
-- 1. Khởi động Database qua các chế độ:
STARTUP NOMOUNT;  -- Chỉ khởi tạo Instance (SGA, Background processes), chưa đọc Controlfile
ALTER DATABASE MOUNT; -- Nạp Controlfile, nhận diện Datafiles
ALTER DATABASE OPEN;  -- Mở database cho người dùng truy vấn

-- 2. Đặt Database sang chế độ chỉ đọc (Read-Only):
ALTER DATABASE OPEN READ ONLY;

-- 3. Mở Database sang chế độ bình thường (Read-Write):
ALTER DATABASE OPEN READ WRITE;

-- 4. Chế độ hạn chế người dùng (Restricted Session - chỉ DBA được vào):
ALTER SYSTEM ENABLE RESTRICTED SESSION;
-- Tắt chế độ hạn chế:
ALTER SYSTEM DISABLE RESTRICTED SESSION;

-- 5. Các chế độ Shutdown Database:
SHUTDOWN NORMAL;        -- Chờ tất cả user ngắt kết nối
SHUTDOWN TRANSACTIONAL; -- Chờ các transaction hiện tại commit/rollback
SHUTDOWN IMMEDIATE;     -- Ngắt kết nối ngay và rollback các transaction dở dang (Khuyên dùng)
SHUTDOWN ABORT;         -- Tắt cưỡng bức ngay lập tức (như rút điện)
*/

-- --------------------------------------------------------------
-- 4.2 QUẢN LÝ TABLESPACE & DATAFILE (Thực hiện bằng SYSDBA)
-- --------------------------------------------------------------

-- 1. Xem danh sách Tablespace và trạng thái:
SELECT tablespace_name, status, contents, extent_management 
FROM dba_tablespaces;

-- 2. Xem thông tin chi tiết các Datafile và dung lượng:
SELECT tablespace_name, file_name, bytes/1024/1024 AS size_mb, autoextensible, maxbytes/1024/1024 AS max_mb
FROM dba_data_files;

-- 3. Thêm một Datafile mới vào Tablespace BOOKSTORE_TBS:
ALTER TABLESPACE BOOKSTORE_TBS 
ADD DATAFILE 'D:\ORACLE26AI_2\ORADATA\FREE\FREEPDB1\BOOKSTORE_DATA02.DBF' SIZE 20M AUTOEXTEND ON;

-- 4. Bật/Tắt tự động mở rộng (Autoextend) của một Datafile:
ALTER DATABASE DATAFILE 'D:\ORACLE26AI_2\ORADATA\FREE\FREEPDB1\BOOKSTORE_DATA02.DBF' AUTOEXTEND OFF;
ALTER DATABASE DATAFILE 'D:\ORACLE26AI_2\ORADATA\FREE\FREEPDB1\BOOKSTORE_DATA02.DBF' AUTOEXTEND ON NEXT 5M MAXSIZE 200M;

-- 5. Thay đổi kích thước (Resize) Datafile:
ALTER DATABASE DATAFILE 'D:\ORACLE26AI_2\ORADATA\FREE\FREEPDB1\BOOKSTORE_DATA02.DBF' RESIZE 30M;

-- 6. Di chuyển vị trí Datafile (Online Datafile Move - tính năng nâng cao từ Oracle 12c/23ai):
-- Di chuyển file sang thư mục mới mà không cần tắt CSDL:
-- ALTER DATABASE MOVE DATAFILE 'D:\ORACLE26AI_2\ORADATA\FREE\FREEPDB1\BOOKSTORE_DATA02.DBF' 
-- TO 'D:\ORACLE26AI_2\ORADATA\FREE\FREEPDB1\BACKUP_BOOKSTORE_DATA02.DBF';

-- Cách truyền thống (Offline):
-- ALTER TABLESPACE BOOKSTORE_TBS OFFLINE;
-- (Di chuyển file vật lý trong Windows Explorer)
-- ALTER TABLESPACE BOOKSTORE_TBS RENAME DATAFILE 'D:\...\old.dbf' TO 'D:\...\new.dbf';
-- ALTER TABLESPACE BOOKSTORE_TBS ONLINE;

-- 7. Khôi phục Datafile bị mất (Re-create Datafile):
-- Khi datafile bị vô tình xóa hoặc hỏng file:
-- ALTER DATABASE DATAFILE 'D:\ORACLE26AI_2\ORADATA\FREE\FREEPDB1\BOOKSTORE_DATA02.DBF' OFFLINE;
-- ALTER DATABASE CREATE DATAFILE 'D:\ORACLE26AI_2\ORADATA\FREE\FREEPDB1\BOOKSTORE_DATA02.DBF' AS 'D:\ORACLE26AI_2\ORADATA\FREE\FREEPDB1\BOOKSTORE_DATA02.DBF';
-- RECOVER DATAFILE 'D:\ORACLE26AI_2\ORADATA\FREE\FREEPDB1\BOOKSTORE_DATA02.DBF';
-- ALTER DATABASE DATAFILE 'D:\ORACLE26AI_2\ORADATA\FREE\FREEPDB1\BOOKSTORE_DATA02.DBF' ONLINE;

-- 8. Xóa Datafile phụ:
ALTER TABLESPACE BOOKSTORE_TBS DROP DATAFILE 'D:\ORACLE26AI_2\ORADATA\FREE\FREEPDB1\BOOKSTORE_DATA02.DBF';

-- --------------------------------------------------------------
-- 4.3 QUẢN TRỊ NGƯỜI DÙNG, PHÂN QUYỀN, PROFILE (CHỨC DANH)
-- --------------------------------------------------------------

-- 1. Tạo Role (Nhóm quyền) cho nhân viên bán sách:
CREATE ROLE r_sales_staff;
GRANT CREATE SESSION TO r_sales_staff;
GRANT SELECT, INSERT, UPDATE ON bookstore.orders TO r_sales_staff;
GRANT SELECT, INSERT, UPDATE ON bookstore.order_items TO r_sales_staff;
GRANT SELECT ON bookstore.books TO r_sales_staff;
GRANT SELECT ON bookstore.book_variants TO r_sales_staff;

-- 2. Tạo Profile giới hạn bảo mật (Failed login, Password expiry):
CREATE PROFILE pf_bookstore_security LIMIT
    FAILED_LOGIN_ATTEMPTS 3         -- Khóa tài khoản sau 3 lần gõ sai mật khẩu
    PASSWORD_LIFE_TIME 60           -- Đổi mật khẩu mỗi 60 ngày
    PASSWORD_LOCK_TIME 1;           -- Khóa tài khoản trong 1 ngày

-- 3. Gán Role và Profile cho một user nhân viên mới:
CREATE USER nv_banhang IDENTIFIED BY "Nhanvien@123"
DEFAULT TABLESPACE BOOKSTORE_TBS
PROFILE pf_bookstore_security;

GRANT r_sales_staff TO nv_banhang;

-- --------------------------------------------------------------
-- 4.4 MINH HỌA EXPORT VÀ IMPORT SCHEMA (ORACLE DATA PUMP)
-- --------------------------------------------------------------
/*
Thực hiện trên Terminal (CMD / PowerShell của Windows):

1. Tạo thư mục Directory trong Oracle:
CREATE OR REPLACE DIRECTORY dpump_dir AS 'D:\backup_oracle';
GRANT READ, WRITE ON DIRECTORY dpump_dir TO bookstore;

2. Lệnh Export toàn bộ Schema bookstore ra file dump (expdp):
expdp bookstore/"123456"@localhost:1521/FREEPDB1 SCHEMAS=bookstore DIRECTORY=dpump_dir DUMPFILE=bookstore_backup.dmp LOGFILE=exp_bookstore.log

3. Lệnh Import Schema từ file dump vào Oracle (impdp):
impdp bookstore/"123456"@localhost:1521/FREEPDB1 SCHEMAS=bookstore DIRECTORY=dpump_dir DUMPFILE=bookstore_backup.dmp LOGFILE=imp_bookstore.log TABLE_EXISTS_ACTION=REPLACE
*/
