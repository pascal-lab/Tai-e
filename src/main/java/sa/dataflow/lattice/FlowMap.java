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

    /**
     * Copies the content from given map to this FlowMap.
     * Returns if the copy changes this FlowMap
     */
    default boolean copyFrom(FlowMap<K, V> map) {
        boolean changed = false;
        for (Entry<K, V> entry : map.entrySet()) {
            changed |= update(entry.getKey(), entry.getValue());
        }
        return changed;
    }
}
