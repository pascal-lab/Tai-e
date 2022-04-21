/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.graph.cfg;

import pascal.taie.language.type.ClassType;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.Hashes;
import pascal.taie.util.graph.AbstractEdge;

import java.util.Set;

/**
 * Represents CFG edges.
 *
 * @param <N> type of CFG nodes.
 */
public class Edge<N> extends AbstractEdge<N> {

    public enum Kind {

        /**
         * Edge from entry node to real start node.
         */
        ENTRY,

        /**
         * Edge kind for fall-through to next statement.
         */
        FALL_THROUGH,

        /**
         * Edge kind for goto statements.
         */
        GOTO,

        /**
         * Edge kind for if statements when condition is true.
         */
        IF_TRUE,

        /**
         * Edge kind for if statements when condition is false.
         */
        IF_FALSE,

        /**
         * Edge kind for switch statements (explicit case).
         */
        SWITCH_CASE,

        /**
         * Edge kind for switch statements (default case).
         */
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

    Edge(Kind kind, N source, N target) {
        super(source, target);
        this.kind = kind;
    }

    /**
     * @return the kind of the edge.
     * @see Edge.Kind
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * @return true if this edge is a switch-case edge, otherwise false.
     */
    public boolean isSwitchCase() {
        return kind == Kind.SWITCH_CASE;
    }

    /**
     * If this edge is a switch-case edge, then returns the case value.
     * The client code should call {@link #isSwitchCase()} to check if
     * this edge is switch-case edge before calling this method.
     *
     * @throws AnalysisException if this edge is not a switch-case edge.
     */
    public int getCaseValue() {
        // SwitchCaseEdge overrides this method, thus this method
        // should NOT be reachable
        throw new AnalysisException(this + " is not a switch-case edge," +
                " please call isSwitchCase() before calling this method");
    }

    /**
     * @return true if this edge is an exceptional edge, otherwise false.
     */
    public boolean isExceptional() {
        return kind == Kind.CAUGHT_EXCEPTION ||
                kind == Kind.UNCAUGHT_EXCEPTION;
    }

    /**
     * If this edge is an exceptional edge, return the exception types along
     * with this edge, otherwise return an empty set.
     */
    public Set<ClassType> getExceptions() {
        assert isExceptional() : this + " is not an exceptional edge";
        return Set.of();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Edge<?> edge = (Edge<?>) o;
        return kind == edge.kind &&
                source.equals(edge.source) &&
                target.equals(edge.target);
    }

    @Override
    public int hashCode() {
        return Hashes.hash(kind, source, target);
    }

    @Override
    public String toString() {
        return "[" + kind + "]: " + source + " -> " + target;
    }
}
