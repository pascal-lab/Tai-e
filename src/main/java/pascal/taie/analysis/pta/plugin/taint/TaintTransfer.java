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

package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.language.classes.JMethod;

/**
 * Represents taint transfer between argument/base/return variables
 * caused by invocation to specific method.
 */
class TaintTransfer {

    /**
     * Special number representing the base variable.
     */
    static final int BASE = -1;

    /**
     * Special number representing the variable that receivers
     * the return value of the invocation.
     */
    static final int RETURN = -2;

    /**
     * The method causing taint transfer.
     */
    private final JMethod method;

    /**
     * Index of source variable of the transfer.
     */
    private final int from;

    /**
     * Index of target variable of the transfer.
     */
    private final int to;

    TaintTransfer(JMethod method, int from, int to) {
        this.method = method;
        this.from = from;
        this.to = to;
    }

    JMethod getMethod() {
        return method;
    }

    int getFrom() {
        return from;
    }

    int getTo() {
        return to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TaintTransfer that = (TaintTransfer) o;
        return method.equals(that.method) &&
                from == that.from && to == that.to;
    }

    @Override
    public int hashCode() {
        int result = method.hashCode();
        result = 31 * result + from;
        result = 31 * result + to;
        return result;
    }

    @Override
    public String toString() {
        return method + ": " + toString(from) + " -> " + toString(to);
    }

    private static String toString(int index) {
        switch (index) {
            case BASE: return "base";
            case RETURN: return "return";
            default: return Integer.toString(index);
        }
    }
}
