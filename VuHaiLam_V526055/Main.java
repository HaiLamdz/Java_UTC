package VuHaiLam_V526055;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("CHỌN BÀI TẬP MUỐN CHẠY: ");
        System.out.println("Bài 1");
        System.out.println("Bài 2");
        System.out.println("Bài 3");
        System.out.print("Bạn chọn: ");
        int chon = sc.nextInt();

        if (chon == 1) {
            Bai1.main(args);
        } else if (chon == 2) {
            Bai2.main(args);
        } else if (chon == 3) {
            Bai3.main(args);
        } else {
            System.out.println("Không có bài này!");
        }
    }
}
