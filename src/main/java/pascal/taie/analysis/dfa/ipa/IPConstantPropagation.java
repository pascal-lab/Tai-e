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

package pascal.taie.analysis.dfa.ipa;

import pascal.taie.analysis.dfa.analysis.constprop.ConstantPropagation;
import pascal.taie.analysis.dfa.analysis.constprop.Value;
import pascal.taie.analysis.dfa.fact.MapFact;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;

import java.util.List;

public class IPConstantPropagation extends
        AbstractIPDataflowAnalysis<JMethod, Stmt, MapFact<Var, Value>> {

    public static final String ID = "ip-constprop";

    private final ConstantPropagation cp;

    public IPConstantPropagation(AnalysisConfig config) {
        super(config);
        cp = new ConstantPropagation(new AnalysisConfig(ConstantPropagation.ID));
    }

    @Override
    public boolean isForward() {
        return cp.isForward();
    }

    @Override
    public MapFact<Var, Value> getEntryInitialFact(Stmt entry) {
        return cp.getEntryInitialFact(icfg.getContainingMethodOf(entry));
    }

    @Override
    public MapFact<Var, Value> newInitialFact() {
        return cp.newInitialFact();
    }

    @Override
    public MapFact<Var, Value> copyFact(MapFact<Var, Value> fact) {
        return cp.copyFact(fact);
    }

    @Override
    public void mergeInto(MapFact<Var, Value> fact, MapFact<Var, Value> result) {
        cp.mergeInto(fact, result);
    }

    @Override
    public boolean transferNonCall(Stmt stmt, MapFact<Var, Value> in, MapFact<Var, Value> out) {
        return cp.transferNode(stmt, in, out);
    }

    @Override
    public boolean transferCall(Stmt callSite, MapFact<Var, Value> in, MapFact<Var, Value> out) {
        boolean changed = false;
        Var lhs = ((Invoke) callSite).getResult();
        if (lhs != null) {
            for (Var inVar : in.keySet()) {
                if (!inVar.equals(lhs)) {
                    changed |= out.update(inVar, in.get(inVar));
                }
            }
            return changed;
        } else {
            return out.copyFrom(in);
        }
    }

    @Override
    public void transferLocalEdge(LocalEdge<Stmt> edge, MapFact<Var, Value> out, MapFact<Var, Value> edgeFact) {
        cp.transferEdge(edge.getCFGEdge(), out, edgeFact);
    }

    @Override
    public void transferCallEdge(CallEdge<Stmt> edge, MapFact<Var, Value> callSiteIn, MapFact<Var, Value> edgeFact) {
        // Passing arguments at call site to parameters of the callee
        InvokeExp invokeExp = ((Invoke) edge.getSource()).getInvokeExp();
        Stmt entry = edge.getTarget();
        JMethod callee = icfg.getContainingMethodOf(entry);
        List<Var> args = invokeExp.getArgs();
        List<Var> params = callee.getIR().getParams();
        for (int i = 0; i < args.size(); ++i) {
            Var arg = args.get(i);
            Var param = params.get(i);
            Value argValue = callSiteIn.get(arg);
            edgeFact.update(param, argValue);
        }
        // Set NAC to this variable of callee
        if (!callee.isStatic()) {
            edgeFact.update(callee.getIR().getThis(), Value.getNAC());
        }
    }

    @Override
    public void transferReturnEdge(ReturnEdge<Stmt> edge, MapFact<Var, Value> returnOut, MapFact<Var, Value> edgeFact) {
        // Passing return value to the LHS of the call statement
        Var lhs = ((Invoke) edge.getCallSite()).getResult();
        if (lhs != null) {
            Value retValue = edge.returnVars()
                    .map(returnOut::get)
                    .reduce(Value.getUndef(), cp::meetValue);
            edgeFact.update(lhs, retValue);
        }
    }
}
