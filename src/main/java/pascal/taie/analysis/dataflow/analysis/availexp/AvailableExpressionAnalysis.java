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

package pascal.taie.analysis.dataflow.analysis.availexp;

import pascal.taie.analysis.dataflow.analysis.AbstractDataflowAnalysis;
import pascal.taie.analysis.dataflow.fact.SetFact;
import pascal.taie.analysis.dataflow.fact.ToppedSetFact;
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

/**
 * Available expression analysis on local variables.
 * In Tai-e IR, Exp.equals(Object) test equality by object identity,
 * which cannot satisfy the requirement of available expression analysis,
 * thus we create ExpWrapper, which contains Exp and tests equality
 * (and computes hashcode) based on the content of the relevant Exps.
 * @see ExpWrapper
 */
public class AvailableExpressionAnalysis extends
        AbstractDataflowAnalysis<Stmt, SetFact<ExpWrapper>> {

    public static final String ID = "availexp";

    public AvailableExpressionAnalysis(AnalysisConfig config) {
        super(config);
    }

    @Override
    public boolean isForward() {
        return true;
    }

    @Override
    public SetFact<ExpWrapper> getEntryInitialFact(CFG<Stmt> cfg) {
        return new ToppedSetFact<>(false);
    }

    @Override
    public SetFact<ExpWrapper> newInitialFact() {
        return new ToppedSetFact<>(true);
    }

    @Override
    public SetFact<ExpWrapper> copyFact(SetFact<ExpWrapper> fact) {
        return fact.duplicate();
    }

    @Override
    public void mergeInto(SetFact<ExpWrapper> fact, SetFact<ExpWrapper> result) {
        result.intersect(fact);
    }

    @Override
    public boolean transferNode(Stmt stmt, SetFact<ExpWrapper> in, SetFact<ExpWrapper> out) {
        if (((ToppedSetFact<ExpWrapper>) in).isTop()) {
            // valid data facts have not arrived yet, just skip and return
            // true to ensure that the successors the will be analyzed later
            return true;
        }
        SetFact<ExpWrapper> oldOut = out.duplicate();
        out.set(in);
        if (stmt instanceof DefinitionStmt) {
            Exp lvalue = ((DefinitionStmt<?, ?>) stmt).getLValue();
            if (lvalue instanceof Var) {
                Var defVar = (Var) lvalue;
                // kill affected expressions
                out.removeIf(expWrapper ->
                        expWrapper.get().getUses().contains(defVar));
            }
            Exp rvalue = ((DefinitionStmt<?, ?>) stmt).getRValue();
            if (isRelevant(rvalue)) {
                // generate available expressions
                out.add(new ExpWrapper(rvalue));
            }
        }
        return !out.equals(oldOut);
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
