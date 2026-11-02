package VuHaiLam_V526055;

public class Diem2D {
    protected double x;
    protected double y;

    public Diem2D() {
        this.x = 0;
        this.y = 0;
    }

    public Diem2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double tinhKhoangCach(Diem2D d) {
        return Math.sqrt(Math.pow(this.x - d.x, 2) + Math.pow(this.y - d.y, 2));
    }

    public Diem2D diemDoiXung() {
        return new Diem2D(-this.x, -this.y);
    }

    public void tinhTien(double dx, double dy) {
        this.x += dx;
        this.y += dy;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}

