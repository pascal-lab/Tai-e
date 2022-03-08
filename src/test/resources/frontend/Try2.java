class Try2 {
    public int f(int x) {
        M:
        while(x > 0) {
            while (x > 3) {
                try {
                    if (x > 10) {
                        break M;
                    } else {
                        int q = 1 + 2;
                    }
                } catch (Exception e) {
                    int i = 1 + 1;
                } finally {
                    int k = 2;
                }
            }
        }
        return 10;
    }
}