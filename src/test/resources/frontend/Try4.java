class Try4 {
    public void f(int x) {
        while (x > 0) {
            try {
                try {
                    if (x > 100) {
                        break;
                    } else if (x > 12312312) {
                        int i = 312312;
                    } else {
                        throw new IllegalAccessException();
                    }
                } catch (Exception e) {
                    System.out.println(2313);
                    throw e;
                } finally {
                    int j = 4232;
                }
            } catch (IllegalAccessException e) {
               int qwe = 2132;
            } finally {
                int q = 123123;
            }
        }
        return;
    }
}