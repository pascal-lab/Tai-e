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

package pascal.taie.frontend.newfrontend;

import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.TypeSystem;

/**
 * Represents a type context, encapsulating a {@link TypeSystem} and essential core JVM types used in the frontend.
 *
 * <p>
 * This class is designed to cache frequently accessed types to improve performance. Accessing types through the
 * {@link TypeSystem#getType} method can be slow due to the need to query or update the underlying type map.
 * </p>
 */
public record TypeContext(TypeSystem typeSystem,
                          ClassType object,
                          ClassType serializable,
                          ClassType cloneable,
                          ClassType string,
                          ClassType reflectArray,
                          ClassType klass,
                          ClassType throwable) {
    /**
     * Constructs a new {@link TypeContext} instance with the given {@link TypeSystem}.
     */
    public TypeContext(TypeSystem typeSystem) {
        this(typeSystem,
                typeSystem.getClassType(ClassNames.OBJECT),
                typeSystem.getClassType(ClassNames.SERIALIZABLE),
                typeSystem.getClassType(ClassNames.CLONEABLE),
                typeSystem.getClassType(ClassNames.STRING),
                typeSystem.getClassType(ClassNames.ARRAY),
                typeSystem.getClassType(ClassNames.CLASS),
                typeSystem.getClassType(ClassNames.THROWABLE));
    }
}
