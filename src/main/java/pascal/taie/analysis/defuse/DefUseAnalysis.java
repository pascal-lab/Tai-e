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

package pascal.taie.analysis.defuse;

import pascal.taie.analysis.MethodAnalysis;
import pascal.taie.analysis.dataflow.analysis.ReachingDefinition;
import pascal.taie.analysis.dataflow.fact.DataflowResult;
import pascal.taie.analysis.dataflow.fact.SetFact;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.IndexMap;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.TwoKeyMultiMap;

/**
 * Computes intra-procedural def-use and use-def chains
 * based on reaching definition analysis.
 */
public class DefUseAnalysis extends MethodAnalysis<DefUse> {

    public static final String ID = "def-use";

    /**
     * Whether compute definitions, i.e., use-def chains.
     */
    private final boolean computeDefs;

    /**
     * Whether compute uses, i.e., def-use chains.
     */
    private final boolean computeUses;

    public DefUseAnalysis(AnalysisConfig config) {
        super(config);
        computeDefs = getOptions().getBoolean("compute-defs");
        computeUses = getOptions().getBoolean("compute-uses");
    }

    @Override
    public DefUse analyze(IR ir) {
        DataflowResult<Stmt, SetFact<Stmt>> rdResult = ir.getResult(ReachingDefinition.ID);
        TwoKeyMultiMap<Stmt, Var, Stmt> defs = computeDefs ?
                Maps.newTwoKeyMultiMap(new IndexMap<>(ir, ir.getStmts().size()),
                        () -> Maps.newMultiMap(Maps.newHybridMap()))
                : null;
        MultiMap<Stmt, Stmt> uses = computeUses ?
                Maps.newMultiMap(new IndexMap<>(ir, ir.getStmts().size()),
                        Sets::newHybridSet)
                : null;
        for (Stmt stmt : ir) {
            SetFact<Stmt> reachDefs = rdResult.getInFact(stmt);
            for (RValue use : stmt.getUses()) {
                if (use instanceof Var useVar) {
                    for (Stmt reachDef : reachDefs) {
                        reachDef.getDef().ifPresent(lhs -> {
                            if (lhs.equals(use)) {
                                if (computeDefs) {
                                    defs.put(stmt, useVar, reachDef);
                                }
                                if (computeUses) {
                                    uses.put(reachDef, stmt);
                                }
                            }
                        });
                    }
                }
            }
        }
        return new DefUse(defs, uses);
    }
}
