package sa.dataflow.lattice;

import java.util.Map;

/**
 * This class represents the data-flow information in product lattice
 * (specifically, in product of two lattices) which can be seen as a map.
 */
public interface FlowMap<K, V> extends Map<K, V> {

    /**
     * Updates the key-value mapping in this FlowMap.
     * Returns if the update changes this FlowMap.
     */
    boolean update(K key, V value);
}
