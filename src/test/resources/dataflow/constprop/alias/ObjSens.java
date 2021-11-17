class ObjSens {

    public static void main(String[] args) {
        X x1 = new X();
        Y y1 = new Y();
        x1.setY(y1);

        X x2 = new X();
        Y y2 = new Y();
        x2.setY(y2);

        Y yy1 = x1.getY();
        yy1.f = 147;
        Y yy2 = x2.getY();
        yy2.f = 258;
        int n = yy1.f;
    }
}

class X {

    private Y y;

    void setY(Y y) {
        this.y = y;
    }

    Y getY() {
        return y;
    }
}

class Y {
    int f;
}
