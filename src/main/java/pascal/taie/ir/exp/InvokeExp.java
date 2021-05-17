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

    protected final MethodRef methodRef;

    protected final List<Var> args;

    protected InvokeExp(MethodRef methodRef, List<Var> args) {
        this.methodRef = methodRef;
        this.args = List.copyOf(args);
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

    public Var getArg(int i) {
        return args.get(i);
    }

    public List<Var> getArgs() {
        return args;
    }

    public abstract String getInvokeString();

    public String getArgsString() {
        return "(" + args.stream()
                .map(Var::toString)
                .collect(Collectors.joining(",")) + ")";
    }

    @Override
    public List<Exp> getUses() {
        return List.copyOf(args);
    }
}
