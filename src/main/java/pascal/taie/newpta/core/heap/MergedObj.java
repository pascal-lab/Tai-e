/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.newpta.core.heap;

import pascal.taie.java.classes.JMethod;
import pascal.taie.java.types.Type;

import java.util.Optional;
import java.util.Set;

import static pascal.taie.util.CollectionUtils.newSet;

/**
 * Represents a set of merged objects.
 */
public class MergedObj implements Obj {
    
    private final String name;

    private final Type type;

    /**
     * Set of objects represented by this merged object.
     */
    private final Set<Obj> representedObjs = newSet();

    /**
     * The representative object of this merged object. It is the first
     * object added.
     */
    private Obj representative;

    MergedObj(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    void addRepresentedObj(Obj obj) {
        setRepresentative(obj);
        representedObjs.add(obj);
    }

    private void setRepresentative(Obj obj) {
        if (representative == null) {
            representative = obj;
        }
    }

    @Override
    public Set<Obj> getAllocation() {
        return representedObjs;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Optional<JMethod> getContainerMethod() {
        return representative != null ?
                representative.getContainerMethod() :
                Optional.empty();
    }

    @Override
    public Type getContainerType() {
        return representative != null ?
                representative.getContainerType() : type;
    }

    @Override
    public String toString() {
        return name;
    }
}
