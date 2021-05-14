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

package pascal.taie.analysis.dfa.analysis;

import pascal.taie.analysis.dfa.fact.SetFact;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;

public class LiveVariableAnalysis extends
        AbstractDataflowAnalysis<Stmt, SetFact<Var>> {

    public LiveVariableAnalysis(AnalysisConfig config) {
        super(config);
    }

    @Override
    public boolean isForward() {
        return false;
    }

    @Override
    public SetFact<Var> getEntryInitialFact(CFG<Stmt> cfg) {
        return new SetFact<>();
    }

    @Override
    public SetFact<Var> newInitialFact() {
        return new SetFact<>();
    }

    @Override
    public SetFact<Var> copyFact(SetFact<Var> fact) {
        return fact.duplicate();
    }

    @Override
    public void mergeInto(SetFact<Var> fact, SetFact<Var> result) {
        result.union(fact);
    }

    @Override
    public boolean transferNode(Stmt stmt, SetFact<Var> in, SetFact<Var> out) {
        SetFact<Var> oldIn = in.duplicate();
        in.setTo(out);
        // kill definition in stmt
        stmt.getDef().ifPresent(def -> {
            if (def instanceof Var) {
                in.remove((Var) def);
            }
        });
        // gen uses in stmt
        stmt.getUses().forEach(use -> {
            if (use instanceof Var) {
                in.add((Var) use);
            }
        });
        return !in.equals(oldIn);
    }
}
