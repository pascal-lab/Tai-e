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

import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.language.type.Type;

import java.util.Optional;

/**
 * Represents the forward propagation of the concrete type along a graph edge.
 */
final class TypeFlow {

    private final FrontendTypeSystem typeSystem;

    /**
     * The edge along which the type is propagating.
     */
    private final TypeFlowEdge edge;

    /**
     * The source type being propagated.
     */
    private final Type type;

    TypeFlow(FrontendTypeSystem typeSystem, TypeFlowEdge edge, Type type) {
        this.typeSystem = typeSystem;
        this.edge = edge;
        this.type = type;
    }

    /**
     * Returns the target node for this type propagation.
     */
    TypeFlowNode getTargetNode() {
        return edge.target();
    }

    /**
     * Calculates the type to be applied to the target node.
     */
    Optional<Type> getTargetType() {
        return switch (edge.kind()) {
            case VAR_VAR -> Optional.of(type);
            case VAR_ARRAY -> TypeUtils.increaseDim(type, typeSystem);
            case ARRAY_VAR -> TypeUtils.decreaseDim(type);
        };
    }
}
