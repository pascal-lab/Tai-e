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

package pascal.taie.analysis.graph.cfg;

import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

class ExceptionalEdge<N> extends Edge<N> {

    private final Set<ClassType> exceptions;

    ExceptionalEdge(Edge.Kind kind, N source, N target,
                    Set<ClassType> exceptions) {
        super(kind, source, target);
        // other exception types might be added to this exceptional edge later,
        // thus we do not use unmodifiable set to store exception types
        this.exceptions = Sets.newHybridSet(exceptions);
    }

    void addExceptions(Collection<ClassType> exceptions) {
        this.exceptions.addAll(exceptions);
    }

    @Override
    public Set<ClassType> getExceptions() {
        return Collections.unmodifiableSet(exceptions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ExceptionalEdge<?> that = (ExceptionalEdge<?>) o;
        return exceptions.equals(that.exceptions);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + exceptions.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + " with exceptions " + exceptions;
    }
}
