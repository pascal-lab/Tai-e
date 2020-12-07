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

import java.util.stream.Stream;

/**
 * Manages context-sensitive elements in pointer analysis.
 */
public interface CSManager {

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
