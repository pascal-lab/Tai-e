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

package pascal.taie.analysis.dataflow.analysis.availexp;

import pascal.taie.analysis.dataflow.analysis.AbstractDataflowAnalysis;
import pascal.taie.analysis.dataflow.analysis.AnalysisDriver;
import pascal.taie.analysis.dataflow.fact.SetFact;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.InstanceOfExp;
import pascal.taie.ir.exp.UnaryExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.Indexer;
import pascal.taie.util.SimpleIndexer;
import pascal.taie.util.collection.IndexerBitSet;

/**
 * Available expression analysis on local variables.
 * In Tai-e IR, Exp.equals(Object) test equality by object identity,
 * which cannot satisfy the requirement of available expression analysis,
 * thus we create ExpWrapper, which contains Exp and tests equality
 * (and computes hashcode) based on the content of the relevant Exps.
 *
 * @see ExpWrapper
 */
public class AvailableExpression extends AnalysisDriver<Stmt, SetFact<ExpWrapper>> {

    public static final String ID = "avail-exp";

    public AvailableExpression(AnalysisConfig config) {
        super(config);
    }

    @Override
    protected Analysis makeAnalysis(CFG<Stmt> cfg) {
        return new Analysis(cfg);
    }

    private static class Analysis extends AbstractDataflowAnalysis<Stmt, SetFact<ExpWrapper>> {

        private final Indexer<ExpWrapper> expIndexer;

        /**
         * Set of all expressions in cfg.
         * Used to fast create universal set (via copy).
         */
        private final SetFact<ExpWrapper> universalSet;

        private Analysis(CFG<Stmt> cfg) {
            super(cfg);
            expIndexer = new SimpleIndexer<>();
            universalSet = computeUniversalSet(cfg, expIndexer);
        }

        @Override
        public boolean isForward() {
            return true;
        }

        @Override
        public SetFact<ExpWrapper> newBoundaryFact() {
            return new SetFact<>(new IndexerBitSet<>(expIndexer, false));
        }

        @Override
        public SetFact<ExpWrapper> newInitialFact() {
            return universalSet.copy();
        }

        @Override
        public void meetInto(SetFact<ExpWrapper> fact, SetFact<ExpWrapper> target) {
            target.intersect(fact);
        }

        @Override
        public boolean transferNode(Stmt stmt, SetFact<ExpWrapper> in, SetFact<ExpWrapper> out) {
            SetFact<ExpWrapper> oldOut = out.copy();
            out.set(in);
            if (stmt instanceof DefinitionStmt<?, ?> defStmt) {
                Exp lvalue = defStmt.getLValue();
                if (lvalue instanceof Var defVar) {
                    // kill affected expressions
                    out.removeIf(expWrapper ->
                            expWrapper.get().getUses().contains(defVar));
                }
                Exp rvalue = defStmt.getRValue();
                if (isRelevant(rvalue)) {
                    // generate available expressions
                    out.add(new ExpWrapper(rvalue));
                }
            }
            return !out.equals(oldOut);
        }

        /**
         * @return a set containing all (relevant) expressions in {@code cfg}.
         */
        private static SetFact<ExpWrapper> computeUniversalSet(
                CFG<Stmt> cfg, Indexer<ExpWrapper> expIndexer) {
            SetFact<ExpWrapper> set = new SetFact<>(
                    new IndexerBitSet<>(expIndexer, false));
            cfg.forEach(stmt -> {
                if (stmt instanceof DefinitionStmt<?, ?> defStmt
                        && isRelevant(defStmt.getRValue())) {
                    set.add(new ExpWrapper(defStmt.getRValue()));
                }
            });
            return set;
        }

        /**
         * Checks if an expression is relevant to available expressions.
         * We only consider these expressions as available expressions.
         */
        private static boolean isRelevant(Exp exp) {
            return exp instanceof Var ||
                    exp instanceof BinaryExp ||
                    exp instanceof CastExp ||
                    exp instanceof InstanceOfExp ||
                    exp instanceof UnaryExp;
        }
    }
}
