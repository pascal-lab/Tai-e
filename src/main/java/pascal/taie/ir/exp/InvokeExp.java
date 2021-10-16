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

package pascal.taie.ir.exp;

import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.type.Type;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Representation of method invocation expression.
 */
public abstract class InvokeExp implements RValue {

    /**
     * The method reference at the invocation.
     */
    protected final MethodRef methodRef;

    /**
     * The arguments of the invocation.
     */
    protected final List<Var> args;

    protected InvokeExp(MethodRef methodRef, List<Var> args) {
        this.methodRef = methodRef;
        this.args = List.copyOf(args);
    }

    @Override
    public Type getType() {
        return methodRef.getReturnType();
    }

    /**
     * @return the method reference at the invocation.
     */
    public MethodRef getMethodRef() {
        return methodRef;
    }

    /**
     * @return the number of the arguments of the invocation.
     */
    public int getArgCount() {
        return args.size();
    }

    /**
     * @return the i-th argument of the invocation.
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index >= getArgCount())
     */
    public Var getArg(int i) {
        return args.get(i);
    }

    /**
     * @return a list of arguments of the invocation.
     */
    public List<Var> getArgs() {
        return args;
    }

    public abstract String getInvokeString();

    public String getArgsString() {
        return "(" + args.stream()
                .map(Var::toString)
                .collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public List<RValue> getUses() {
        return List.copyOf(args);
    }
}
