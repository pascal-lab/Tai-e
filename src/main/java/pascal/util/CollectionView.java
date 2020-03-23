package pascal.util;

import java.util.Collection;
import java.util.function.Function;


public interface CollectionView<From, To> extends Collection<To> {

    public static <From, To> CollectionView<From, To> of(
            Collection<From> collection, Function<From, To> mapper) {
        return new ImmutableCollectionView<>(collection, mapper);
    }
}
