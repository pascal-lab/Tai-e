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

package pascal.taie.analysis.oldpta.ir;

import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * All implementations of Variable should inherit this class.
 */
public abstract class AbstractVariable implements Variable {

    protected final Type type;
    protected final JMethod container;

    /**
     * Set of call sites where this variable is the base variable
     */
    private Set<Call> calls = Set.of();

    /**
     * Set of instance stores where this variable is the base variable
     */
    private Set<InstanceStore> instStores = Set.of();

    /**
     * Set of instance loads where this variable is the base variable
     */
    private Set<InstanceLoad> instLoads = Set.of();

    /**
     * Set of array stores where this variable is the base variable
     */
    private Set<ArrayStore> arrayStores = Set.of();

    /**
     * Set of array loads where this variable is the base variable
     */
    private Set<ArrayLoad> arrayLoads = Set.of();

    protected AbstractVariable(Type type, JMethod container) {
        this.type = type;
        this.container = container;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public JMethod getContainerMethod() {
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
