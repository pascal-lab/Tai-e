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

    // Names of special important classes
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

    // Names of invokedynamic-related classes
    public static final String CALL_SITE = "java.lang.invoke.CallSite";

    public static final String METHOD_HANDLE = "java.lang.invoke.MethodHandle";

    public static final String LOOKUP = "java.lang.invoke.MethodHandles$Lookup";

    public static final String VAR_HANDLE = "java.lang.invoke.VarHandle";

    public static final String METHOD_TYPE = "java.lang.invoke.MethodType";

    // Names of special exceptions
    public static final String ABSTRACT_METHOD_ERROR = "java.lang.AbstractMethodError";

    public static final String ARITHMETIC_EXCEPTION = "java.lang.ArithmeticException";

    public static final String ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION = "java.lang.ArrayIndexOutOfBoundsException";

    public static final String ARRAY_STORE_EXCEPTION = "java.lang.ArrayStoreException";

    public static final String CLASS_CAST_EXCEPTION = "java.lang.ClassCastException";

    public static final String CLASS_NOT_FOUND_EXCEPTION = "java.lang.ClassNotFoundException";

    public static final String CLONE_NOT_SUPPORTED_EXCEPTION = "java.lang.CloneNotSupportedException";

    public static final String EXCEPTION_IN_INITIALIZER_ERROR = "java.lang.ExceptionInInitializerError";

    public static final String ILLEGAL_ACCESS_ERROR = "java.lang.IllegalAccessError";

    public static final String ILLEGAL_MONITOR_STATE_EXCEPTION = "java.lang.IllegalMonitorStateException";

    public static final String INCOMPATIBLE_CLASS_CHANGE_ERROR = "java.lang.IncompatibleClassChangeError";

    public static final String INDEX_OUT_OF_BOUNDS_EXCEPTION = "java.lang.IndexOutOfBoundsException";

    public static final String INSTANTIATION_ERROR = "java.lang.InstantiationError";

    public static final String INTERNAL_ERROR = "java.lang.InternalError";

    public static final String INTERRUPTED_EXCEPTION = "java.lang.InterruptedException";

    public static final String LINKAGE_ERROR = "java.lang.LinkageError";

    public static final String NEGATIVE_ARRAY_SIZE_EXCEPTION = "java.lang.NegativeArraySizeException";

    public static final String NO_CLASS_DEF_FOUND_ERROR = "java.lang.NoClassDefFoundError";

    public static final String NO_SUCH_FIELD_ERROR = "java.lang.NoSuchFieldError";

    public static final String NO_SUCH_METHOD_ERROR = "java.lang.NoSuchMethodError";

    public static final String NULL_POINTER_EXCEPTION = "java.lang.NullPointerException";

    public static final String OUT_OF_MEMORY_ERROR = "java.lang.OutOfMemoryError";

    public static final String RUNTIME_EXCEPTION = "java.lang.RuntimeException";

    public static final String STACK_OVERFLOW_ERROR = "java.lang.StackOverflowError";

    public static final String UNKNOWN_ERROR = "java.lang.UnknownError";

    public static final String UNSATISFIED_LINK_ERROR = "java.lang.UnsatisfiedLinkError";

    public static final String VERIFY_ERROR = "java.lang.VerifyError";

    public static final String ANNOTATION = "java.lang.annotation.Annotation";

    // Suppresses default constructor, ensuring non-instantiability.
    private ClassNames() {
    }
}
