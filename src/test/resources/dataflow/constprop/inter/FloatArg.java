public class FloatArg {

    int foo(int i, float f) {
        return (int) (f * i);
    }

    public static void main(String[] args) {
        int x = 1;
        float f = 2.0f;
        new FloatArg().foo(x, f);
    }
}