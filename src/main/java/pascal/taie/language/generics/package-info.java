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

/**
 * In this package, we offer a generics model for Java.
 * It is not an invasive change to the type system, but rather an additional attribute which
 * offers information about the generics starting from Java 5,
 * as in <a href="https://docs.oracle.com/javase/specs/jvms/se20/html/jvms-4.html#jvms-4.7.9.1">
 * JVM Spec. 4.7.9.1 Signatures</a>.
 * Notes that our implementation will not follow the JVM Spec. strictly, and will
 * be altered slightly for convenience, e.g.,
 * <ul>
 *     <li>We name <i>*Signature</i> as <i>*GSignature</i> (<i>G</i> means <i>Generics</i>).</li>
 *     <li>We name <i>JavaTypeSignature</i> as <i>TypeGSignature</i>.</li>
 *     <li>
 *         We make <i>{@link pascal.taie.language.generics.VoidDescriptor}</i>
 *         a <i>{@link pascal.taie.language.generics.TypeGSignature}</i>.
 *     </li>
 * </ul>
 */
@Experimental
package pascal.taie.language.generics;

import pascal.taie.util.Experimental;
