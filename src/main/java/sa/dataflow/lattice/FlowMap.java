package sa.dataflow.lattice;

import java.util.Set;

public interface FlowMap<K, V> {

    /**
     * Returns the value of given key.
     */
    V get(K key);

    /**
     * Associates given value to given key.
     * @return old value
     */
    V put(K key, V value);

    /**
     * Meets old value and given value, and associates the new value
     * with given key.
     * @return old value
     */
    V meetAndPut(K key, V value);

    /**
     * Returns if given key is present.
     */
    boolean containsKey(K key);

    /**
     * Returns set of all keys.
     */
    Set<K> keySet();
}
