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

package pascal.taie.pta.env.nativemodel;

import pascal.taie.callgraph.CallKind;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.classes.MethodRef;
import pascal.taie.pta.ir.AbstractCallSite;
import pascal.taie.pta.ir.Variable;

import java.util.List;
import java.util.Objects;

/**
 * Mock call sites for model the side effects of native code.
 */
class MockCallSite extends AbstractCallSite {

    /**
     * Identifier of this mock call site.
     */
    private final String id;

    MockCallSite(CallKind kind, MethodRef methodRef, Variable receiver,
                 List<Variable> args, JMethod containerMethod,
                 String id) {
        super(kind);
        this.methodRef = methodRef;
        this.receiver = receiver;
        this.args = args;
        this.containerMethod = containerMethod;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockCallSite that = (MockCallSite) o;
        return containerMethod.equals(that.containerMethod)
                && Objects.equals(receiver, that.receiver)
                && args.equals(that.args)
                && methodRef.equals(that.methodRef)
                && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(containerMethod, receiver, args, methodRef, id);
    }

    @Override
    public String toString() {
        return String.format("[Mock@%s]%s/%s%s(%s)", id, containerMethod,
                receiver != null ? receiver.getName() + "." : "",
                methodRef, args);
    }
}
