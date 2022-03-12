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
 * A simple map and list based implementation of {@link ObjectIdMapper}.
 * The ids of the objects are same as their indexes in the list.
 *
 * If {@link #getId(Object)} is called with an object that was absent in this mapper,
 * it will be added to the mapper automatically.
 * If {@link #getObject(int)} is called with an id that does not map to any object,
 * an {@link IllegalArgumentException} will be thrown.
 */
public class SimpleMapper<E> implements ObjectIdMapper<E> {

    private final Map<E, Integer> obj2id;

    private final List<E> id2obj;

    private int counter = 0;

    /**
     * Constructs an empty mapper.
     */
    public SimpleMapper() {
        this.obj2id = Maps.newMap();
        this.id2obj = new ArrayList<>();
    }

    /**
     * Constructs an empty mapper with the specified initial capacity.
     */
    public SimpleMapper(int initialCapacity) {
        this.obj2id = Maps.newMap(initialCapacity);
        this.id2obj = new ArrayList<>(initialCapacity);
    }

    /**
     * Constructs a mapper with a collection. The elements in the collection
     * will be added to the new-created mapper.
     */
    public SimpleMapper(Collection<? extends E> c) {
        this(c.size());
        c.forEach(this::getId);
    }

    @Override
    public int getId(E o) {
        Objects.requireNonNull(o, "null cannot be mapped");
        Integer id = obj2id.get(o);
        if (id == null) {
            obj2id.put(o, counter);
            id2obj.add(o);
            return counter++;
        } else {
            return id;
        }
    }

    @Override
    public E getObject(int id) {
        try {
            return id2obj.get(id);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(
                    "id " + id + " was not mapped to any object", e);
        }
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "SimpleMapper{", "}");
        for (int i = 0; i < counter; i++) {
            joiner.add(i + ":" + id2obj.get(i));
        }
        return joiner.toString();
    }
}
