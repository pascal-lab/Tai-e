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

package bamboo.pta.jimple;

import bamboo.pta.element.Variable;
import bamboo.pta.statement.ArrayLoad;
import bamboo.pta.statement.ArrayStore;
import bamboo.pta.statement.Call;
import bamboo.pta.statement.InstanceLoad;
import bamboo.pta.statement.InstanceStore;
import soot.Local;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

class JimpleVariable implements Variable {

    private final Local var;

    private final JimpleType type;

    private final JimpleMethod containerMethod;

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

    public JimpleVariable(Local var, JimpleType type, JimpleMethod containerMethod) {
        this.var = var;
        this.type = type;
        this.containerMethod = containerMethod;
    }

    @Override
    public JimpleType getType() {
        return type;
    }

    @Override
    public JimpleMethod getContainerMethod() {
        return containerMethod;
    }

    @Override
    public String getName() {
        return var.getName();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JimpleVariable that = (JimpleVariable) o;
        return var.equals(that.var);
    }

    @Override
    public int hashCode() {
        return var.hashCode();
    }

    @Override
    public String toString() {
        return containerMethod + "/" + var.getName();
    }
}
