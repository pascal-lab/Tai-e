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

import java.util.stream.Stream;

/**
 * Manages the data structures in context-sensitive pointer analysis.
 */
public interface DataManager {

    void setPointsToSetFactory(PointsToSetFactory setFactory);

    CSVariable getCSVariable(Context context, Variable var);

    InstanceField getInstanceField(CSObj base, Field field);

    ArrayIndex getArrayIndex(CSObj array);
    
    StaticField getStaticField(Field field);

    CSObj getCSObj(Context heapContext, Obj obj);

    CSCallSite getCSCallSite(Context context, CallSite callSite);

    CSMethod getCSMethod(Context context, Method method);

    Stream<CSVariable> getCSVariables();

    Stream<InstanceField> getInstanceFields();

    Stream<ArrayIndex> getArrayIndexes();

    Stream<StaticField> getStaticFields();
}
