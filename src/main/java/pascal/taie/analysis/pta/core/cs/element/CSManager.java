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
import pascal.taie.util.Indexer;

import java.util.Collection;

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
     * @return all variables (without contexts).
     */
    Collection<Var> getVars();

    /**
     * @return all relevant context-sensitive variables for given variable.
     */
    Collection<CSVar> getCSVarsOf(Var var);

    /**
     * @return all context-sensitive variables.
     */
    Collection<CSVar> getCSVars();

    /**
     * @return all context-sensitive objects.
     */
    Collection<CSObj> getObjects();

    /**
     * @return all static field pointers.
     */
    Collection<StaticField> getStaticFields();

    /**
     * @return all instance field pointers.
     */
    Collection<InstanceField> getInstanceFields();

    /**
     * @return all array index pointers.
     */
    Collection<ArrayIndex> getArrayIndexes();

    /**
     * @return {@link Indexer} for {@link CSObj} maintained by this manager.
     * The indexer is useful for creating efficient points-to sets.
     */
    Indexer<CSObj> getObjectIndexer();

    /**
     * @return {@link Indexer} for {@link CSMethod} maintained by this manager.
     */
    Indexer<CSMethod> getMethodIndexer();
}
