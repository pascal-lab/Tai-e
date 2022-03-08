class Try3 {
    public void f() {
        try {

        } catch (Exception e) {

        } finally {

        }
        return;
    }

    public void g(int x) {
        while (x > 10) {
            try {
                int i = 1 + 1;
                break;
            } catch (Exception e) {
                break;
            }
        }
        return;
    }
}