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
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.FieldStmt;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.MapUtils;
import pascal.taie.util.collection.SetQueue;
import pascal.taie.util.collection.SetUtils;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

public class IPConstantPropagation extends
        AbstractIPDataflowAnalysis<JMethod, Stmt, MapFact<Var, Value>> {

    public static final String ID = "ip-constprop";

    private final ConstantPropagation cp;

    private final boolean aliasAware;

    private PointerAnalysisResult pta;

    /**
     * Map from store statements to the corresponding load statements, where
     * the base variables of both store and load statements may be aliases,
     * e.g., [a.f = b;] -> [x = y.f;], where a and y are aliases.
     */
    private Map<StoreField, Set<LoadField>> storeToLoads;

    /**
     * Map from objects to set of its fields which have not been initialized
     * in constructors.
     */
    private Map<Obj, Set<JField>> uninitializedFields;

    public IPConstantPropagation(AnalysisConfig config) {
        super(config);
        cp = new ConstantPropagation(new AnalysisConfig(ConstantPropagation.ID));
        aliasAware = getOptions().getBoolean("alias-aware");
        if (aliasAware) {
            String ptaId = getOptions().getString("pta");
            pta = World.getResult(ptaId);
            storeToLoads = computeStoreToLoads();
            uninitializedFields = computeUninitializedFields();
        }
    }

    private Map<StoreField, Set<LoadField>> computeStoreToLoads() {
        // compute storeToLoads via alias information
        // derived from pointer analysis
        Map<Obj, Set<Var>> pointedBy = MapUtils.newMap();
        pta.vars().forEach(var ->
                pta.getPointsToSet(var).forEach(obj ->
                        MapUtils.addToMapSet(pointedBy, obj, var)));
        Map<StoreField, Set<LoadField>> storeToLoads = MapUtils.newMap();
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
        return storeToLoads;
    }

    private Map<Obj, Set<JField>> computeUninitializedFields() {
        // compute uninitialized fields based on pointer analysis result
        Map<Obj, Set<JField>> uninitFields = MapUtils.newMap();
        Map<Obj, Set<Var>> pointedBy = MapUtils.newMap();
        pta.vars().forEach(var ->
                pta.getPointsToSet(var).forEach(obj ->
                        MapUtils.addToMapSet(pointedBy, obj, var)));
        pointedBy.keySet().forEach(obj -> {
            Set<JField> allFields = getAllFieldsOf(obj.getType());
            pointedBy.get(obj).forEach(var -> {
                if (var.getMethod().isConstructor() &&
                        var.getType().equals(obj.getType())) {
                    JMethod constructor = var.getMethod();
                    Set<JField> initFields =
                            computeInitializedFields(constructor, obj);
                    allFields.stream()
                            .filter(Predicate.not(initFields::contains))
                            .forEach(field -> MapUtils.addToMapSet(uninitFields, obj, field));
                }
            });
        });
        return uninitFields;
    }

    private Set<JField> getAllFieldsOf(Type type) {
        Set<JField> fields = SetUtils.newHybridSet();
        if (type instanceof ClassType) {
            JClass klass = ((ClassType) type).getJClass();
            while (klass != null) {
                fields.addAll(klass.getDeclaredFields());
                klass = klass.getSuperClass();
            }
        }
        return fields;
    }

    private Set<JField> computeInitializedFields(JMethod constructor, Obj baseObj) {
        Set<JField> fields = SetUtils.newHybridSet();
        Set<JMethod> methods = SetUtils.newHybridSet();
        Queue<JMethod> workList = new SetQueue<>();
        workList.add(constructor);
        while (!workList.isEmpty()) {
            JMethod m = workList.poll();
            if (methods.add(m)) {
                m.getIR().getStmts()
                        .stream()
                        .filter(s -> s instanceof StoreField)
                        .map(s -> (StoreField) s)
                        .filter(Predicate.not(StoreField::isStatic))
                        .forEach(store -> {
                            InstanceFieldAccess fa =
                                    (InstanceFieldAccess) store.getFieldAccess();
                            Var base = fa.getBase();
                            if (pta.getPointsToSet(base).contains(baseObj)) {
                                fields.add(fa.getFieldRef().resolve());
                            }
                        });
                pta.getCallGraph()
                        .succsOf(m)
                        .forEach(workList::add);
            }
        }
        return fields;
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
    protected boolean transferCall(Stmt stmt, MapFact<Var, Value> in, MapFact<Var, Value> out) {
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
    protected boolean transferNonCall(Stmt stmt, MapFact<Var, Value> in, MapFact<Var, Value> out) {
        return aliasAware ?
                transferAliasAware(stmt, in, out) :
                cp.transferNode(stmt, in, out);
    }

    private boolean transferAliasAware(
            Stmt stmt, MapFact<Var, Value> in, MapFact<Var, Value> out) {
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
                // if o.f may be uninitialized, update x to 0
                Var base = ((InstanceFieldAccess) load.getFieldAccess()).getBase();
                JField field = load.getFieldRef().resolve();
                for (Obj obj : pta.getPointsToSet(base)) {
                    Set<JField> uninitFields = uninitializedFields.get(obj);
                    if (uninitFields != null && uninitFields.contains(field)) {
                        Value oldV = out.get(lhs);
                        Value newV = cp.meetValue(oldV, Value.makeConstant(0));
                        changed |= out.update(lhs, newV);
                        break;
                    }
                }
                return changed;
            } else { // o.f = x
                StoreField store = (StoreField) stmt;
                Var var = store.getRValue();
                Value value = in.get(var);
                storeToLoads.get(store).forEach(load -> {
                    Var lhs = load.getLValue();
                    MapFact<Var, Value> loadOut = solver.getOutFact(load);
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

    private static boolean isAliasRelevant(Stmt stmt) {
        if (stmt instanceof FieldStmt) {
            FieldStmt<?,?> fs = (FieldStmt<?, ?>) stmt;
            return !fs.isStatic() &&
                    fs.getRValue().getType().equals(PrimitiveType.INT);
        }
        return false;
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
