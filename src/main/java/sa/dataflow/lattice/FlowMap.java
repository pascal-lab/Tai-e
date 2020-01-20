package sa.dataflow.lattice;

import java.util.Set;

/**
 * This class represents the data-flow information in product lattice
 * (specifically, in product of two lattices) which can be seen as a map.
 */
public interface FlowMap<K, V> {

    /**
     * Returns the value of given key.
     */
    V get(K key);

    /**
     * Associates given value to given key.
     * @return whether this operation changes the map.
     */
    boolean put(K key, V value);

    /**
     * Returns if given key is present.
     */
    boolean containsKey(K key);

    /**
     * Returns set of all keys.
     */
    Set<K> keySet();
}
