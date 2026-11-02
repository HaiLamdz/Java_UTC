package VuHaiLam_V526055;

public class PhanSo {
    private int tuSo;
    private int mauSo;

    public PhanSo() {
        this.tuSo = 0;
        this.mauSo = 1;
    }

    public PhanSo(int tuSo, int mauSo) {
        this.tuSo = tuSo;
        this.mauSo = (mauSo == 0) ? 1 : mauSo;
        rutGon();
    }

    public int getTuSo() {
        return tuSo;
    }

    public void setTuSo(int tuSo) {
        this.tuSo = tuSo;
    }

    public int getMauSo() {
        return mauSo;
    }

    public void setMauSo(int mauSo) {
        if (mauSo != 0) {
            this.mauSo = mauSo;
        }
    }

    private int timUCLN(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            int temp = a % b;
            a = b;
            b = temp;
        }
        return a == 0 ? 1 : a;
    }

    public void rutGon() {
        int ucln = timUCLN(this.tuSo, this.mauSo);
        this.tuSo /= ucln;
        this.mauSo /= ucln;
        if (this.mauSo < 0) {
            this.tuSo = -this.tuSo;
            this.mauSo = -this.mauSo;
        }
    }

    public PhanSo cong(PhanSo ps) {
        int tu = this.tuSo * ps.mauSo + ps.tuSo * this.mauSo;
        int mau = this.mauSo * ps.mauSo;
        return new PhanSo(tu, mau);
    }

    public PhanSo tru(PhanSo ps) {
        int tu = this.tuSo * ps.mauSo - ps.tuSo * this.mauSo;
        int mau = this.mauSo * ps.mauSo;
        return new PhanSo(tu, mau);
    }

    public PhanSo nhan(PhanSo ps) {
        int tu = this.tuSo * ps.tuSo;
        int mau = this.mauSo * ps.mauSo;
        return new PhanSo(tu, mau);
    }

    public PhanSo chia(PhanSo ps) {
        int tu = this.tuSo * ps.mauSo;
        int mau = this.mauSo * ps.tuSo;
        return new PhanSo(tu, mau);
    }

    public int soSanh(PhanSo ps) {
        long v1 = (long) this.tuSo * ps.mauSo;
        long v2 = (long) ps.tuSo * this.mauSo;
        return Long.compare(v1, v2);
    }

    public boolean laSoNguyen() {
        return this.tuSo % this.mauSo == 0;
    }

    @Override
    public String toString() {
        return tuSo + "/" + mauSo;
    }
}

