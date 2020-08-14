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

package bamboo.pta.core.heap;

import bamboo.pta.element.AbstractObj;
import bamboo.pta.element.Method;
import bamboo.pta.element.Obj;
import bamboo.pta.element.Type;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a set of merged objects.
 */
class MergedObj extends AbstractObj {

    private final String name;

    /**
     * Set of objects represented by this merged object.
     */
    private final Set<Obj> representedObjs = ConcurrentHashMap.newKeySet();

    /**
     * The representative object of this merged object. It is the first
     * object added.
     */
    private final AtomicReference<Obj> representative = new AtomicReference<>();

    MergedObj(Type type, String name) {
        super(type);
        this.name = name;
    }

    void addRepresentedObj(Obj obj) {
        representative.compareAndSet(null, obj);
        representedObjs.add(obj);
    }

    @Override
    public Kind getKind() {
        return Kind.MERGED;
    }

    @Override
    public Set<Obj> getAllocation() {
        return representedObjs;
    }

    @Override
    public Optional<Method> getContainerMethod() {
        return representative.get() != null ?
                representative.get().getContainerMethod() :
                Optional.empty();
    }

    @Override
    public Type getContainerType() {
        return representative.get() != null ?
                representative.get().getContainerType() :
                type;
    }

    @Override
    public String toString() {
        return name;
    }
}
