package VuHaiLam_V526055;

import java.util.Scanner;

public class Bai2 {

    // Viết hàm nhận vào 2 mảng và in ra mảng tổng
    public static void inMangTong(int[] a, int[] b) {
        // Tìm số phần tử của mảng dài hơn (giống count($a) > count($b) trong PHP)
        int max = a.length;
        if (b.length > max) {
            max = b.length;
        }

        // Tạo mảng kết quả có độ dài bằng mảng dài hơn
        int[] c = new int[max];

        for (int i = 0; i < max; i++) {
            int so1 = 0;
            int so2 = 0;

            // Nếu mảng a còn phần tử tại vị trí i thì lấy ra
            if (i < a.length) {
                so1 = a[i];
            }

            // Nếu mảng b còn phần tử tại vị trí i thì lấy ra
            if (i < b.length) {
                so2 = b[i];
            }

            c[i] = so1 + so2;
        }

        // In mảng tổng ra màn hình
        System.out.print("Mảng tổng là: ");
        for (int i = 0; i < max; i++) {
            System.out.print(c[i] + " ");
        }
        System.out.println();
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Nhập mảng 1
        System.out.print("Nhập số phần tử của mảng 1: ");
        int n1 = sc.nextInt();
        int[] a = new int[n1];
        for (int i = 0; i < n1; i++) {
            System.out.print("a[" + i + "] = ");
            a[i] = sc.nextInt();
        }

        // Nhập mảng 2
        System.out.print("Nhập số phần tử của mảng 2: ");
        int n2 = sc.nextInt();
        int[] b = new int[n2];
        for (int i = 0; i < n2; i++) {
            System.out.print("b[" + i + "] = ");
            b[i] = sc.nextInt();
        }

        // Gọi hàm để in ra mảng tổng
        inMangTong(a, b);
    }
}
