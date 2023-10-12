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
 * This package implements modification side-effect analysis (MOD) which
 * computes the objects that may be modified by each method and statement.
 * <p>
 * The analysis was defined in paper:
 * Ana Milanova, Atanas Rountev, and Barbara G. Ryder.
 * Parameterized Object Sensitivity for Points-to Analysis for Java.
 * In TOSEM 2005.
 * <p>
 * However, the algorithm described in the paper is very inefficient.
 * Therefore, we have designed and implemented a new and efficient
 * algorithm to compute modification information.
 */
package pascal.taie.analysis.sideeffect;
