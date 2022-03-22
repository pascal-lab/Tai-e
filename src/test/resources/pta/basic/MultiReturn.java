public class MultiReturn {
    A foo(A a) {
        int x = a.getX();
        if (x % 2 == 0)
            return new A(x / 2);
        else
            return a;
    }

    public static void main(String[] args) {
        MultiReturn mr = new MultiReturn();
        A a = mr.foo(new A(2));
    }
}

class A {
    int x;

    A(int x) {
        this.x = x;
    }

    int getX() {
        return x;
    }
}