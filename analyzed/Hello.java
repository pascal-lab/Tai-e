public class Hello {
    
    void foo(int n, boolean bb) {
        n = 10;
        A a = new A();
        boolean b = true && bb || false;
        if (b) {
            int x = 100;
            return;
        } else {
            int y = 200;
        }
        a.bar();
    }
}

class A {
    void bar() {}
}
