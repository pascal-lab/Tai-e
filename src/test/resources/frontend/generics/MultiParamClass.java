/**
 * Generic class with multiple type parameters.
 */
public class MultiParamClass<K, V> {
    private K key;
    private V value;

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public void setEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }
}
