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

package pascal.taie.util;

import pascal.taie.util.collection.Maps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * A simple map and list based implementation of {@link Indexer}.
 * The indexes of the objects are same as their indexes in the list.
 * <p>
 * If {@link #getIndex(Object)} is called with an object that was absent
 * in this indexer, it will be added to the indexer automatically.
 * If {@link #getObject(int)} is called with an index that does not
 * map to any object, an {@link IllegalArgumentException} will be thrown.
 * <p>
 * If you want to obtain an object of its corresponding index by
 * {@code getObject(i)}, make sure that the object has been indexed by
 * {@code getIndex(o)}.
 */
public class SimpleIndexer<E> implements Indexer<E> {

    private final Map<E, Integer> obj2index;

    private final List<E> index2obj;

    private int counter = 0;

    /**
     * Constructs an empty indexer.
     */
    public SimpleIndexer() {
        this.obj2index = Maps.newMap();
        this.index2obj = new ArrayList<>();
    }

    /**
     * Constructs an empty mapper with the specified initial capacity.
     */
    public SimpleIndexer(int initialCapacity) {
        this.obj2index = Maps.newMap(initialCapacity);
        this.index2obj = new ArrayList<>(initialCapacity);
    }

    /**
     * Constructs a mapper with a collection. The elements in the collection
     * will be added to the new-created mapper.
     */
    public SimpleIndexer(Collection<? extends E> c) {
        this(c.size());
        c.forEach(this::getIndex);
    }

    @Override
    public int getIndex(E o) {
        Objects.requireNonNull(o, "null cannot be indexed");
        Integer id = obj2index.get(o);
        if (id == null) {
            obj2index.put(o, counter);
            index2obj.add(o);
            return counter++;
        } else {
            return id;
        }
    }

    @Override
    public E getObject(int index) {
        try {
            return index2obj.get(index);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(
                    "index " + index + " was not mapped to any object", e);
        }
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "SimpleIndexer{", "}");
        for (int i = 0; i < counter; i++) {
            joiner.add(i + ":" + index2obj.get(i));
        }
        return joiner.toString();
    }
}
