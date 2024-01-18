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

package pascal.taie.analysis.pta.plugin.assertion;

import pascal.taie.ir.stmt.Invoke;

import java.util.Map;

/**
 * Represents result of {@link Checker}.
 *
 * @param invoke    the checked assertion.
 * @param assertion the information of the assertion.
 * @param failures  the information about failures, which map a program element
 *                  to its relevant analysis result. If this map is empty,
 *                  then it means that the assertion is passed.
 */
record Result(Invoke invoke, String assertion, Map<?, ?> failures) {
}
