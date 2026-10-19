package VuHaiLam_V526055;

import java.util.Scanner;

public class Bai3 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Nhập chuỗi các số nguyên: ");
        String str = sc.nextLine();

        // Tách chuỗi theo khoảng trắng (tương tự explode(" ", $str) trong PHP)
        // "\\s+" giúp xử lý trường hợp có 1 hoặc nhiều dấu cách liền nhau
        String[] mangChuoi = str.trim().split("\\s+");

        int soPhanTu = mangChuoi.length; // giống count($arr) trong PHP
        int tong = 0;

        for (int i = 0; i < mangChuoi.length; i++) {
            // Chuyển chuỗi thành số nguyên (tương tự ép kiểu (int)$val trong PHP)
            tong += Integer.parseInt(mangChuoi[i]);
        }

        // In kết quả theo đúng định dạng mẫu
        System.out.println("Số phần tử: " + soPhanTu + "; Tổng: " + tong);
    }
}
