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
 * The {@code pascal.taie.vm} package provides the core classes for the Tai-e virtual machine.
 * <p>
 * This package includes:
 * <ul>
 *   <li>{@link pascal.taie.vm.VM}: The main virtual machine for executing intermediate representations (IR) of Java programs.</li>
 *   <li>{@link pascal.taie.vm.JValue}, {@link pascal.taie.vm.JObject}, {@link pascal.taie.vm.JPrimitive}, {@link pascal.taie.vm.JArray}:
 *   Abstractions for representing Java values and objects in the VM.</li>
 * </ul>
 * <p>
 * The VM supports execution of Java IR, method invocation, field access, and exception handling,
 * with special handling for native and JVM classes.
 */
package pascal.taie.vm;
