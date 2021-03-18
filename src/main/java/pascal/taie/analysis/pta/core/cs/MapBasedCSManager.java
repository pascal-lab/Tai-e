/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.analysis.pta.core.cs;

import pascal.taie.analysis.pta.core.context.Context;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.analysis.pta.set.PointsToSetFactory;
import pascal.taie.util.CollectionUtils;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static pascal.taie.util.CollectionUtils.newHybridMap;
import static pascal.taie.util.CollectionUtils.newMap;

/**
 * Managing data by maintaining the data and their context-sensitive
 * counterparts by maps.
 */
public class MapBasedCSManager implements CSManager {

    private final Map<Var, Map<Context, CSVar>> vars = newMap();
    private final Map<CSObj, Map<JField, InstanceField>> instanceFields = newMap();
    private final Map<CSObj, ArrayIndex> arrayIndexes = newMap();
    private final Map<JField, StaticField> staticFields = newMap();
    private final Map<Obj, Map<Context, CSObj>> objs = newMap();
    private final Map<InvokeExp, Map<Context, CSCallSite>> callSites = newMap();
    private final Map<JMethod, Map<Context, CSMethod>> methods = newMap();

    private static <R, Key1, Key2> R getOrCreateCSElement(
            Map<Key1, Map<Key2, R>> map, Key1 key1, Key2 key2, BiFunction<Key1, Key2, R> creator) {
        return map.computeIfAbsent(key1, k -> newHybridMap())
                .computeIfAbsent(key2, (k) -> creator.apply(key1, key2));
    }

    @Override
    public CSVar getCSVar(Context context, Var var) {
        return getOrCreateCSElement(vars, Objects.requireNonNull(var), context,
                (v, c) -> initializePointsToSet(new CSVar(v, c)));
    }

    @Override
    public InstanceField getInstanceField(CSObj base, JField field) {
        return getOrCreateCSElement(instanceFields, base, field,
                (b, f) -> initializePointsToSet(new InstanceField(b, f)));
    }

    @Override
    public ArrayIndex getArrayIndex(CSObj array) {
        return arrayIndexes.computeIfAbsent(array,
                (a) -> initializePointsToSet(new ArrayIndex(a)));
    }

    @Override
    public StaticField getStaticField(JField field) {
        return staticFields.computeIfAbsent(field,
                (f) -> initializePointsToSet(new StaticField(f)));
    }

    @Override
    public CSObj getCSObj(Context heapContext, Obj obj) {
        return getOrCreateCSElement(objs, obj, heapContext, CSObj::new);
    }

    @Override
    public CSCallSite getCSCallSite(Context context, InvokeExp callSite) {
        return getOrCreateCSElement(callSites, callSite, context, CSCallSite::new);
    }

    @Override
    public CSMethod getCSMethod(Context context, JMethod method) {
        return getOrCreateCSElement(methods, method, context, CSMethod::new);
    }

    @Override
    public Stream<CSVar> getCSVars() {
        return CollectionUtils.getAllValues(vars);
    }

    @Override
    public Stream<InstanceField> getInstanceFields() {
        return CollectionUtils.getAllValues(instanceFields);
    }

    @Override
    public Stream<ArrayIndex> getArrayIndexes() {
        return arrayIndexes.values().stream();
    }

    @Override
    public Stream<StaticField> getStaticFields() {
        return staticFields.values().stream();
    }

    private <P extends Pointer> P initializePointsToSet(P pointer) {
        pointer.setPointsToSet(PointsToSetFactory.make());
        return pointer;
    }
}
