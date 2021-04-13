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

import pascal.taie.language.types.ClassType;

import java.util.Collection;

class ExceptionalEdge<N> extends Edge<N> {

    private final Collection<ClassType> exceptions;

    public ExceptionalEdge(N kind, N source, N target,
                           Collection<ClassType> exceptions) {
        super(kind, source, target);
        this.exceptions = exceptions;
    }

    @Override
    public Collection<ClassType> getExceptions() {
        return exceptions;
    }

    @Override
    public String toString() {
        return super.toString() + " with " + exceptions;
    }
}
