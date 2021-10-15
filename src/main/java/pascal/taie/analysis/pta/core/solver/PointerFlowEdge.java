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
import pascal.taie.language.type.Type;
import pascal.taie.util.Hashes;
import pascal.taie.util.graph.AbstractEdge;

import java.util.Objects;
import java.util.Optional;

public class PointerFlowEdge extends AbstractEdge<Pointer> {

    private final Kind kind;

    /**
     * Type of "source" node. This type is useful for handling some cases,
     * e.g., type casting and reflective assignment.
     * If this field is null, it means that this PFG edge does not have
     * type constraint between "source" and "target" nodes.
     */
    private final Type type;

    public PointerFlowEdge(Kind kind, Pointer source, Pointer target, Type type) {
        super(source, target);
        this.kind = kind;
        this.type = type;
    }

    public Kind getKind() {
        return kind;
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
                source.equals(that.source) &&
                target.equals(that.target) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Hashes.safeHash(kind, source, target, type);
    }

    @Override
    public String toString() {
        return "[" + kind + "]" + source + " -> " + target;
    }

    public enum Kind {
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
