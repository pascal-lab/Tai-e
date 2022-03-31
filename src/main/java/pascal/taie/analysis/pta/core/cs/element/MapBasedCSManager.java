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
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.Indexer;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.TwoKeyMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Manages data by maintaining the data and their context-sensitive
 * counterparts by maps.
 */
public class MapBasedCSManager implements CSManager {

    private final TwoKeyMap<Var, Context, CSVar> vars = Maps.newTwoKeyMap();
    private final TwoKeyMap<Invoke, Context, CSCallSite> callSites = Maps.newTwoKeyMap();
    private final TwoKeyMap<JMethod, Context, CSMethod> methods = Maps.newTwoKeyMap();
    private final Map<JField, StaticField> staticFields = Maps.newMap();
    private final TwoKeyMap<CSObj, JField, InstanceField> instanceFields = Maps.newTwoKeyMap();
    private final Map<CSObj, ArrayIndex> arrayIndexes = Maps.newMap();

    /**
     * Delegates implementation of CSObj-related API to CSObjManager.
     */
    private final CSObjManager objManager = new CSObjManager();

    @Override
    public CSVar getCSVar(Context context, Var var) {
        return vars.computeIfAbsent(var, context, CSVar::new);
    }

    @Override
    public CSObj getCSObj(Context heapContext, Obj obj) {
        return objManager.getCSObj(heapContext, obj);
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
        return staticFields.computeIfAbsent(field, StaticField::new);
    }

    @Override
    public InstanceField getInstanceField(CSObj base, JField field) {
        return instanceFields.computeIfAbsent(base, field, InstanceField::new);
    }

    @Override
    public ArrayIndex getArrayIndex(CSObj array) {
        return arrayIndexes.computeIfAbsent(array, ArrayIndex::new);
    }

    @Override
    public Collection<Var> getVars() {
        return vars.keySet();
    }

    @Override
    public Collection<CSVar> getCSVars() {
        return vars.values();
    }

    @Override
    public Collection<CSVar> getCSVarsOf(Var var) {
        var csVars = vars.get(var);
        return csVars != null ? csVars.values() : Set.of();
    }

    @Override
    public Collection<CSObj> getObjects() {
        return objManager.getObjects();
    }

    @Override
    public Collection<StaticField> getStaticFields() {
        return Collections.unmodifiableCollection(staticFields.values());
    }

    @Override
    public Collection<InstanceField> getInstanceFields() {
        return instanceFields.values();
    }

    @Override
    public Collection<ArrayIndex> getArrayIndexes() {
        return Collections.unmodifiableCollection(arrayIndexes.values());
    }

    @Override
    public Indexer<CSObj> getObjectIndexer() {
        return objManager;
    }
}
