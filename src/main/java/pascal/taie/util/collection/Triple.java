package pascal.taie.util.collection;

public record Triple<T1, T2, T3>(T1 first, T2 second, T3 third) {

    @Override
    public String toString() {
        return "<" + first + ", " + second + ", " + third + ">";
    }
}
