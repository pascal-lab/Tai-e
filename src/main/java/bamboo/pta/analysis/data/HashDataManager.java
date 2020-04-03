/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.analysis.data;

import bamboo.pta.analysis.context.Context;
import bamboo.pta.element.CallSite;
import bamboo.pta.element.Field;
import bamboo.pta.element.Method;
import bamboo.pta.element.Obj;
import bamboo.pta.element.Variable;
import bamboo.pta.set.PointsToSetFactory;
import bamboo.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * Hash map based element manager.
 */
public class HashDataManager implements DataManager {

    private PointsToSetFactory setFactory;

    private Map<Context, Map<Variable, CSVariable>> vars = new HashMap<>();

    private Map<CSObj, Map<Field, InstanceField>> instanceFields = new HashMap<>();

    private Map<CSObj, ArrayField> arrayFields = new HashMap<>();

    private Map<Field, StaticField> staticFields = new HashMap<>();

    private Map<Context, Map<Obj, CSObj>> objs = new HashMap<>();

    private Map<Context, Map<CallSite, CSCallSite>> callSites = new HashMap<>();

    private Map<Context, Map<Method, CSMethod>> methods = new HashMap<>();

    public HashDataManager(PointsToSetFactory setFactory) {
        setPointsToSetFactory(setFactory);
    }

    @Override
    public void setPointsToSetFactory(PointsToSetFactory setFactory) {
        this.setFactory = setFactory;
    }

    @Override
    public CSVariable getCSVariable(Context context, Variable var) {
        return getOrCreateCSElement(vars, context, var,
                (c, v) -> initializePointsToSet(new CSVariable(c, v)));
    }

    @Override
    public InstanceField getInstanceField(CSObj base, Field field) {
        return getOrCreateCSElement(instanceFields, base, field,
                (b, f) -> initializePointsToSet(new InstanceField(b, f)));
    }

    @Override
    public ArrayField getArrayField(CSObj array) {
        return arrayFields.computeIfAbsent(array,
                (a) -> initializePointsToSet(new ArrayField(a)));
    }

    @Override
    public StaticField getStaticField(Field field) {
        return staticFields.computeIfAbsent(field,
                (f) -> initializePointsToSet(new StaticField(f)));
    }

    @Override
    public CSObj getCSObj(Context heapContext, Obj obj) {
        return getOrCreateCSElement(objs, heapContext, obj, CSObj::new);
    }

    @Override
    public CSCallSite getCSCallSite(Context context, CallSite callSite) {
        return getOrCreateCSElement(callSites, context, callSite, CSCallSite::new);
    }

    @Override
    public CSMethod getCSMethod(Context context, Method method) {
        return getOrCreateCSElement(methods, context, method, CSMethod::new);
    }

    @Override
    public Stream<CSVariable> getCSVariables() {
        return CollectionUtils.getAllValues(vars);
    }

    @Override
    public Stream<InstanceField> getInstanceFields() {
        return CollectionUtils.getAllValues(instanceFields);
    }

    private <P extends Pointer> P initializePointsToSet(P pointer) {
        pointer.setPointsToSet(setFactory.makePointsToSet());
        return pointer;
    }

    private static <R, T, U> R getOrCreateCSElement(
            Map<T, Map<U, R>> map, T key1, U key2, BiFunction<T, U, R> creator) {
        return map.computeIfAbsent(key1, k -> new HashMap<>())
                .computeIfAbsent(key2, (k) -> creator.apply(key1, key2));
    }
}
