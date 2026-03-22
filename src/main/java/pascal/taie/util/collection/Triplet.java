package pascal.taie.util.collection;

import java.io.Serializable;

public record Triplet <T1, T2, T3>(T1 first, T2 second, T3 third)
        implements Serializable {
    @Override
    public String toString() {
        return "<" + first + ", " + second + ", " + third + ">";
    }
}
