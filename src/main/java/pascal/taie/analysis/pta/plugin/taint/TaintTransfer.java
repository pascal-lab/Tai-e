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
import pascal.taie.language.type.Type;

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
     * String representation of base variable.
     */
    private static final String BASE_STR = "base";

    /**
     * Special number representing the variable that receivers
     * the result of the invocation.
     */
    static final int RESULT = -2;

    /**
     * String representation of result variable
     */
    private static final String RESULT_STR = "result";

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

    /**
     * Type of transferred taint object.
     */
    private final Type type;

    TaintTransfer(JMethod method, int from, int to, Type type) {
        this.method = method;
        this.from = from;
        this.to = to;
        this.type = type;
    }

    /**'
     * @return the method that causes taint transfer.
     */
    JMethod getMethod() {
        return method;
    }

    /**
     * @return the index of "from" variable.
     */
    int getFrom() {
        return from;
    }

    /**
     * @return the index of "to" variable.
     */
    int getTo() {
        return to;
    }

    /**
     * @return the type of the transferred taint object.
     */
    Type getType() {
        return type;
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
                from == that.from && to == that.to && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        int result = method.hashCode();
        result = 31 * result + from;
        result = 31 * result + to;
        result = 31 * result + type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return method + ": " + toString(from) + " -> " + toString(to) +
                "(" + type + ")";
    }

    /**
     * Coverts string to index.
     */
    static int toInt(String s) {
        switch (s.toLowerCase()) {
            case BASE_STR:
                return BASE;
            case RESULT_STR:
                return RESULT;
            default:
                return Integer.parseInt(s);
        }
    }

    /**
     * Converts index to string.
     */
    private static String toString(int index) {
        switch (index) {
            case BASE:
                return BASE_STR;
            case RESULT:
                return RESULT_STR;
            default:
                return Integer.toString(index);
        }
    }
}
