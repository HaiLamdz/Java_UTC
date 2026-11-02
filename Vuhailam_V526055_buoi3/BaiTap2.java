package VuHaiLam_V526055;

import java.util.ArrayList;
import java.util.List;

public class BaiTap2 {
    public static void main(String[] args) {
        List<Diem2D> dsDiem = new ArrayList<>();
        dsDiem.add(new Diem2D(1, 2));
        dsDiem.add(new Diem2D(4, 6));
        dsDiem.add(new Diem2D(-2, 3));
        dsDiem.add(new Diem3D(1, 2, 3));
        dsDiem.add(new Diem3D(4, 6, 3));
        dsDiem.add(new Diem3D(-1, -2, 5));

        List<Diem2D> list2D = new ArrayList<>();
        List<Diem3D> list3D = new ArrayList<>();

        for (Diem2D d : dsDiem) {
            if (d instanceof Diem3D) {
                list3D.add((Diem3D) d);
            } else {
                list2D.add(d);
            }
        }

        double tongKC2D = 0;
        for (int i = 0; i < list2D.size() - 1; i++) {
            tongKC2D += list2D.get(i).tinhKhoangCach(list2D.get(i + 1));
        }

        double tongKC3D = 0;
        for (int i = 0; i < list3D.size() - 1; i++) {
            tongKC3D += list3D.get(i).tinhKhoangCach(list3D.get(i + 1));
        }

        System.out.println("Tong khoang cach cac diem 2D: " + tongKC2D);
        System.out.println("Tong khoang cach cac diem 3D: " + tongKC3D);

        int soLuongBanDau = dsDiem.size();
        for (int i = 0; i < soLuongBanDau; i++) {
            dsDiem.add(dsDiem.get(i).diemDoiXung());
        }

        System.out.println("Danh sach cac diem sau khi them diem doi xung:");
        for (Diem2D d : dsDiem) {
            System.out.println(d);
        }
    }
}

