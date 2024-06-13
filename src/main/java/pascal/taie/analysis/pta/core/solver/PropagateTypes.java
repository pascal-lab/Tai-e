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

import pascal.taie.ir.exp.Exp;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Checks whether propagation of objects is allowed.
 * The decision is made based on the type of the relevant expression and
 * the allowed types given in the constructor.
 */
public class PropagateTypes {

    private final boolean allowReference;

    private final boolean allowNull;

    private final Set<PrimitiveType> allowedPrimitives;

    /**
     * Elements of {@code types} can be:
     * <ul>
     *     <li>"reference": allow reference types</li>
     *     <li>"null" (or null): allow null type</li>
     *     <li>various primitive types: allow the corresponding primitive types</li>
     * </ul>
     */
    public PropagateTypes(List<String> types, TypeSystem typeSystem) {
        allowReference = types.contains("reference");
        allowNull = types.contains("null") || types.contains(null);
        allowedPrimitives = types.stream()
                .filter(typeSystem::isPrimitiveType)
                .map(typeSystem::getPrimitiveType)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAllowed(Type type) {
        if (type instanceof ReferenceType) {
            return (type instanceof NullType) ? allowNull : allowReference;
        } else if (type instanceof PrimitiveType primitiveType) {
            return allowedPrimitives.contains(primitiveType);
        }
        return false;
    }

    public boolean isAllowed(Exp exp) {
        return isAllowed(exp.getType());
    }

    @Override
    public String toString() {
        return "PropagateTypes{" +
                "allowReference=" + allowReference +
                ", allowNull=" + allowNull +
                ", allowedPrimitives=" + allowedPrimitives +
                '}';
    }
}
