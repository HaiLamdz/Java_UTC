package VuHaiLam_V526055;

import java.util.Scanner;

public class Bai1 {
    public static void main(String[] args) {
        // Khởi tạo Scanner để nhập dữ liệu từ bàn phím
        Scanner sc = new Scanner(System.in);

        // Nhập số lượng phần tử n
        System.out.print("Nhập số phần tử n: ");
        int n = sc.nextInt();

        // Khai báo mảng gồm n phần tử
        int[] arr = new int[n];

        // Nhập từng phần tử vào mảng
        for (int i = 0; i < n; i++) {
            System.out.print("Nhập phần tử thứ " + (i + 1) + ": ");
            arr[i] = sc.nextInt();
        }

        // Tính tổng các phần tử
        int tong = 0;
        for (int i = 0; i < n; i++) {
            tong += arr[i];
        }

        // In kết quả
        System.out.println("Tổng các phần tử trong mảng: " + tong);
    }
}
