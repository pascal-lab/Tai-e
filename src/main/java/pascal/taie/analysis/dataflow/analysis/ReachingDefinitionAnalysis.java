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

import pascal.taie.analysis.dataflow.fact.SetFact;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;

public class ReachingDefinitionAnalysis extends
        AbstractDataflowAnalysis<Stmt, SetFact<Stmt>> {

    public ReachingDefinitionAnalysis(AnalysisConfig config) {
        super(config);
    }

    @Override
    public boolean isForward() {
        return true;
    }

    @Override
    public SetFact<Stmt> getEntryInitialFact(CFG<Stmt> cfg) {
        return new SetFact<>();
    }

    @Override
    public SetFact<Stmt> newInitialFact() {
        return new SetFact<>();
    }

    @Override
    public SetFact<Stmt> copyFact(SetFact<Stmt> fact) {
        return fact.duplicate();
    }

    @Override
    public void mergeInto(SetFact<Stmt> fact, SetFact<Stmt> result) {
        result.union(fact);
    }

    @Override
    public boolean transferNode(Stmt stmt, SetFact<Stmt> in, SetFact<Stmt> out) {
        SetFact<Stmt> oldOut = out.duplicate();
        out.set(in);
        stmt.getDef().ifPresent(def -> {
            if (def instanceof Var) {
                Var defVar = (Var) def;
                // kill previous definitions of defVar
                out.removeIf(s -> s.getDef().stream().anyMatch(defVar::equals));
                // generate definition of defVar
                out.add(stmt);
            }
        });
        return !out.equals(oldOut);
    }
}
