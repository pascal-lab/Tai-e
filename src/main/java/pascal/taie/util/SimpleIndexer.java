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
