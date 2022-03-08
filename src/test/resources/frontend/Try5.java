class Try5 {
    public static void f() {
        int x = 1;
        while (x < 100) {
            try {
                int k = 20;
            } catch (Exception e) {
                int j = 30;
                break;
            } finally {
                int q = 123;
            }
            x = x + 1;
        }
        return;
    }
}