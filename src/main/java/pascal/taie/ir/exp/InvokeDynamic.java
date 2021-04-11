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

package pascal.taie.ir.exp;

import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.types.Type;

import java.util.List;

public class InvokeDynamic extends InvokeExp {

    private final MethodRef bootstrapMethodRef;

    private final String methodName;

    private final MethodType methodType;

    private final List<Var> bootstrapArgs;

    public InvokeDynamic(MethodRef bootstrapMethodRef,
                         String methodName, MethodType methodType,
                         List<Var> bootstrapArgs, List<Var> args) {
        super(null, args);
        this.bootstrapMethodRef = bootstrapMethodRef;
        this.methodName = methodName;
        this.methodType = methodType;
        this.bootstrapArgs = bootstrapArgs;
    }

    public MethodRef getBootstrapMethodRef() {
        return bootstrapMethodRef;
    }

    public String getMethodName() {
        return methodName;
    }

    public MethodType getMethodType() {
        return methodType;
    }

    public List<Var> getBootstrapArgs() {
        return bootstrapArgs;
    }

    @Override
    public Type getType() {
        return methodType.getReturnType();
    }

    @Override
    public MethodRef getMethodRef() {
        throw new UnsupportedOperationException(
                "InvokeDynamic.getMethodRef() is unavailable");
    }

    @Override
    public String getInvokeString() {
        return "invokedynamic";
    }

    @Override
    public String toString() {
        // TODO: finish me
        return "InvokeDynamic{" +
                "bootstrapMethodRef=" + bootstrapMethodRef +
                ", methodName='" + methodName + '\'' +
                ", methodType=" + methodType +
                ", bootstrapArgs=" + bootstrapArgs +
                '}';
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
