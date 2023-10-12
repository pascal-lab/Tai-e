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

package pascal.taie.analysis.sideeffect;

import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.config.AnalysisConfig;

public class SideEffectAnalysis extends ProgramAnalysis<SideEffect> {

    public static final String ID = "side-effect";

    /**
     * Whether the analysis only tracks the modifications on the objects
     * created in application code.
     */
    private final boolean onlyApp;

    public SideEffectAnalysis(AnalysisConfig config) {
        super(config);
        onlyApp = getOptions().getBoolean("only-app");
    }

    @Override
    public SideEffect analyze() {
        PointerAnalysisResult pta = World.get().getResult(PointerAnalysis.ID);
        return new TopologicalSolver(onlyApp).solve(pta);
    }
}
