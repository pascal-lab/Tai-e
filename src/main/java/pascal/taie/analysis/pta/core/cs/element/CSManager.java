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

    CSCallSite getCSCallSite(Context context, Invoke callSite);

    CSMethod getCSMethod(Context context, JMethod method);

    Stream<CSVar> csVarsOf(Var var);

    Stream<CSVar> csVars();

    Stream<InstanceField> instanceFields();

    Stream<ArrayIndex> arrayIndexes();

    Stream<StaticField> staticFields();

    Stream<CSObj> objects();
}
