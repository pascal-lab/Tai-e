class Try6 {
    public void f() {
        int x = 1;
        while (x < 1000) {
            try {
                int q = 10;
            } catch (Exception e) {
                int p = 20;
                throw e;
            } finally {
                break;
            }
        }
        return;
    }
}