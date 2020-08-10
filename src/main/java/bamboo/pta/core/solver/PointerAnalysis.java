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

package bamboo.pta.core.solver;

import bamboo.callgraph.CallGraph;
import bamboo.pta.core.ProgramManager;
import bamboo.pta.core.context.ContextSelector;
import bamboo.pta.core.cs.ArrayIndex;
import bamboo.pta.core.cs.CSCallSite;
import bamboo.pta.core.cs.CSManager;
import bamboo.pta.core.cs.CSMethod;
import bamboo.pta.core.cs.CSVariable;
import bamboo.pta.core.cs.InstanceField;
import bamboo.pta.core.cs.StaticField;
import bamboo.pta.core.heap.HeapModel;
import bamboo.pta.set.PointsToSetFactory;

import java.util.stream.Stream;

public interface PointerAnalysis {

    ProgramManager getProgramManager();

    CSManager getCSManager();

    ContextSelector getContextSelector();

    CallGraph<CSCallSite, CSMethod> getCallGraph();

    void analyze();

    /**
     * @return all variables in the (reachable) program.
     */
    Stream<CSVariable> getVariables();

    /**
     * @return all instance fields in the (reachable) program.
     */
    Stream<InstanceField> getInstanceFields();

    /**
     * @return all array indexes in the (reachable) program.
     */
    Stream<ArrayIndex> getArrayIndexes();

    /**
     * @return all static fields in the (reachable) program.
     */
    Stream<StaticField> getStaticFields();

    /**
     * @return if this pointer analysis is context-sensitive.
     */
    boolean isContextSensitive();
}
