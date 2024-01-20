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

package pascal.taie.language.type;

import java.util.Map;
import java.util.Set;

public interface PrimitiveType extends ValueType {

    /**
     * @return {@code true} if given name represents a primitive type.
     */
    static boolean isPrimitiveType(String name) {
        // stub implementation to pass compilation
        return Set.of("int").contains(name);
    }

    /**
     * @return the primitive type specified by specific name.
     * @throws IllegalArgumentException if given name is irrelevant to any primitive type.
     */
    static PrimitiveType get(String name) {
        // stub implementation to pass compilation
        return Map.of("int", IntType.INT).get(name);
        //throw new IllegalArgumentException(name + " is not primitive type");
    }

    @Override
    default String getName() {
        return toString();
    }

    /**
     * JVM Spec. (2.11.1): most operations on values of actual types
     * boolean, byte, char, and short are correctly performed by instructions
     * operating on values of computational type int.
     *
     * @return {@code true} if the values of this type are represented
     * as integers in computation.
     */
    boolean asInt();
}
