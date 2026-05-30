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
 * Provides names of special classes.
 */
@StringProvider
public final class ClassNames {

    public static final String OBJECT = "java.lang.Object";

    public static final String SERIALIZABLE = "java.io.Serializable";

    public static final String CLONEABLE = "java.lang.Cloneable";

    public static final String CLASS = "java.lang.Class";

    public static final String ARRAY = "java.lang.reflect.Array";

    public static final String CONSTRUCTOR = "java.lang.reflect.Constructor";

    public static final String METHOD = "java.lang.reflect.Method";

    public static final String FIELD = "java.lang.reflect.Field";

    public static final String STRING = "java.lang.String";

    public static final String STRING_BUILDER = "java.lang.StringBuilder";

    public static final String STRING_BUFFER = "java.lang.StringBuffer";

    public static final String BOOLEAN = "java.lang.Boolean";

    public static final String BYTE = "java.lang.Byte";

    public static final String SHORT = "java.lang.Short";

    public static final String CHARACTER = "java.lang.Character";

    public static final String INTEGER = "java.lang.Integer";

    public static final String LONG = "java.lang.Long";

    public static final String FLOAT = "java.lang.Float";

    public static final String DOUBLE = "java.lang.Double";

    public static final String VOID = "java.lang.Void";

    public static final String THREAD = "java.lang.Thread";

    public static final String THREAD_GROUP = "java.lang.ThreadGroup";

    public static final String THROWABLE = "java.lang.Throwable";

    public static final String ERROR = "java.lang.Error";

    public static final String EXCEPTION = "java.lang.Exception";

    public static final String ANNOTATION = "java.lang.annotation.Annotation";

    // Names of invokedynamic-related classes
    public static final String CALL_SITE = "java.lang.invoke.CallSite";

    public static final String METHOD_HANDLE = "java.lang.invoke.MethodHandle";

    public static final String LOOKUP = "java.lang.invoke.MethodHandles$Lookup";

    public static final String VAR_HANDLE = "java.lang.invoke.VarHandle";

    public static final String METHOD_TYPE = "java.lang.invoke.MethodType";

    // Suppresses default constructor, ensuring non-instantiability.
    private ClassNames() {
    }
}
