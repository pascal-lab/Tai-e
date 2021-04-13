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
import java.util.Collections;

public class Edge<N> {

    public enum Kind {

        /** Edge from entry node to real start node. */
        ENTRY,

        /** Edge kind for fall-through to next statement. */
        FALL_THROUGH,

        /** Edge kind for goto statements. */
        GOTO,

        /** Edge kind for if statements when condition is true. */
        IF_TRUE,

        /** Edge kind for if statements when condition is false. */
        IF_FALSE,

        /** Edge kind for switch statements (explicit case). */
        SWITCH_CASE,

        /** Edge kind for switch statements (default case). */
        SWITCH_DEFAULT,

        /**
         * Edge representing exceptional control flow from an
         * exception-raising node to an explicit handler for the exception.
         */
        CAUGHT_EXCEPTION,

        /**
         * Edge representing the possibility that a node raise an exception
         * that cannot be caught by the current method.
         * These edges always go to the exit node of the CFG.
         */
        UNCAUGHT_EXCEPTION,

        /**
         * Edge kind for return statements.
         * These edges always go to the exit node of the CFG.
         */
        RETURN,
    }

    private final Kind kind;

    private final N source;

    private final N target;

    public Edge(Kind kind, N source, N target) {
        this.kind = kind;
        this.source = source;
        this.target = target;
    }

    public Kind getKind() {
        return kind;
    }

    public N getSource() {
        return source;
    }

    public N getTarget() {
        return target;
    }

    public Collection<ClassType> getExceptions() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "[" + kind + "]: " + source + " -> " + target;
    }
}
