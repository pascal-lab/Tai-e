import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NullAnnotation {
    @Nullable
    NullAnnotation getNullable() {
        return null;
    }

    @CheckForNull
    NullAnnotation getCheckForNull() {
        return new NullAnnotation();
    }

    @Nonnull
    NullAnnotation getNonnull(@CheckForNull Object o) {
        return new NullAnnotation();
    }

    void nonNullParameter(@Nonnull Object nonNull) {

    }

    void nullableParameter(@Nullable Object nullable) {

    }
}
