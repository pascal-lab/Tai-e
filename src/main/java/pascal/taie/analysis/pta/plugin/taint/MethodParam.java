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
 * Represents a specific parameter of a method.
 */
class MethodParam {

    /**
     * The method.
     */
    private final JMethod method;

    /**
     * Index of the parameter.
     */
    private final int index;

    MethodParam(JMethod method, int index) {
        this.method = method;
        this.index = index;
    }

    JMethod getMethod() {
        return method;
    }

    int getIndex() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodParam that = (MethodParam) o;
        return method.equals(that.method) && index == that.index;
    }

    @Override
    public int hashCode() {
        return method.hashCode() * 31 + index;
    }

    @Override
    public String toString() {
        return method + "/" + index;
    }
}
