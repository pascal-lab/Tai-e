/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package panda.pta.statement;

import panda.pta.element.CallSite;
import panda.pta.element.Variable;

import java.util.Optional;

/**
 * Represents a call statement r = o.m()/r = T.m();
 */
public class Call implements Statement {

    private final CallSite callSite;

    /**
     * LHS variable which receives the result of the call.
     * This field is @Nullable.
     */
    private final Variable lhs;

    public Call(CallSite callSite, Variable lhs) {
        this.callSite = callSite;
        callSite.setCall(this);
        this.lhs = lhs;
    }

    public CallSite getCallSite() {
        return callSite;
    }

    public Optional<Variable> getLHS() {
        return Optional.ofNullable(lhs);
    }

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Kind getKind() {
        return Kind.CALL;
    }

    @Override
    public String toString() {
        return lhs != null
                ? lhs + " = " + callSite.toString()
                : callSite.toString();
    }
}
