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

package pascal.taie.analysis.pta;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.language.classes.JMethod;

import java.util.Set;

/**
 * Extended version {@link PointerAnalysisResult}.
 *
 * Unlike {@link PointerAnalysisResult} which provides results directly
 * computed from pointer analysis, this class provides more commonly-used results
 * that are indirectly derived from original pointer analysis result.
 */
public interface PointerAnalysisResultEx {

    PointerAnalysisResult getPointerAnalysisResult();

    /**
     * @return the methods whose receiver objects contain obj.
     */
    Set<JMethod> getMethodsInvokedOn(Obj obj);

    /**
     * @return the receiver objects of given method.
     */
    Set<Obj> getReceiverObjectsOf(JMethod method);
}
