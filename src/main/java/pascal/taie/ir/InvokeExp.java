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

package pascal.taie.ir;

import pascal.taie.java.classes.MethodRef;
import pascal.taie.java.types.Type;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Representation of method invocation expression.
 */
public abstract class InvokeExp implements Exp {

    protected final MethodRef methodRef;

    protected final List<Atom> args;

    private Site callSite;

    public InvokeExp(MethodRef methodRef, List<Atom> args) {
        this.methodRef = methodRef;
        this.args = Collections.unmodifiableList(args);
    }

    @Override
    public Type getType() {
        return methodRef.getReturnType();
    }

    public MethodRef getMethodRef() {
        return methodRef;
    }

    public int getArgCount() {
        return args.size();
    }

    public Atom getArg(int i) {
        return args.get(i);
    }

    public List<Atom> getArgs() {
        return args;
    }

    public Site getCallSite() {
        return callSite;
    }

    public void setCallSite(Site callSite) {
        this.callSite = callSite;
    }

    protected abstract String getInvokeString();

    protected String getArgsString() {
        return args.stream()
                .map(Atom::toString)
                .collect(Collectors.joining(","));
    }
}
