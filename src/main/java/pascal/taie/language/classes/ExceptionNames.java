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
 * Provides names for special exception classes.
 */
public class ExceptionNames {

    public static final String ARITHMETIC_EXCEPTION = "java.lang.ArithmeticException";

    public static final String ARRAY_STORE_EXCEPTION = "java.lang.ArrayStoreException";

    public static final String CLASS_CAST_EXCEPTION = "java.lang.ClassCastException";

    public static final String CLONE_NOT_SUPPORTED_EXCEPTION = "java.lang.CloneNotSupportedException";

    public static final String EXCEPTION_IN_INITIALIZER_ERROR = "java.lang.ExceptionInInitializerError";

    public static final String INDEX_OUT_OF_BOUNDS_EXCEPTION = "java.lang.IndexOutOfBoundsException";

    public static final String INTERRUPTED_EXCEPTION = "java.lang.InterruptedException";

    public static final String NEGATIVE_ARRAY_SIZE_EXCEPTION = "java.lang.NegativeArraySizeException";

    public static final String NULL_POINTER_EXCEPTION = "java.lang.NullPointerException";

    public static final String OUT_OF_MEMORY_ERROR = "java.lang.OutOfMemoryError";

    public static final String RUNTIME_EXCEPTION = "java.lang.RuntimeException";

    private ExceptionNames() {
    }
}
