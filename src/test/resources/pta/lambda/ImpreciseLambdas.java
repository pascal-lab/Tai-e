import java.util.function.Consumer;

class ImpreciseLambdas {
    public static void main(String[] args) {
        Object[] lambdas = new Object[10];
        int i = 0;
        Consumer<Object> f1 = x -> print(x);
        lambdas[i++] = f1;
        Consumer<Integer> f2 = n -> useInt(n);
        lambdas[i++] = f2;
        ((Consumer<Object>) lambdas[0]).accept(new Object());
    }

    private static void print(Object o) { // pt(o) -> { new Object() }
    }

    private static void useInt(Integer i) { // pt(i) should be empty
    }
}
