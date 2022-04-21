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

package pascal.taie.analysis.pta.client;

import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;

public class PolymorphicCallSite extends Collector {

    public static final String ID = "poly-call";

    public PolymorphicCallSite(AnalysisConfig config) {
        super(config);
    }

    @Override
    boolean isRelevant(Stmt stmt) {
        return stmt instanceof Invoke invoke &&
                (invoke.isVirtual() || invoke.isInterface());
    }

    @Override
    boolean isWanted(Stmt stmt, PointerAnalysisResult result) {
        Invoke invoke = (Invoke) stmt;
        return result.getCallGraph().getCalleesOf(invoke).size() > 1;
    }

    @Override
    String getDescription() {
        return ID;
    }
}
