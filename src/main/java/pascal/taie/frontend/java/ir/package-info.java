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
 * This package provides the core IR building functionality for the Java bytecode frontend.
 *
 * <p>Key components:
 * <ul>
 *   <li>{@link pascal.taie.frontend.java.ir.BytecodeIRBuilder}:
 *   Converts JVM bytecode to Tai-e 3-address code IR</li>
 *   <li>{@link pascal.taie.frontend.java.ir.DefaultIRBuilder}:
 *   Implements {@link pascal.taie.ir.IRBuilder}</li>
 *   <li>{@link pascal.taie.frontend.java.ir.BytecodeCFG} /
 *   {@link pascal.taie.frontend.java.ir.BytecodeBlock}:
 *   Control flow graph representation</li>
 *   <li>{@link pascal.taie.frontend.java.ir.VarManager}:
 *   IR variable creation and naming</li>
 * </ul>
 *
 * <p>Sub-packages:
 * <ul>
 *   <li>{@code ir.ssa} - SSA transformation and dominator analysis</li>
 *   <li>{@code ir.type} - Type inference and cast insertion</li>
 * </ul>
 *
 * @see pascal.taie.frontend.java.ir.ssa
 * @see pascal.taie.frontend.java.ir.typing
 */
package pascal.taie.frontend.java.ir;
