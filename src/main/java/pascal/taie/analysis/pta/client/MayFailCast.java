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

import pascal.taie.World;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.type.Type;

public class MayFailCast extends Collector {

    public static final String ID = "may-fail-cast";

    public MayFailCast(AnalysisConfig config) {
        super(config);
    }

    @Override
    boolean isRelevant(Stmt stmt) {
        return stmt instanceof Cast;
    }

    @Override
    boolean isWanted(Stmt stmt, PointerAnalysisResult result) {
        Cast cast = (Cast) stmt;
        Type castType = cast.getRValue().getCastType();
        Var from = cast.getRValue().getValue();
        for (Obj obj : result.getPointsToSet(from)) {
            if (!World.get().getTypeSystem().isSubtype(
                    castType, obj.getType())) {
                return true;
            }
        }
        return false;
    }

    @Override
    String getDescription() {
        return ID;
    }
}
