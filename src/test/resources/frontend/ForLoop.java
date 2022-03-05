class ForLoop {
    public int f() {
        int temp = 10;
        for (int i = 0; i < 100; i = i + 1) {
            temp = temp * i;
            if (temp < 0) {
                break;
            }
        }
        return temp;
    }
}