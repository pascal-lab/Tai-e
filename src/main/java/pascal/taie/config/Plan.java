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

package pascal.taie.config;

import pascal.taie.util.graph.Graph;
import pascal.taie.util.graph.SimpleGraph;

import java.util.List;
import java.util.Set;

public record Plan(
        // List of analyses to be executed.
        List<AnalysisConfig> analyses,

        // Graph that describes dependence among analyses.
        Graph<AnalysisConfig> dependenceGraph,

        // Set of analyses whose results are kept.
        Set<String> keptAnalyses) {

    /**
     * Special element for {@link #keptAnalyses}, which means
     * to keep results of all analyses.
     */
    public static final String KEEP_ALL = "$KEEP-ALL";

    private static final Plan EMPTY = new Plan(List.of(), new SimpleGraph<>(), Set.of());

    /**
     * @return an empty plan.
     */
    public static Plan emptyPlan() {
        return EMPTY;
    }
}
