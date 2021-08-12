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

package pascal.taie.analysis.dataflow.ipa;

import pascal.taie.World;
import pascal.taie.analysis.dataflow.analysis.constprop.ConstantPropagation;
import pascal.taie.analysis.dataflow.analysis.constprop.Value;
import pascal.taie.analysis.dataflow.fact.MapFact;
import pascal.taie.analysis.graph.icfg.CallEdge;
import pascal.taie.analysis.graph.icfg.LocalEdge;
import pascal.taie.analysis.graph.icfg.ReturnEdge;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.FieldStmt;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.util.collection.MapUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class IPConstantPropagation extends
        AbstractIPDataflowAnalysis<JMethod, Stmt, MapFact<Var, Value>> {

    public static final String ID = "ip-constprop";

    private final ConstantPropagation cp;

    private final boolean aliasAware;

    /**
     * Map from store statements to the corresponding load statements,
     * where the base variables of both store and load statements are aliases,
     * e.g., [a.f = b;] -> [x = y.f;], where a and y are aliases.
     */
    private Map<StoreField, Set<LoadField>> storeToLoads;

    public IPConstantPropagation(AnalysisConfig config) {
        super(config);
        cp = new ConstantPropagation(new AnalysisConfig(ConstantPropagation.ID));
        aliasAware = getOptions().getBoolean("alias-aware");
        if (aliasAware) {
            preAnalysis();
        }
    }

    /**
     * Pre-analysis for alias-aware analysis.
     */
    private void preAnalysis() {
        String ptaId = getOptions().getString("pta");
        PointerAnalysisResult result = World.getResult(ptaId);
        // compute storeToLoads via alias information
        // derived from pointer analysis
        Map<Obj, Set<Var>> pointedBy = MapUtils.newMap();
        result.vars().forEach(var ->
                result.getPointsToSet(var).forEach(obj ->
                        MapUtils.addToMapSet(pointedBy, obj, var)));
        storeToLoads = MapUtils.newMap();
        pointedBy.values().forEach(aliases -> {
            for (Var v : aliases) {
                v.getStoreFields()
                        .stream()
                        .filter(IPConstantPropagation::isAliasRelevant)
                        .forEach(store -> {
                            JField storedField = store.getFieldRef().resolve();
                            aliases.forEach(u ->
                                u.getLoadFields().forEach(load -> {
                                    JField loadedField = load
                                            .getFieldRef().resolve();
                                    if (storedField.equals(loadedField)) {
                                        MapUtils.addToMapSet(storeToLoads, store, load);
                                    }
                                })
                            );
                        });
            }
        });
        // TODO: compute uninitialized fields
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
        return aliasAware ?
                transferNonCallAliasAware(stmt, in, out) :
                cp.transferNode(stmt, in, out);
    }

    private boolean transferNonCallAliasAware(
            Stmt stmt, MapFact<Var, Value> in, MapFact<Var, Value> out) {
        if (isAliasRelevant(stmt)) {
            if (stmt instanceof LoadField) { // x = o.f
                LoadField load = (LoadField) stmt;
                // if o has not been initialized, set x to 0
                // otherwise, kill x
            } else { // o.f = x
                StoreField store = (StoreField) stmt;
                Var var = store.getRValue();
                Value value = in.get(var);
                MapFact<Var, Value> temp = newInitialFact();
                temp.update(var, value);

                // propagate value of x to loads of o
                // put affected load statements to work-list
                // propagate in to out
            }
        }
        return cp.transferNode(stmt, in, out);
    }

    private static boolean isAliasRelevant(Stmt stmt) {
        if (stmt instanceof FieldStmt) {
            FieldStmt<?,?> fs = (FieldStmt<?, ?>) stmt;
            return !fs.isStatic() &&
                    fs.getRValue().getType().equals(PrimitiveType.INT);
        }
        return false;
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
