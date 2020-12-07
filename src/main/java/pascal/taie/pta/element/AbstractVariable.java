/*
 * Tai'e - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai'e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.pta.element;

import pascal.taie.pta.statement.ArrayLoad;
import pascal.taie.pta.statement.ArrayStore;
import pascal.taie.pta.statement.Call;
import pascal.taie.pta.statement.InstanceLoad;
import pascal.taie.pta.statement.InstanceStore;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * All implementations of Variable should inherit this class.
 */
public abstract class AbstractVariable implements Variable {

    protected final Type type;
    protected final Method container;

    /**
     * Set of call sites where this variable is the base variable
     */
    private Set<Call> calls = Collections.emptySet();

    /**
     * Set of instance stores where this variable is the base variable
     */
    private Set<InstanceStore> instStores = Collections.emptySet();

    /**
     * Set of instance loads where this variable is the base variable
     */
    private Set<InstanceLoad> instLoads = Collections.emptySet();

    /**
     * Set of array stores where this variable is the base variable
     */
    private Set<ArrayStore> arrayStores = Collections.emptySet();

    /**
     * Set of array loads where this variable is the base variable
     */
    private Set<ArrayLoad> arrayLoads = Collections.emptySet();

    protected AbstractVariable(Type type, Method container) {
        this.type = type;
        this.container = container;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Method getContainerMethod() {
        return container;
    }

    @Override
    public void addCall(Call call) {
        if (calls.isEmpty()) {
            calls = new LinkedHashSet<>(4);
        }
        calls.add(call);
    }

    @Override
    public Set<Call> getCalls() {
        return calls;
    }

    @Override
    public void addInstanceLoad(InstanceLoad load) {
        if (instLoads.isEmpty()) {
            instLoads = new LinkedHashSet<>(4);
        }
        instLoads.add(load);
    }

    @Override
    public Set<InstanceLoad> getInstanceLoads() {
        return instLoads;
    }

    @Override
    public Set<InstanceStore> getInstanceStores() {
        return instStores;
    }

    @Override
    public void addInstanceStore(InstanceStore store) {
        if (instStores.isEmpty()) {
            instStores = new LinkedHashSet<>(4);
        }
        instStores.add(store);
    }

    @Override
    public void addArrayLoad(ArrayLoad load) {
        if (arrayLoads.isEmpty()) {
            arrayLoads = new LinkedHashSet<>(4);
        }
        arrayLoads.add(load);
    }

    @Override
    public Set<ArrayLoad> getArrayLoads() {
        return arrayLoads;
    }

    @Override
    public void addArrayStore(ArrayStore store) {
        if (arrayStores.isEmpty()) {
            arrayStores = new LinkedHashSet<>(4);
        }
        arrayStores.add(store);
    }

    @Override
    public Set<ArrayStore> getArrayStores() {
        return arrayStores;
    }
}
