public class DeadLoop {

    public static void main(String args[]) {
        int x = 5;
        int y = recursion(x);
        int z = whileLoop(x);
    }

    public static int recursion(int x) {
        x += 1;
        recursion(x);
        return x;
    }

    public static int whileLoop(int x) {
        int y = 2;
        int z = 1;
        while (x > y) {
            z += 1;
        }
        return z;
    }

}