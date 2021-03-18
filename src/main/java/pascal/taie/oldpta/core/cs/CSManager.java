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

package pascal.taie.oldpta.core.cs;

import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.oldpta.core.context.Context;
import pascal.taie.oldpta.ir.CallSite;
import pascal.taie.oldpta.ir.Obj;
import pascal.taie.oldpta.ir.Variable;

import java.util.stream.Stream;

/**
 * Manages context-sensitive elements in pointer analysis.
 */
public interface CSManager {

    CSVariable getCSVariable(Context context, Variable var);

    InstanceField getInstanceField(CSObj base, JField field);

    ArrayIndex getArrayIndex(CSObj array);

    StaticField getStaticField(JField field);

    CSObj getCSObj(Context heapContext, Obj obj);

    CSCallSite getCSCallSite(Context context, CallSite callSite);

    CSMethod getCSMethod(Context context, JMethod method);

    Stream<CSVariable> getCSVariables();

    Stream<InstanceField> getInstanceFields();

    Stream<ArrayIndex> getArrayIndexes();

    Stream<StaticField> getStaticFields();
}
