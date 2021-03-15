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

package pascal.taie.pta.core.cs;

import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.java.classes.JField;
import pascal.taie.java.classes.JMethod;
import pascal.taie.pta.core.context.Context;
import pascal.taie.pta.core.heap.Obj;

import java.util.stream.Stream;

/**
 * Manages context-sensitive elements in pointer analysis.
 */
public interface CSManager {

    CSVar getCSVar(Context context, Var var);

    InstanceField getInstanceField(CSObj base, JField field);

    ArrayIndex getArrayIndex(CSObj array);

    StaticField getStaticField(JField field);

    CSObj getCSObj(Context heapContext, Obj obj);

    CSCallSite getCSCallSite(Context context, InvokeExp callSite);

    CSMethod getCSMethod(Context context, JMethod method);

    Stream<CSVar> getCSVars();

    Stream<InstanceField> getInstanceFields();

    Stream<ArrayIndex> getArrayIndexes();

    Stream<StaticField> getStaticFields();
}
