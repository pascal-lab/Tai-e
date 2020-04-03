/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.dataflow.lattice;

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
