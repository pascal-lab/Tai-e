public class TestConstantPropagation {
    
    void foo(int n, boolean bb) {
        n = 10;
        A a = new A();
        boolean b = true && bb || false;
        int y, xxx;
        if (b) {
            int x = 100;
            return;
        } else {
            y = 200;
        }
        int z = n + y;
        a.bar(z);
    }
}

class A {
    void bar(int p) {
        int i10 = 10;
        int x = i10 + 111;
        int y = x * 5;
        int z = y - 12;
        boolean b = z > 1000;
        return;
    }
}
