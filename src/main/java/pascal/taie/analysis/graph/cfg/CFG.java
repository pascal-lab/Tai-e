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

import pascal.taie.ir.IR;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.graph.Graph;

import java.util.stream.Stream;

/**
 * Representation of a control-flow graph of a method.
 * @param <N> type of nodes.
 */
public interface CFG<N> extends Graph<N> {

    IR getIR();

    default JMethod getMethod() {
        return getIR().getMethod();
    }

    N getEntry();

    Stream<N> getExits();
}
