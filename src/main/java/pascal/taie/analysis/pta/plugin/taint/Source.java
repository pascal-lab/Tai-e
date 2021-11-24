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
import pascal.taie.util.Hashes;

/**
 * Represents a source that consists of a source method and
 * type of taint object.
 */
class Source {

    /**
     * The source method.
     */
    private final JMethod method;

    /**
     * Type of taint object.
     */
    private final Type type;

    Source(JMethod method, Type type) {
        this.method = method;
        this.type = type;
    }

    /**
     * @return the source method.
     */
    JMethod getMethod() {
        return method;
    }

    /**
     * @return the type of the taint object.
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
        Source that = (Source) o;
        return method.equals(that.method) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Hashes.hash(method, type);
    }

    @Override
    public String toString() {
        return method + "(" + type + ")";
    }
}
