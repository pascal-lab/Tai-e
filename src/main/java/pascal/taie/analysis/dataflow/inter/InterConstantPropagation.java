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

package pascal.taie.analysis.dataflow.inter;

import pascal.taie.World;
import pascal.taie.analysis.dataflow.analysis.constprop.CPFact;
import pascal.taie.analysis.dataflow.analysis.constprop.ConstantPropagation;
import pascal.taie.analysis.dataflow.analysis.constprop.Value;
import pascal.taie.analysis.graph.icfg.CallEdge;
import pascal.taie.analysis.graph.icfg.LocalEdge;
import pascal.taie.analysis.graph.icfg.ReturnEdge;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class InterConstantPropagation extends
        AbstractInterDataflowAnalysis<JMethod, Stmt, CPFact> {

    public static final String ID = "inter-constprop";

    private final ConstantPropagation cp;

    private final boolean aliasAware;

    /**
     * Map from store statements to the corresponding load statements, where
     * the base variables of both store and load statements may be aliases,
     * e.g., [a.f = b;] -> [x = y.f;], where a and y are aliases.
     */
    private Map<StoreField, Set<LoadField>> storeToLoads;

    public InterConstantPropagation(AnalysisConfig config) {
        super(config);
        cp = new ConstantPropagation(new AnalysisConfig(ConstantPropagation.ID));
        aliasAware = getOptions().getBoolean("alias-aware");
        if (aliasAware) {
            storeToLoads = computeStoreToLoads();
        }
    }

    private Map<StoreField, Set<LoadField>> computeStoreToLoads() {
        // compute storeToLoads via alias information
        // derived from pointer analysis
        String ptaId = getOptions().getString("pta");
        PointerAnalysisResult pta = World.getResult(ptaId);
        Map<Obj, Set<Var>> pointedBy = Maps.newMap();
        pta.vars().forEach(var ->
                pta.getPointsToSet(var).forEach(obj ->
                        Maps.addToMapSet(pointedBy, obj, var)));
        Map<StoreField, Set<LoadField>> storeToLoads = Maps.newMap();
        pointedBy.values().forEach(aliases -> {
            for (Var v : aliases) {
                v.getStoreFields()
                        .stream()
                        .filter(this::isAliasRelevant)
                        .forEach(store -> {
                            JField storedField = store.getFieldRef().resolve();
                            aliases.forEach(u ->
                                    u.getLoadFields().forEach(load -> {
                                        JField loadedField = load
                                                .getFieldRef().resolve();
                                        if (storedField.equals(loadedField)) {
                                            Maps.addToMapSet(storeToLoads, store, load);
                                        }
                                    })
                            );
                        });
            }
        });
        return storeToLoads;
    }

    @Override
    public boolean isForward() {
        return cp.isForward();
    }

    @Override
    public CPFact newBoundaryFact(Stmt boundary) {
        IR ir = icfg.getContainingMethodOf(boundary).getIR();
        return cp.newBoundaryFact(ir);
    }

    @Override
    public CPFact newInitialFact() {
        return cp.newInitialFact();
    }

    @Override
    public void meetInto(CPFact fact, CPFact target) {
        cp.meetInto(fact, target);
    }

    @Override
    protected boolean transferCall(Stmt stmt, CPFact in, CPFact out) {
        Invoke call = (Invoke) stmt;
        boolean changed = false;
        Var lhs = call.getResult();
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
    protected boolean transferNonCall(Stmt stmt, CPFact in, CPFact out) {
        return aliasAware ?
                transferAliasAware(stmt, in, out) :
                cp.transferNode(stmt, in, out);
    }

    private boolean transferAliasAware(
            Stmt stmt, CPFact in, CPFact out) {
        if (isAliasRelevant(stmt)) {
            if (stmt instanceof LoadField) { // x = o.f
                LoadField load = (LoadField) stmt;
                boolean changed = false;
                Var lhs = load.getLValue();
                // kill x
                for (Var inVar : in.keySet()) {
                    if (!inVar.equals(lhs)) {
                        changed |= out.update(inVar, in.get(inVar));
                    }
                }
                return changed;
            } else { // o.f = x
                StoreField store = (StoreField) stmt;
                Var var = store.getRValue();
                Value value = in.get(var);
                storeToLoads.get(store).forEach(load -> {
                    // propagate stored value to aliased loads
                    Var lhs = load.getLValue();
                    CPFact loadOut = solver.getOutFact(load);
                    Value oldV = loadOut.get(lhs);
                    Value newV = cp.meetValue(oldV, value);
                    if (loadOut.update(lhs, newV)) {
                        solver.propagate(load);
                    }
                });
                return cp.transferNode(stmt, in, out);
            }
        } else {
            return cp.transferNode(stmt, in, out);
        }
    }

    private boolean isAliasRelevant(Stmt stmt) {
        if (stmt instanceof LoadField) {
            LoadField load = (LoadField) stmt;
            return !load.isStatic() &&
                    ConstantPropagation.canHoldInt(load.getLValue());
        } else if (stmt instanceof StoreField) {
            StoreField store = (StoreField) stmt;
            return !store.isStatic() &&
                    ConstantPropagation.canHoldInt(store.getRValue());
        }
        return false;
    }

    @Override
    public void transferLocalEdge(LocalEdge<Stmt> edge, CPFact out,
                                  CPFact edgeFact) {
        cp.transferEdge(edge.getCFGEdge(), out, edgeFact);
    }

    @Override
    public void transferCallEdge(CallEdge<Stmt> edge, CPFact callSiteIn,
                                 CPFact edgeFact) {
        // Passing arguments at call site to parameters of the callee
        InvokeExp invokeExp = ((Invoke) edge.getSource()).getInvokeExp();
        Stmt entry = edge.getTarget();
        JMethod callee = icfg.getContainingMethodOf(entry);
        List<Var> args = invokeExp.getArgs();
        List<Var> params = callee.getIR().getParams();
        for (int i = 0; i < args.size(); ++i) {
            Var arg = args.get(i);
            Var param = params.get(i);
            if (ConstantPropagation.canHoldInt(param)) {
                Value argValue = callSiteIn.get(arg);
                edgeFact.update(param, argValue);
            }
        }
    }

    @Override
    public void transferReturnEdge(ReturnEdge<Stmt> edge, CPFact returnOut,
                                   CPFact edgeFact) {
        // Passing return value to the LHS of the call statement
        Var lhs = ((Invoke) edge.getCallSite()).getResult();
        if (lhs != null && ConstantPropagation.canHoldInt(lhs)) {
            Value retValue = edge.returnVars()
                    .map(returnOut::get)
                    .reduce(Value.getUndef(), cp::meetValue);
            edgeFact.update(lhs, retValue);
        }
    }
}
