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
 * Manages context-sensitive elements and pointers in pointer analysis.
 */
public interface CSManager {

    /**
     * @return a context-sensitive variable for given context and variable.
     */
    CSVar getCSVar(Context context, Var var);

    /**
     * @return a context-sensitive object for given context and object.
     */
    CSObj getCSObj(Context heapContext, Obj obj);

    /**
     * @return a context-sensitive call site for given context and call site.
     */
    CSCallSite getCSCallSite(Context context, Invoke callSite);

    /**
     * @return a context-sensitive method for given context and method.
     */
    CSMethod getCSMethod(Context context, JMethod method);

    /**
     * @return the corresponding StaticField pointer for given static field.
     */
    StaticField getStaticField(JField field);

    /**
     * @return the corresponding InstanceField pointer for given object
     * and instance field.
     */
    InstanceField getInstanceField(CSObj base, JField field);

    /**
     * @return the corresponding ArrayIndex pointer for given array object.
     */
    ArrayIndex getArrayIndex(CSObj array);

    /**
     * @return all relevant context-sensitive variables for given variable.
     */
    Stream<CSVar> csVarsOf(Var var);

    /**
     * @return all context-sensitive variables.
     */
    Stream<CSVar> csVars();

    /**
     * @return all context-sensitive objects.
     */
    Stream<CSObj> objects();

    /**
     * @return all static field pointers.
     */
    Stream<StaticField> staticFields();

    /**
     * @return all instance field pointers.
     */
    Stream<InstanceField> instanceFields();

    /**
     * @return all array index pointers.
     */
    Stream<ArrayIndex> arrayIndexes();
}
