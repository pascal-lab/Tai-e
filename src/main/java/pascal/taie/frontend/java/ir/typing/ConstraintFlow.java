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

package pascal.taie.frontend.java.ir.typing;

import java.util.Optional;

import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.language.type.Type;

/**
 * Represents the backward propagation of type constraints along a graph edge.
 */
final class ConstraintFlow {

    private final FrontendTypeSystem typeSystem;

    /**
     * The edge along which the constraint is propagating backwards.
     */
    private final TypeFlowEdge edge;

    /**
     * The source constraint being propagated.
     */
    private final Type type;

    ConstraintFlow(FrontendTypeSystem typeSystem, TypeFlowEdge edge, Type originalConstraint) {
        this.typeSystem = typeSystem;
        this.edge = edge;
        this.type = originalConstraint;
    }

    /**
     * Returns the target node for this constraint propagation.
     */
    TypeFlowNode getTargetNode() {
        return edge.source();
    }

    /**
     * Calculates the constraint to be applied to the target node.
     */
    Optional<Type> getTargetConstraintType() {
        return switch (edge.kind()) {
            case VAR_VAR -> Optional.of(type);
            case VAR_ARRAY -> TypeUtils.decreaseDim(type);
            case ARRAY_VAR -> TypeUtils.increaseDim(type, typeSystem);
        };
    }
}
