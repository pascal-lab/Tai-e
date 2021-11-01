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

package pascal.taie.analysis.pta.core.cs.selector;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.language.classes.JMethod;

/**
 * Represents context sensitivity variants.
 */
public interface ContextSelector {

    /**
     * @return the empty context that does not contain any context elements.
     */
    Context getEmptyContext();

    /**
     * Selects contexts for static methods.
     *
     * @param callSite the (context-sensitive) call site.
     * @param callee   the callee.
     * @return the context for the callee.
     */
    Context selectContext(CSCallSite callSite, JMethod callee);

    /**
     * Selects contexts for instance methods.
     *
     * @param callSite the (context-sensitive) call site.
     * @param recv     the (context-sensitive) receiver object for the callee.
     * @param callee   the callee.
     * @return the context for the callee.
     */
    Context selectContext(CSCallSite callSite, CSObj recv, JMethod callee);

    /**
     * Selects heap contexts for new-created abstract objects.
     *
     * @param method the (context-sensitive) method that contains the
     *               allocation site of the new-created object.
     * @param obj    the new-created object.
     * @return the heap context for the object.
     */
    Context selectHeapContext(CSMethod method, Obj obj);
}
