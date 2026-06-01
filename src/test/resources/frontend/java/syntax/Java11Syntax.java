import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

interface Java11Formatter {

    default String decorate(String value) {
        return join(prefix(), value);
    }

    private String prefix() {
        return "v11";
    }

    private static String join(String prefix, String value) {
        return prefix + ":" + value;
    }
}

/**
 * Tests Java 9-11 syntax: private interface methods, effectively-final
 * try-with-resources variables, diamond with anonymous classes, local-variable
 * type inference, and var in lambda parameters.
 */
public class Java11Syntax implements Java11Formatter {

    public List<String> exercise(List<String> values) {
        var base = "v11";
        Function<String, String> mapper =
                (var value) -> decorate(base + ":" + value.strip().repeat(2));
        return List.copyOf(values).stream()
                .map(mapper)
                .collect(Collectors.toUnmodifiableList());
    }

    public String readFirstLine(String text) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(text));
        try (reader) {
            return reader.readLine();
        }
    }

    public Number anonymousDiamond(Number number) {
        Java11Box<Number> box = new Java11Box<>(number) {
            @Override
            Number value() {
                return super.value();
            }
        };
        return box.value();
    }

    private static class Java11Box<T> {

        private final T value;

        Java11Box(T value) {
            this.value = value;
        }

        T value() {
            return value;
        }
    }
}
