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


package pascal.taie.language.generics;

import pascal.taie.util.Experimental;
import pascal.taie.util.collection.Maps;

import java.util.concurrent.ConcurrentMap;

/**
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se20/html/jvms-4.html#jvms-TypeVariableSignature">
 * JVM Spec. 4.7.9.1 TypeVariableSignature</a>
 */
public final class TypeVariableGSignature implements ReferenceTypeGSignature {

    private static final ConcurrentMap<String, TypeVariableGSignature> map =
            Maps.newConcurrentMap(48);

    private final String typeName;

    private TypeVariableGSignature(String typeName) {
        this.typeName = typeName;
    }

    public static TypeVariableGSignature of(String typeName) {
        return map.computeIfAbsent(typeName, TypeVariableGSignature::new);
    }

    @Experimental
    public String getTypeName() {
        return typeName;
    }

    @Override
    public String toString() {
        return typeName;
    }

}
