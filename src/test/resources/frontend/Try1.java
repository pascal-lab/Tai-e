class Try1 {
    public static void f() throws Exception {
        int i = 0;
        while (i > 0) {
            try {
                System.out.println("asdfasfx");
                if ( i> 1000 ) {
                    throw new IllegalAccessException("adsa");
                } else {
                    continue;
                }
            } catch (IllegalAccessException e1) {
                System.out.println(10);
            } catch (Exception e2) {
                System.out.println(100);
            } finally {
                System.out.println("dsfx");
            }
        }
        return;
    }
}