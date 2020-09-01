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

package panda.pta.core.cs;

import panda.pta.core.context.Context;
import panda.pta.element.CallSite;
import panda.pta.element.Field;
import panda.pta.element.Method;
import panda.pta.element.Obj;
import panda.pta.element.Variable;
import panda.pta.set.PointsToSetFactory;

import java.util.stream.Stream;

/**
 * Manages context-sensitive elements in pointer analysis.
 */
public interface CSManager {

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
