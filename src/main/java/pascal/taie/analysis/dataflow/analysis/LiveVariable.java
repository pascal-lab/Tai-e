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
import pascal.taie.ir.LocalVarMapper;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.ObjectIdMapper;
import pascal.taie.util.collection.MapperBitSet;

/**
 * Implementation of live variable analysis.
 */
public class LiveVariable extends AnalysisDriver<Stmt, SetFact<Var>> {

    public static final String ID = "livevar";

    public LiveVariable(AnalysisConfig config) {
        super(config);
    }

    @Override
    protected Analysis makeAnalysis(CFG<Stmt> cfg) {
        return new Analysis(cfg, getOptions().getBoolean("strongly"));
    }

    private static class Analysis extends AbstractDataflowAnalysis<Stmt, SetFact<Var>> {

        /**
         * Whether enable strongly live variable analysis.
         */
        private final boolean strongly;

        /**
         * Mapper for variables in the IR.
         */
        private final ObjectIdMapper<Var> varMapper;

        private Analysis(CFG<Stmt> cfg, boolean strongly) {
            super(cfg);
            this.strongly = strongly;
            this.varMapper = new LocalVarMapper(cfg.getIR());
        }

        @Override
        public boolean isForward() {
            return false;
        }

        @Override
        public SetFact<Var> newBoundaryFact() {
            return newInitialFact();
        }

        @Override
        public SetFact<Var> newInitialFact() {
            return new SetFact<>(new MapperBitSet<>(varMapper));
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
}
