import java.util.List;

/**
 * Class with generic methods.
 */
public class GenericMethod {

    public <T> T identity(T value) {
        return value;
    }

    public <T extends Number> T boundedIdentity(T value) {
        return value;
    }

    public <K, V> V getFromPair(K key, V value) {
        return value;
    }

    public <T> List<T> toList(T value) {
        return null;
    }

    public void consumeList(List<String> list) {
    }

    public <E extends Exception> void throwsGeneric() throws E {
    }
}
