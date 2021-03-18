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

package pascal.taie.analysis.pta.core.solver;

import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.language.types.Type;
import pascal.taie.util.HashUtils;

import java.util.Objects;
import java.util.Optional;

public class PointerFlowEdge {

    private final Kind kind;
    private final Pointer from;
    private final Pointer to;
    /**
     * Type of "to" node. This type is useful for handling some cases,
     * e.g., type casting and reflective assignment.
     * If this field is null, it means that this PFG edge does not have
     * type constraint between "from" and "to" nodes.
     */
    private final Type type;

    public PointerFlowEdge(Kind kind, Pointer from, Pointer to, Type type) {
        this.kind = kind;
        this.from = from;
        this.to = to;
        this.type = type;
    }

    public Kind getKind() {
        return kind;
    }

    public Pointer getFrom() {
        return from;
    }

    public Pointer getTo() {
        return to;
    }

    public Optional<Type> getType() {
        return Optional.ofNullable(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointerFlowEdge that = (PointerFlowEdge) o;
        return kind == that.kind &&
                from.equals(that.from) &&
                to.equals(that.to) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return HashUtils.safeHash(kind, from, to, type);
    }

    @Override
    public String toString() {
        return "[" + kind + "]" + from + " -> " + to;
    }

    enum Kind {
        LOCAL_ASSIGN,
        CAST,

        INSTANCE_LOAD,
        INSTANCE_STORE,

        ARRAY_LOAD,
        ARRAY_STORE,

        STATIC_LOAD,
        STATIC_STORE,

        PARAMETER_PASSING,
        RETURN,
    }
}
