public class Left {
    public<T> T left(T a, T b) {
        return a;
    }

    public Object leftObject(Object a, Object b) {
        return a;
    }

    public B leftB(B a, B b) {
        return b;
    }

    public int[][] leftArray(int[][] a, int b) {
        return a;
    }
}

class B {}