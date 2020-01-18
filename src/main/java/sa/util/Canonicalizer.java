package sa.util;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * A canonicalizer based on object equality.
 * The canonicalied objects must be immutable.
 */
public class Canonicalizer<T> {

    private Map<T, T> map = new WeakHashMap<>();

    public T canonicalize(T object) {
        T canonical = map.get(object);
        if (canonical == null) {
            map.put(object, object);
            return object;
        } else {
            return canonical;
        }
    }
}
