import java.util.function.Consumer;

class ImpreciseLambdas {
    public static void main(String[] args) {
        Object[] lambdas = new Object[10];
        int i = 0;
        Consumer<String> f1 = x -> print(x);
        lambdas[i++] = f1;
        Consumer<Integer> f2 = n -> useInt(n);
        lambdas[i++] = f2;
        ((Consumer<String>) lambdas[0]).accept("xxx");
    }

    private static void print(String s) { // pt(s) -> { "xxx" }
    }

    private static void useInt(Integer i) { // pt(i) should be empty
    }
}
