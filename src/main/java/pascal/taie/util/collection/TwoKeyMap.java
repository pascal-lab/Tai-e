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

import pascal.taie.util.TriConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * A collection that maps two-key pairs to values.
 * You can visualize the contents of a two-key map either as a map from keys
 * to second-level maps as values:
 *
 * <ul>
 *   <li>k1 → { k2 → v1, k3 → v2 }
 *   <li>k2 → { k4 → v1 }
 * </ul>
 *
 * ... or as a single "flattened" collection of key1-key2-value triples:
 *
 * <ul>
 *   <li>k1, k2 → v1
 *   <li>k1, k3 → v2
 *   <li>k2, k4 → v1
 * </ul>
 *
 * Note that both {@code null} keys and values are <i>not</i> permitted in this map.
 *
 * @param <K1> type of first keys in this map
 * @param <K2> type of second keys in this map
 * @param <V>  type of values in this map
 */
public interface TwoKeyMap<K1, K2, V> {

    /**
     * @return {@code true} if this two-key map contains the mapping with
     * {@code key1} as the first key and {@code key2} as the second key.
     */
    boolean containsKey(K1 key1, K2 key2);

    /**
     * @return {@code true} if this two-key map contains at least one mapping
     * with {@code key1} as the first key.
     */
    boolean containsKey(K1 key1);

    /**
     * @return {@code true} if this two-key map contains at least one mapping
     * with {@code value} as the value. Note that this operation may be slow
     * compared to {@link #containsKey(Object)}.
     */
    boolean containsValue(V value);

    /**
     * @return the value to which the specified keys is mapped, or {@code null}
     * if this map contains no mapping for the keys.
     */
    @Nullable V get(K1 key1, K2 key2);

    /**
     * @return an unmodifiable view of the second-level map for {@code key1},
     * or {@code null} if this map contains no mapping for the key.
     */
    @Nullable Map<K2, V> get(K1 key1);

    /**
     * Associates the specified value with the specified two-key pair in this map.
     * If the map previously contained a mapping for the keys, the old value
     * is replaced by the specified value.
     *
     * @return the previous value associated with {@code key1} and {@code key2},
     * or {@code null} if there was no mapping for key2.
     */
    @Nullable V put(@Nonnull K1 key1, @Nonnull K2 key2, @Nonnull V value);

    /**
     * Copies all the mappings from the specified map to second-level map
     * associated with {@code key1}.
     */
    void putAll(@Nonnull K1 key1, @Nonnull Map<K2, V> map);

    /**
     * Copies all the mappings from the specified two-key map to this map.
     */
    void putAll(@Nonnull TwoKeyMap<K1, K2, V> twoKeyMap);

    /**
     * Removes the mapping for a key-pair from this map if it is present.
     *
     * @return the value to which this map previously associated the key pair,
     * or {@code null} if the map contained no mapping for the key pair.
     */
    @Nullable V remove(K1 key1, K2 key2);

    /**
     * Removes all mappings with {@code key1} as the first key in this map.
     *
     * @return {@code true} if the two-key map changed.
     */
    boolean removeAll(K1 key1);

    /**
     * @return an unmodifiable view of all <i>distinct</i> two-keys contained
     * in this two-key map. Note that the result contains a two-key pair if
     * and only if this map maps that key pair to a non-{@code null} value.
     */
    Set<Pair<K1, K2>> twoKeySet();

    /**
     *  @return an unmodifiable view of first keys of all mappings contained
     * in this two-key map. Note that the result contains a key if and only if
     * this map contains at least one mapping with the key as the first key.
     */
    Set<K1> keySet();

    /**
     * @return an unmodifiable view collection containing the <i>value</i>
     * from each key1-key2-value triples contained in this map, without
     * collapsing duplicates (so {@code values().size() == size()}).
     */
    Collection<V> values();

    /**
     * @return an unmodifiable view of all key1-key2-value triples
     * contained in this two-key map, as {@link Entry} instances.
     */
    Set<Entry<K1, K2, V>> entrySet();

    /**
     * Performs the given action for all key1-key2-value triples
     * contained in this map.
     */
    default void forEach(@Nonnull TriConsumer<K1, K2, V> action) {
        Objects.requireNonNull(action);
        entrySet().forEach(entry -> action.accept(
                entry.getKey1(), entry.getKey2(), entry.getValue()));
    }

    /**
     * @return the value to which the specified key-pair is mapped,
     * or {@code defaultValue} if this map contains no mapping for the key-pair.
     */
    default V getOrDefault(K1 key1, K2 key2, V defaultValue) {
        V v;
        return (v = get(key1, key2)) != null ? v : defaultValue;
    }

    /**
     * If the specified key-pair is not already associated with a value,
     * attempts to compute its value using the given mapping function and
     * enters it into this map unless {@code null}.
     * <p>
     * If the mapping function returns {@code null}, no mapping is recorded.
     * If the mapping function itself throws an (unchecked) exception,
     * the exception is rethrown, and no mapping is recorded.
     *
     * @return the current (existing or computed) value associated with
     * the specified key, or {@code null} if the computed value is {@code null}.
     */
    default V computeIfAbsent(K1 key1, K2 key2,
                      @Nonnull BiFunction<K1, K2, V> mapper) {
        Objects.requireNonNull(mapper);
        V v;
        if ((v = get(key1, key2)) == null) {
            V newValue;
            if ((newValue = mapper.apply(key1, key2)) != null) {
                put(key1, key2, newValue);
                return newValue;
            }
        }
        return v;
    }

    /**
     * Removes all the mappings from this map.
     * The map will be empty after this call returns.
     */
    void clear();

    /**
     * @return {@code true} if this map contains no key1-key2-value mappings.
     */
    boolean isEmpty();

    /**
     * @return the number of key1-key2-value mappings in this map.
     */
    int size();

    /**
     * A map entry (key1-key2-value triple). The {@link #entrySet()} method
     * returns a collection-view of the map, whose elements are of this class.
     * The only way to obtain a reference to a map entry is from the iterator
     * of this collection-view. These {@link Entry>} objects are valid only
     * for the duration of the iteration; more formally, the behavior ofa map entry
     * is undefined if the backing map has been modified after the entry was
     * returned by the iterator.
     */
    interface Entry<K1, K2, V> {

        /**
         * @return the first key corresponding to this entry.
         */
        K1 getKey1();

        /**
         * @return the second key corresponding to this entry.
         */
        K2 getKey2();

        /**
         * Returns the value corresponding to this entry. If the mapping
         * has been removed from the backing map (by the iterator's
         * remove operation), the results of this call are undefined.
         *
         * @return the value corresponding to this entry
         */
        V getValue();

        /**
         * Compares the specified object with this entry for equality.
         *
         * @return {@code true} if the given object is also a map entry
         * and the two entries represent the same mapping.
         */
        boolean equals(Object o);

        /**
         * @return the hash code value for this map entry.
         */
        int hashCode();
    }
}
