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
 * This package contains classes for building and managing class hierarchies in Tai-e.
 * <p>
 * The main components are:
 * <ul>
 *   <li>{@link pascal.taie.frontend.newfrontend.hierarchy.ClassHierarchyBuilder} - Interface for building class hierarchies</li>
 *   <li>{@link pascal.taie.frontend.newfrontend.hierarchy.DefaultCHBuilder} - Default implementation for building class hierarchies</li>
 *   <li>{@link pascal.taie.frontend.newfrontend.hierarchy.BytecodeClassBuilder} - Builds class information from ASM bytecode</li>
 *   <li>{@link pascal.taie.frontend.newfrontend.hierarchy.DefaultClassLoader} - Handles class loading and phantom classes</li>
 * </ul>
 */
package pascal.taie.frontend.newfrontend.hierarchy;
