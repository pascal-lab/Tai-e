class If2 {
    public void f() {
        if (false) {
            if (false) {
                return;
            }
        }
        int i = 1 + 1;
    }

    public void g() {
        while (true) {
            if (false) {
                break;
            }
        }
        int i = 1 + 1;
    }
}