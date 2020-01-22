package sa.util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides convenient utility operations for collections.
 */
public class CollectionUtils {

    private CollectionUtils() {}

    public static <K, E> boolean addToMapSet(Map<K, Set<E>> map, K key, E element) {
        Set<E> set = map.get(key);
        if (set == null) {
            set = newSet();
            map.put(key, set);
        }
        return set.add(element);
    }

    public static <E> Set<E> newSet() {
        return new HashSet<>();
    }
}
