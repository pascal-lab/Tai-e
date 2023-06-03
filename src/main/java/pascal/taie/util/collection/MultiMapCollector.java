/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.util.collection;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class MultiMapCollector<T, K, V, R> implements Collector<T, MultiMap<K, V>, R> {

    private static final Set<Characteristics> CH = Collections.unmodifiableSet(
            EnumSet.of(Characteristics.IDENTITY_FINISH));

    private final Supplier<MultiMap<K, V>> supplier;

    private final Function<? super T, ? extends K> keyMapper;

    private final Function<? super T, ? extends V> valueMapper;

    private MultiMapCollector(Supplier<MultiMap<K, V>> supplier,
                              Function<? super T, ? extends K> keyMapper,
                              Function<? super T, ? extends V> valueMapper) {
        this.supplier = supplier;
        this.keyMapper = keyMapper;
        this.valueMapper = valueMapper;
    }

    @Override
    public Supplier<MultiMap<K, V>> supplier() {
        return supplier;
    }

    @Override
    public BiConsumer<MultiMap<K, V>, T> accumulator() {
        return this::accumulate;
    }

    @Override
    public BinaryOperator<MultiMap<K, V>> combiner() {
        return MultiMapCollector::combine;
    }

    @Override
    public Function<MultiMap<K, V>, R> finisher() {
        return castingIdentity();
    }

    @Override
    public Set<Characteristics> characteristics() {
        return CH;
    }

    private void accumulate(MultiMap<K, V> m, T e) {
        m.put(keyMapper.apply(e), valueMapper.apply(e));
    }

    private static <K, V> MultiMap<K, V> combine(MultiMap<K, V> m1, MultiMap<K, V> m2) {
        m1.putAll(m2);
        return m1;
    }

    @SuppressWarnings("unchecked")
    private static <I, R> Function<I, R> castingIdentity() {
        return i -> (R) i;
    }

    public static <T, K, V, M extends MultiMap<K, V>>
    Collector<T, ?, M> get(Supplier<MultiMap<K, V>> supplier,
                           Function<? super T, ? extends K> keyMapper,
                           Function<? super T, ? extends V> valueMapper) {
        return new MultiMapCollector<>(supplier, keyMapper, valueMapper);
    }

    public static <T, K, V, M extends MultiMap<K, V>>
    Collector<T, ?, M> get(Function<? super T, ? extends K> keyMapper,
                           Function<? super T, ? extends V> valueMapper) {
        return get(Maps::newMultiMap, keyMapper, valueMapper);
    }
}
