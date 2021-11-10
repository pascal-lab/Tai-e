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
import pascal.taie.analysis.graph.icfg.CallToReturnEdge;
import pascal.taie.analysis.graph.icfg.ICFG;
import pascal.taie.analysis.graph.icfg.ICFGBuilder;
import pascal.taie.analysis.graph.icfg.NormalEdge;
import pascal.taie.analysis.graph.icfg.ReturnEdge;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Implementation of interprocedural constant propagation for int values.
 */
public class InterConstantPropagation extends
        AbstractInterDataflowAnalysis<JMethod, Stmt, CPFact> {

    public static final String ID = "inter-constprop";

    private final ConstantPropagation cp;

    /**
     * Whether the constant propagation use control-flow edge information
     * to refine analysis results.
     */
    private final boolean edgeRefine;

    /**
     * Whether the constant propagation takes alias information into account.
     * If this field is true, it would leverage pointer analysis results to
     * handle instance fields, static fields and arrays more precisely.
     */
    private final boolean aliasAware;

    /**
     * Map from store statements to the corresponding load statements,
     * including both static and instance field stores and loads.
     * For static fields, if the store and load statements operate on
     * the same field, e.g., T.f = x; ... y = T.f;, then they should
     * be recorded in this map.
     * For instance fields, if the base variables of both store and
     * load statements may be aliases, e.g., [a.f = b;] -> [x = y.f;],
     * where a and y are aliases, then they should be recorded in this map.
     */
    private Map<StoreField, Set<LoadField>> fieldStoreToLoads;

    public InterConstantPropagation(AnalysisConfig config) {
        super(config);
        cp = new ConstantPropagation(new AnalysisConfig(ConstantPropagation.ID));
        edgeRefine = getOptions().getBoolean("edge-refine");
        aliasAware = getOptions().getBoolean("alias-aware");
        if (aliasAware) {
            initializeAliases();
        }
    }

    private void initializeAliases() {
        fieldStoreToLoads = Maps.newMap();
        // collect related static field stores and loads
        Map<JField, Set<StoreField>> staticStores = Maps.newMap();
        Map<JField, Set<LoadField>> staticLoads = Maps.newMap();
        ICFG<JMethod, Stmt> icfg = World.getResult(ICFGBuilder.ID);
        for (Stmt s : icfg) {
            if (s instanceof StoreField) {
                StoreField store = (StoreField) s;
                if (store.isStatic()) {
                    Maps.addToMapSet(staticStores,
                            store.getFieldRef().resolve(), store);
                }
            }
            if (s instanceof LoadField) {
                LoadField load = (LoadField) s;
                if (load.isStatic()) {
                    Maps.addToMapSet(staticLoads,
                            load.getFieldRef().resolve(), load);
                }
            }
        }
        staticStores.forEach((field, stores) -> {
            for (StoreField store : stores) {
                for (LoadField load : staticLoads.getOrDefault(field, Set.of())) {
                    Maps.addToMapSet(fieldStoreToLoads, store, load);
                }
            }
        });
        // collect related instance field stores and loads
        // via alias information derived from pointer analysis
        String ptaId = getOptions().getString("pta");
        PointerAnalysisResult pta = World.getResult(ptaId);
        Map<Obj, Set<Var>> pointedBy = Maps.newMap();
        pta.vars().forEach(var ->
                pta.getPointsToSet(var).forEach(obj ->
                        Maps.addToMapSet(pointedBy, obj, var)));
        pointedBy.values().forEach(aliases -> {
            for (Var v : aliases) {
                v.getStoreFields()
                        .stream()
                        .filter(Predicate.not(StoreField::isStatic))
                        .forEach(store -> {
                            JField storedField = store.getFieldRef().resolve();
                            aliases.forEach(u ->
                                    u.getLoadFields().forEach(load -> {
                                        JField loadedField = load
                                                .getFieldRef().resolve();
                                        if (storedField.equals(loadedField)) {
                                            Maps.addToMapSet(fieldStoreToLoads, store, load);
                                        }
                                    })
                            );
                        });
            }
        });
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
    protected boolean transferCallNode(Stmt stmt, CPFact in, CPFact out) {
        return out.copyFrom(in);
    }

    @Override
    protected boolean transferNonCallNode(Stmt stmt, CPFact in, CPFact out) {
        return aliasAware ?
                transferAliasAware(stmt, in, out) :
                cp.transferNode(stmt, in, out);
    }


    private boolean transferAliasAware(Stmt stmt, CPFact in, CPFact out) {
        return stmt.accept(new StmtVisitor<>() {

            @Override
            public Boolean visit(LoadArray load) {
                return StmtVisitor.super.visit(load);
            }

            @Override
            public Boolean visit(StoreArray store) {
                return StmtVisitor.super.visit(store);
            }

            @Override
            public Boolean visit(LoadField load) {
                boolean changed = false;
                Var lhs = load.getLValue();
                // kill x
                for (Var inVar : in.keySet()) {
                    if (!inVar.equals(lhs)) {
                        changed |= out.update(inVar, in.get(inVar));
                    }
                }
                return changed;
            }

            @Override
            public Boolean visit(StoreField store) {
                Var var = store.getRValue();
                Value value = in.get(var);
                fieldStoreToLoads.get(store).forEach(load -> {
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

            @Override
            public Boolean visitDefault(Stmt stmt) {
                return cp.transferNode(stmt, in, out);
            }
        });
    }

    @Override
    protected CPFact transferNormalEdge(NormalEdge<Stmt> edge, CPFact out) {
        // Just apply edge transfer of intraprocedural constant propagation
        return edgeRefine ? cp.transferEdge(edge.getCFGEdge(), out) : out;
    }

    @Override
    protected CPFact transferCallToReturnEdge(CallToReturnEdge<Stmt> edge, CPFact out) {
        // Kill the value of LHS variable
        Invoke invoke = (Invoke) edge.getSource();
        Var lhs = invoke.getResult();
        if (lhs != null) {
            CPFact result = out.copy();
            result.remove(lhs);
            return result;
        } else {
            return out;
        }
    }

    @Override
    protected CPFact transferCallEdge(CallEdge<Stmt> edge, CPFact callSiteOut) {
        // Passing arguments at call site to parameters of the callee
        InvokeExp invokeExp = ((Invoke) edge.getSource()).getInvokeExp();
        JMethod callee = edge.getCallee();
        List<Var> args = invokeExp.getArgs();
        List<Var> params = callee.getIR().getParams();
        CPFact result = newInitialFact();
        for (int i = 0; i < args.size(); ++i) {
            Var arg = args.get(i);
            Var param = params.get(i);
            if (ConstantPropagation.canHoldInt(param)) {
                Value argValue = callSiteOut.get(arg);
                result.update(param, argValue);
            }
        }
        return result;
    }

    @Override
    protected CPFact transferReturnEdge(ReturnEdge<Stmt> edge, CPFact returnOut) {
        // Passing return value to the LHS of the call statement
        Var lhs = ((Invoke) edge.getCallSite()).getResult();
        CPFact result = newInitialFact();
        if (lhs != null && ConstantPropagation.canHoldInt(lhs)) {
            Value retValue = edge.returnVars()
                    .map(returnOut::get)
                    .reduce(Value.getUndef(), cp::meetValue);
            result.update(lhs, retValue);
        }
        return result;
    }
}
