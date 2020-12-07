/*
 * Tai'e - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai'e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.pta.core.cs;

import pascal.taie.pta.core.context.Context;
import pascal.taie.pta.element.CallSite;
import pascal.taie.pta.element.Field;
import pascal.taie.pta.element.Method;
import pascal.taie.pta.element.Obj;
import pascal.taie.pta.element.Variable;
import pascal.taie.pta.set.PointsToSetFactory;
import pascal.taie.util.CollectionUtils;
import pascal.taie.util.HybridArrayHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * Managing data by maintaining the data and their context-sensitive
 * counterparts by maps.
 */
public class MapBasedCSManager implements CSManager {

    private final Map<Variable, Map<Context, CSVariable>> vars = new HashMap<>();
    private final Map<CSObj, Map<Field, InstanceField>> instanceFields = new HashMap<>();
    private final Map<CSObj, ArrayIndex> arrayIndexes = new HashMap<>();
    private final Map<Field, StaticField> staticFields = new HashMap<>();
    private final Map<Obj, Map<Context, CSObj>> objs = new HashMap<>();
    private final Map<CallSite, Map<Context, CSCallSite>> callSites = new HashMap<>();
    private final Map<Method, Map<Context, CSMethod>> methods = new HashMap<>();

    private static <R, Key1, Key2> R getOrCreateCSElement(
            Map<Key1, Map<Key2, R>> map, Key1 key1, Key2 key2, BiFunction<Key1, Key2, R> creator) {
        return map.computeIfAbsent(key1, k -> new HybridArrayHashMap<>())
                .computeIfAbsent(key2, (k) -> creator.apply(key1, key2));
    }

    @Override
    public CSVariable getCSVariable(Context context, Variable var) {
        return getOrCreateCSElement(vars, Objects.requireNonNull(var), context,
                (v, c) -> initializePointsToSet(new CSVariable(v, c)));
    }

    @Override
    public InstanceField getInstanceField(CSObj base, Field field) {
        return getOrCreateCSElement(instanceFields, base, field,
                (b, f) -> initializePointsToSet(new InstanceField(b, f)));
    }

    @Override
    public ArrayIndex getArrayIndex(CSObj array) {
        return arrayIndexes.computeIfAbsent(array,
                (a) -> initializePointsToSet(new ArrayIndex(a)));
    }

    @Override
    public StaticField getStaticField(Field field) {
        return staticFields.computeIfAbsent(field,
                (f) -> initializePointsToSet(new StaticField(f)));
    }

    @Override
    public CSObj getCSObj(Context heapContext, Obj obj) {
        return getOrCreateCSElement(objs, obj, heapContext, CSObj::new);
    }

    @Override
    public CSCallSite getCSCallSite(Context context, CallSite callSite) {
        return getOrCreateCSElement(callSites, callSite, context, CSCallSite::new);
    }

    @Override
    public CSMethod getCSMethod(Context context, Method method) {
        return getOrCreateCSElement(methods, method, context, CSMethod::new);
    }

    @Override
    public Stream<CSVariable> getCSVariables() {
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
