class Try7 {
    public int f() {
        try {
            return 20;
        } finally {
            int i = 1 + 1;
        }
    }

    public int g() {
        try {
            throw new Exception("12312");
        } catch (Exception e) {
            return 10;
        } finally {
            int i = 1 + 1;
        }
    }

    public int h(int x) {
        try {
            if (x > 10) {
                return 20;
            }
            int y = 1 + 2;
        } finally {
            int i = 1 + 1;
        }
        return 30;
    }

    public int k() {
        try {
            try {
                return 10;
            } finally {
                int j = 1 + 2;
            }
        } finally {
            int q = 123;
        }
        return 30;
    }
}