import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface Java8TypeUse {
}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(Java8Tags.class)
@interface Java8Tag {
    String value();
}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@interface Java8Tags {
    Java8Tag[] value();
}

@FunctionalInterface
interface Java8Action<T> {

    T apply(T value);
}

/**
 * Helper interface for testing Java 8 default and static interface methods.
 */
interface Java8Named {

    default String prefix(String value) {
        return "v8:" + value;
    }

    static String suffix() {
        return ":done";
    }
}

/**
 * Tests Java 8 syntax: lambda expressions, method references, constructor
 * references, default/static interface methods, functional interfaces,
 * repeatable annotations, type-use annotations, and intersection casts.
 */
@Java8Tag("lambda")
@Java8Tag("method-reference")
public class Java8Syntax implements Java8Named {

    @Java8Tag("pipeline")
    public List<String> exercise(String... values) {
        Java8Action<String> clean =
                (@Java8TypeUse String value) -> prefix(value.trim());
        Function<String, String> normalize = clean::apply;
        Supplier<List<String>> listFactory = ArrayList<String>::new;
        Java8Named named = (Java8Named & java.io.Serializable) this;

        return Arrays.stream(values)
                .map(normalize.andThen(String::toUpperCase))
                .map(value -> value + Java8Named.suffix() + named.prefix(""))
                .collect(Collectors.toCollection(listFactory));
    }

    public Map<String, Integer> countLengths(
            List<@Java8TypeUse String> values) {
        return values.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        String::length,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    public Optional<String> findFirstNonEmpty(List<String> values) {
        Predicate<String> nonEmpty = ((Predicate<String>) String::isEmpty).negate();
        return values.stream()
                .filter(nonEmpty)
                .findFirst();
    }
}
