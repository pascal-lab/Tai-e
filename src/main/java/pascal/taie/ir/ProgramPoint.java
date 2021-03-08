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

import pascal.taie.ir.stmt.Stmt;
import pascal.taie.java.classes.JMethod;
import pascal.taie.util.HashUtils;

/**
 * Representation of specific program points, consists of
 * a method (container) and a statement (point).
 */
public class ProgramPoint {

    private final JMethod method;

    private final Stmt stmt;

    public ProgramPoint(JMethod method, Stmt stmt) {
        this.method = method;
        this.stmt = stmt;
    }

    public JMethod getMethod() {
        return method;
    }

    public Stmt getStmt() {
        return stmt;
    }

    @Override
    public int hashCode() {
        return HashUtils.hash(method, stmt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProgramPoint that = (ProgramPoint) o;
        return method.equals(that.method) && stmt.equals(that.stmt);
    }

    @Override
    public String toString() {
        // TODO: display source location?
        return "[PP] " +method + ":" + stmt.getIndex();
    }
}
