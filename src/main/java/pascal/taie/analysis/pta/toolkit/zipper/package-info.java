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
 * This package contains implementation of Zipper and Zipper-e, which selects
 * precision-critical methods in the program.
 * <p>
 * The techniques were presented in papers:
 * (1) Yue Li, Tian Tan, Anders Møller, and Yannis Smaragdakis.
 * Precision-Guided Context Sensitivity for Pointer Analysis.
 * In OOPSLA 2018, and
 * <p>
 * (2) Yue Li, Tian Tan, Anders Møller, and Yannis Smaragdakis.
 * A Principled Approach to Selective Context Sensitivity for Pointer Analysis.
 * In TOPLAS 2020.
 */
package pascal.taie.analysis.pta.toolkit.zipper;
