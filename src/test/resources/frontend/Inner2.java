interface P {

}

class Inner2 {
    public void f() {
        new P() {
            class H {
                public int k() {
                    return 20;
                }
            }
        };

        class H {

        }
    }
}