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

package bamboo.pta.analysis.solver;

import bamboo.callgraph.CallGraph;
import bamboo.pta.analysis.ProgramManager;
import bamboo.pta.analysis.context.ContextSelector;
import bamboo.pta.analysis.data.ArrayIndex;
import bamboo.pta.analysis.data.CSCallSite;
import bamboo.pta.analysis.data.CSMethod;
import bamboo.pta.analysis.data.CSVariable;
import bamboo.pta.analysis.data.DataManager;
import bamboo.pta.analysis.data.InstanceField;
import bamboo.pta.analysis.data.StaticField;
import bamboo.pta.analysis.heap.HeapModel;
import bamboo.pta.set.PointsToSetFactory;

import java.util.stream.Stream;

public interface PointerAnalysis {

    ProgramManager getProgramManager();

    void setProgramManager(ProgramManager programManager);

    DataManager getDataManager();

    void setDataManager(DataManager dataManager);

    ContextSelector getContextSelector();

    void setContextSelector(ContextSelector contextSelector);

    void setHeapModel(HeapModel heapModel);

    void setPointsToSetFactory(PointsToSetFactory setFactory);

    void analyze();

    CallGraph<CSCallSite, CSMethod> getCallGraph();

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
