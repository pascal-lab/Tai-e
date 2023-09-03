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

package pascal.taie.language.classes;

/**
 * Provides signatures of special methods and fields.
 */
@StringProvider
public final class Signatures {

    // Signatures of special methods
    public static final String FINALIZE = "<java.lang.Object: void finalize()>";

    public static final String FINALIZER_REGISTER = "<java.lang.ref.Finalizer: void register(java.lang.Object)>";

    public static final String REFERENCE_INIT = "<java.lang.ref.Reference: void <init>(java.lang.Object,java.lang.ref.ReferenceQueue)>";

    public static final String LAMBDA_METAFACTORY = "<java.lang.invoke.LambdaMetafactory: java.lang.invoke.CallSite metafactory(java.lang.invoke.MethodHandles$Lookup,java.lang.String,java.lang.invoke.MethodType,java.lang.invoke.MethodType,java.lang.invoke.MethodHandle,java.lang.invoke.MethodType)>";

    public static final String LAMBDA_ALTMETAFACTORY = "<java.lang.invoke.LambdaMetafactory: java.lang.invoke.CallSite altMetafactory(java.lang.invoke.MethodHandles$Lookup,java.lang.String,java.lang.invoke.MethodType,java.lang.Object[])>";

    public static final String STRING_CONCAT_FACTORY_MAKE = "<java.lang.invoke.StringConcatFactory: java.lang.invoke.CallSite makeConcatWithConstants(java.lang.invoke.MethodHandles$Lookup,java.lang.String,java.lang.invoke.MethodType,java.lang.String,java.lang.Object[])>";

    public static final String INVOKEDYNAMIC_FINDVIRTUAL = "<java.lang.invoke.MethodHandles$Lookup: java.lang.invoke.MethodHandle findVirtual(java.lang.Class,java.lang.String,java.lang.invoke.MethodType)>";

    public static final String INVOKEDYNAMIC_FINDSTATIC = "<java.lang.invoke.MethodHandles$Lookup: java.lang.invoke.MethodHandle findStatic(java.lang.Class,java.lang.String,java.lang.invoke.MethodType)>";

    public static final String INVOKEDYNAMIC_FINDSPECIAL = "<java.lang.invoke.MethodHandles$Lookup: java.lang.invoke.MethodHandle findSpecial(java.lang.Class,java.lang.String,java.lang.invoke.MethodType,java.lang.Class)>";

    public static final String INVOKEDYNAMIC_FINDCONSTRUCTOR = "<java.lang.invoke.MethodHandles$Lookup: java.lang.invoke.MethodHandle findConstructor(java.lang.Class,java.lang.invoke.MethodType)>";

    // Signatures of special fields
    public static final String REFERENCE_PENDING = "<java.lang.ref.Reference: java.lang.ref.Reference pending>";

    // Suppresses default constructor, ensuring non-instantiability.
    private Signatures() {
    }
}
