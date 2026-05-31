import static java.lang.Math.max;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@interface Java6Marker {
    String value() default "java6";
}

/**
 * Tests Java 6-compatible syntax: annotations, static imports, bounded
 * generics, wildcards, varargs, enhanced for loops, enum switch, assertions,
 * autoboxing/unboxing, generic methods, inner classes, and anonymous classes.
 */
@Java6Marker("class")
public class Java6Syntax<T extends Number & Comparable<T>> {

    private enum Mode {
        FAST,
        SAFE
    }

    private interface Worker {
        int run(int value);
    }

    @Java6Marker("field")
    private final List<? extends T> numbers = new ArrayList<T>();

    @Java6Marker("method")
    public int exercise(List<Integer> values, String... labels) {
        assert labels != null;

        List<Integer> copy = new ArrayList<Integer>();
        for (Integer value : values) {
            copy.add(value);
        }

        Worker worker = new Worker() {
            @Override
            public int run(int value) {
                return max(value * 2, 1);
            }
        };

        int total = 0;
        for (Integer value : copy) {
            int primitive = value;
            total += worker.run(primitive);
        }
        return choose(total + labels.length + numbers.size(), Mode.SAFE);
    }

    public <E extends Exception> int genericMethod(T value,
                                                   @Java6Marker E failure)
            throws E {
        if (value == null) {
            throw failure;
        }
        Holder holder = new Holder(value);
        return holder.get().intValue();
    }

    private int choose(int value, Mode mode) {
        switch (mode) {
            case FAST:
                return value + 1;
            case SAFE:
                return value - 1;
            default:
                throw new AssertionError(mode);
        }
    }

    private class Holder {

        private final T value;

        Holder(T value) {
            this.value = value;
        }

        T get() {
            return value;
        }
    }
}
