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
