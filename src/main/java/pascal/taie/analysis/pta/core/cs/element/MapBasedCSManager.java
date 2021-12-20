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

package pascal.taie.analysis.pta.core.cs.element;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.pts.PointsToSetFactory;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.TwoKeyMap;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Manages data by maintaining the data and their context-sensitive
 * counterparts by maps.
 */
public class MapBasedCSManager implements CSManager {

    private final TwoKeyMap<Var, Context, CSVar> vars = Maps.newTwoKeyMap();
    private final TwoKeyMap<Obj, Context, CSObj> objs = Maps.newTwoKeyMap();
    private final TwoKeyMap<Invoke, Context, CSCallSite> callSites = Maps.newTwoKeyMap();
    private final TwoKeyMap<JMethod, Context, CSMethod> methods = Maps.newTwoKeyMap();
    private final Map<JField, StaticField> staticFields = Maps.newMap();
    private final TwoKeyMap<CSObj, JField, InstanceField> instanceFields = Maps.newTwoKeyMap();
    private final Map<CSObj, ArrayIndex> arrayIndexes = Maps.newMap();

    @Override
    public CSVar getCSVar(Context context, Var var) {
        return vars.computeIfAbsent(var, context,
                (v, c) -> initializePointsToSet(new CSVar(v, c)));
    }

    @Override
    public CSObj getCSObj(Context heapContext, Obj obj) {
        return objs.computeIfAbsent(obj, heapContext, CSObj::new);
    }

    @Override
    public CSCallSite getCSCallSite(Context context, Invoke callSite) {
        return callSites.computeIfAbsent(callSite, context, CSCallSite::new);
    }

    @Override
    public CSMethod getCSMethod(Context context, JMethod method) {
        return methods.computeIfAbsent(method, context, CSMethod::new);
    }

    @Override
    public StaticField getStaticField(JField field) {
        return staticFields.computeIfAbsent(field,
                (f) -> initializePointsToSet(new StaticField(f)));
    }

    @Override
    public InstanceField getInstanceField(CSObj base, JField field) {
        return instanceFields.computeIfAbsent(base, field,
                (b, f) -> initializePointsToSet(new InstanceField(b, f)));
    }

    @Override
    public ArrayIndex getArrayIndex(CSObj array) {
        return arrayIndexes.computeIfAbsent(array,
                (a) -> initializePointsToSet(new ArrayIndex(a)));
    }

    @Override
    public Stream<CSVar> csVars() {
        return vars.values().stream();
    }

    @Override
    public Stream<CSVar> csVarsOf(Var var) {
        var csVars = vars.get(var);
        return csVars == null ? Stream.empty() :
                csVars.values().stream();
    }

    @Override
    public Stream<CSObj> objects() {
        return objs.values().stream();
    }

    @Override
    public Stream<StaticField> staticFields() {
        return staticFields.values().stream();
    }

    @Override
    public Stream<InstanceField> instanceFields() {
        return instanceFields.values().stream();
    }

    @Override
    public Stream<ArrayIndex> arrayIndexes() {
        return arrayIndexes.values().stream();
    }

    private <P extends Pointer> P initializePointsToSet(P pointer) {
        pointer.setPointsToSet(PointsToSetFactory.make());
        return pointer;
    }
}
