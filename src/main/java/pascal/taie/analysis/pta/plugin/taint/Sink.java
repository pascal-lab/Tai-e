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
 * Represents a sink that consists of a sink method and
 * a sensitive index.
 */
class Sink {

    /**
     * The sink method.
     */
    private final JMethod method;

    /**
     * Index of the sensitive parameter.
     */
    private final int index;

    Sink(JMethod method, int index) {
        this.method = method;
        this.index = index;
    }

    /**
     * @return the sink method.
     */
    JMethod getMethod() {
        return method;
    }

    /**
     * @return the index of the sensitive argument.
     */
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
        Sink that = (Sink) o;
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
