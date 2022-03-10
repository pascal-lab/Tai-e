/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.dataflow.analysis;

import pascal.taie.analysis.dataflow.fact.LocalVarSet;
import pascal.taie.analysis.dataflow.fact.SetFact;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.Stmt;

/**
 * Implementation of live variable analysis.
 */
public class LiveVariableAnalysis extends
        AbstractDataflowAnalysis<Stmt, SetFact<Var>> {

    public static final String ID = "livevar";

    /**
     * Whether enable strongly live variable analysis.
     */
    private final boolean strongly;

    public LiveVariableAnalysis(AnalysisConfig config) {
        super(config);
        strongly = getOptions().getBoolean("strongly");
    }

    @Override
    public boolean isForward() {
        return false;
    }

    @Override
    public SetFact<Var> newBoundaryFact(CFG<Stmt> cfg) {
        return new SetFact<>(new LocalVarSet(cfg.getIR()));
    }

    @Override
    public SetFact<Var> newInitialFact(CFG<Stmt> cfg) {
        return new SetFact<>(new LocalVarSet(cfg.getIR()));
    }

    @Override
    public void meetInto(SetFact<Var> fact, SetFact<Var> target) {
        target.union(fact);
    }

    @Override
    public boolean transferNode(Stmt stmt, SetFact<Var> in, SetFact<Var> out) {
        SetFact<Var> oldIn = in.copy();
        in.set(out);
        // kill definition in stmt
        stmt.getDef().ifPresent(def -> {
            if (def instanceof Var) {
                in.remove((Var) def);
            }
        });
        // generate uses in stmt
        if (strongly) {
            // only add strongly live variables
            if (stmt instanceof Copy copy) {
                // for a Copy statement, say x = y, we consider y as
                // strongly live only when x is also strongly live
                Var lVar = copy.getLValue();
                Var rVar = copy.getRValue();
                if (out.contains(lVar)) {
                    in.add(rVar);
                }
            } else {
                // for non-Copy statements, all used variables
                // are considered strongly live
                stmt.getUses().forEach(use -> {
                    if (use instanceof Var) {
                        in.add((Var) use);
                    }
                });
            }
        } else {
            // add all used variables
            stmt.getUses().forEach(use -> {
                if (use instanceof Var) {
                    in.add((Var) use);
                }
            });
        }
        return !in.equals(oldIn);
    }
}
