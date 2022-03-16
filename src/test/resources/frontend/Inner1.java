class Inner1 {
    int x;
    public int y;
    public int f() {
        return x;
    }

    public int g(final int h, final int p) {
        class I {
            public I(int q) {}
            public int f() {
                x = Inner1.this.f();
                y = g(p, h);
                if (y < 1) {
                    class Q {
                        public int p() { return 100; }
                    }
                } else {
                    class P {
                        public int g() { return 900; }
                    }
                }
                class a1 {
                    public int qq() { return 200; }
                }
                return 20;
            }
        }
        I i = new I(10);
        return 10;
    }
}