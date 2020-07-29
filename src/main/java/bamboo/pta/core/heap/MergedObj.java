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

/**
 * Represents a set of merged objects.
 */
class MergedObj extends AbstractObj {

    /**
     * Set of objects represented by this merged object.
     */
    private final Set<Obj> representedObjs = ConcurrentHashMap.newKeySet();

    MergedObj(Type type) {
        super(type);
    }

    void addRepresentedObj(Obj obj) {
        representedObjs.add(obj);
    }

    @Override
    public Kind getKind() {
        return Kind.MERGED;
    }

    @Override
    public Object getAllocation() {
        return representedObjs;
    }

    @Override
    public Optional<Method> getContainerMethod() {
        return Optional.empty();
    }

    @Override
    public Type getContainerType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("[Merged %s]@%d",
                type, System.identityHashCode(this));
    }
}
