package VuHaiLam_V526055;

import java.util.ArrayList;
import java.util.List;

public class BaiTap1 {
    public static void main(String[] args) {
        List<PhanSo> dsPhanSo = new ArrayList<>();
        dsPhanSo.add(new PhanSo(1, 2));
        dsPhanSo.add(new PhanSo(4, 2));
        dsPhanSo.add(new PhanSo(3, 4));
        dsPhanSo.add(new PhanSo(9, 3));
        dsPhanSo.add(new PhanSo(5, 6));

        System.out.println("Danh sach phan so: " + dsPhanSo);

        PhanSo max = dsPhanSo.get(0);
        PhanSo tong = new PhanSo(0, 1);
        int demKhongNguyen = 0;

        for (PhanSo ps : dsPhanSo) {
            if (ps.soSanh(max) > 0) {
                max = ps;
            }
            tong = tong.cong(ps);
            if (!ps.laSoNguyen()) {
                demKhongNguyen++;
            }
        }

        System.out.println("Phan so lon nhat: " + max);
        System.out.println("Tong cac phan so: " + tong);
        System.out.println("So phan so khong phai so nguyen: " + demKhongNguyen);
    }
}

