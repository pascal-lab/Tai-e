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

package pascal.taie.analysis.exception;

import pascal.taie.analysis.MethodAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;

public class ThrowAnalysis extends MethodAnalysis<ThrowResult> {

    public static final String ID = "throw";

    /**
     * If this field is null, then this analysis ignores implicit exceptions.
     */
    private final ImplicitThrowAnalysis implicitThrowAnalysis;

    private final ExplicitThrowAnalysis explicitThrowAnalysis;

    public ThrowAnalysis(AnalysisConfig config) {
        super(config);
        if ("all".equals(getOptions().getString("exception"))) {
            implicitThrowAnalysis = new ImplicitThrowAnalysis();
        } else {
            implicitThrowAnalysis = null;
        }
        if ("pta".equals(getOptions().getString("algorithm"))) {
            explicitThrowAnalysis = new PTABasedExplicitThrowAnalysis();
        } else {
            explicitThrowAnalysis = new IntraExplicitThrowAnalysis();
        }
    }

    @Override
    public ThrowResult analyze(IR ir) {
        ThrowResult result = new ThrowResult(
                ir, implicitThrowAnalysis);
        explicitThrowAnalysis.analyze(ir, result);
        return result;
    }
}
