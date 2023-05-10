package pascal.taie.util.collection;

public record Quadruple<T1, T2, T3, T4>(T1 first, T2 second, T3 third, T4 fourth) {
    @Override
    public String toString() {
        return "<" + first + ", " + second + ", " + third + ", " + fourth + ">";
    }
}
