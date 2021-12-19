/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.util.collection;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * A collection that maps keys to values, similar to {@link java.util.Map},
 * but in which each key may be associated with <i>multiple</i> values.
 * The values associated with the same key contain <i>no</i> duplicates.
 * You can visualize the contents of a multimap either as a map from keys
 * to <i>nonempty</i> collections of values:
 *
 *  <ul>
 *   <li>k1 → [v1]
 *   <li>k2 → [v2, v3, v4]
 *   <li>k3 → [v2, v5]
 *</ul>
 *
 * @param <K> type of the keys in this map
 * @param <V> type of the values in this map
 */
public interface MultiMap<K, V> {

    /**
     * @return true if this multimap contains at least one key-value pair
     * with the key {@code key} and the value {@code value}.
     */
    boolean contains(K key, V value);

    /**
     * @return {@code true} if this multimap contains at least one key-value pair
     * with the key {@code key}.
     */
    boolean containsKey(K key);

    /**
     * @return {@code true} if this multimap contains at least one key-value pair
     * with the value {@code value}. Note that this operation may be slow
     * compared to {@link #containsKey(Object)}.
     */
    boolean containsValue(V value);

    /**
     * @return an unmodifiable view of the values associated with {@code key}
     * in this multimap, if {@code key} is absent; otherwise, this returns
     * an empty set.
     */
    Set<V> get(@Nonnull K key);

    /**
     * Stores a key-value pair in this multimap.
     *
     * @return {@code true} if the multimap changed.
     */
    boolean put(@Nonnull K key, @Nonnull V value);

    /**
     * Stores a key-value pair in this multimap for each of {@code values},
     * all using the same key, {@code key}.
     *
     * @return {@code true} if the multimap changed
     */
    boolean putAll(@Nonnull K key, Collection<? extends V> values);

    /**
     * Stores all key-value pairs of {@code multimap} in this multimap.
     *
     * @return {@code true} if the multimap changed
     */
    boolean putAll(MultiMap<K, V> multiMap);

    /**
     * Removes a single key-value pair with the key {@code key} and the value
     * {@code value} from this multimap, if such exists.
     *
     * @return {@code true} if the multimap changed
     */
    boolean remove(K key, V value);

    /**
     * Removes all values associated with the key {@code key}.
     *
     * <p>Once this method returns, {@code key} will not be mapped to any values,
     * so it will not appear in {@link #keySet()}.
     *
     * @return {@code true} if the multimap changed.
     */
    boolean removeAll(K key);

    /**
     * Removes all key-value pairs for {@code key} and {@code values}.
     *
     * @return {@code true} if the multimap changed.
     */
    boolean removeAll(K key, Collection<? extends V> values);

    /**
     * @return an unmodifiable view of all <i>distinct</i> keys contained
     * in this multimap. Note that the key set contains a key if and only if
     * this multimap maps that key to at least one value.
     */
    Set<K> keySet();

    /**
     * Performs the given action for all key-value pairs contained in this multimap.
     */
    void forEach(@Nonnull BiConsumer<K, V> action);

    /**
     * Removes all key-value pairs from the multimap, leaving it empty.
     */
    void clear();

    /**
     * @return {@code true} if this multimap is empty.
     */
    boolean isEmpty();

    /**
     * @return the number of key-value pairs in this multimap.
     */
    int size();
}
