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

/**
 * In <a href="https://docs.oracle.com/javase/specs/jvms/se20/html/jvms-4.html#jvms-ArrayTypeSignature">JVM Spec. 4.7.9.1 ArrayTypeSignature</a>,
 * an <i>array type signature</i> represents one dimension of an array type.
 * For example, the bytecode signature and the corresponding Java generic are:
 * <ul>
 *     <li>{@code [[B} and {@code byte[][]}</li>
 *     <li>{@code [[Ljava/lang/String;} and {@code String[][]}</li>
 *     <li>{@code [[Ljava/util/HashMap<TK;TV;>;} and {@code java.util.HashMap<K, V>[][]}</li>
 * </ul>
 * In our implementation, we use {@link ArrayTypeGSignature} to represent
 * an n-dimensions({@link #dimensions}) array for a specific type({@link #baseTypeGSig}).
 */
public final class ArrayTypeGSignature implements ReferenceTypeGSignature {

    private final int dimensions;

    private final TypeGSignature baseTypeGSig;

    ArrayTypeGSignature(int dimensions, TypeGSignature baseTypeGSig) {
        this.dimensions = dimensions;
        this.baseTypeGSig = baseTypeGSig;
    }

    public int getDimensions() {
        return dimensions;
    }

    @Experimental
    public TypeGSignature getBaseTypeGSignature() {
        return baseTypeGSig;
    }

    @Override
    public String toString() {
        return baseTypeGSig + "[]".repeat(dimensions);
    }
}
