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

/**
 * In <a href="https://docs.oracle.com/javase/specs/jvms/se20/html/jvms-4.html#jvms-ReferenceTypeSignature">
 * JVM Spec. 4.7.9.1 ReferenceTypeSignature</a>,
 * A <i>reference type signature</i> represents a reference type of
 * the Java programming language, that is,
 * a class or interface type({@link ClassTypeGSignature}),
 * a type variable({@link TypeVariableGSignature}),
 * or an array type({@link ArrayTypeGSignature}).
 */
public sealed interface ReferenceTypeGSignature
        extends TypeGSignature
        permits ArrayTypeGSignature, ClassTypeGSignature, TypeVariableGSignature {

    default boolean isJavaLangObject() {
        return false;
    }

}
