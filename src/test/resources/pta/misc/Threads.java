class Threads {

    public static void main(String[] args) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                foo();
            }
        });
        t.start();
        Thread t2 = Thread.currentThread();
    }

    static void foo() {
        Object o = new Object();
    }
}
