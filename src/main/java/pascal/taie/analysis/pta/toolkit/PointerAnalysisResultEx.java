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

package pascal.taie.analysis.pta.toolkit;

import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.language.classes.JMethod;

import java.util.Set;

/**
 * Extended version {@link PointerAnalysisResult}.
 * <p>
 * Unlike {@link PointerAnalysisResult} which only provides results directly
 * computed from pointer analysis, this class provides more commonly-used results
 * that are indirectly derived from original pointer analysis result.
 */
public interface PointerAnalysisResultEx {

    /**
     * @return the base pointer analysis result.
     */
    PointerAnalysisResult getBase();

    /**
     * @return the methods whose receiver objects contain obj.
     */
    Set<JMethod> getMethodsInvokedOn(Obj obj);

    /**
     * @return the receiver objects of given method.
     */
    Set<Obj> getReceiverObjectsOf(JMethod method);

    /**
     * @return the objects that are allocated in given method.
     */
    Set<Obj> getObjectsAllocatedIn(JMethod method);
}
