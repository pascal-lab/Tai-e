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

package pascal.taie.newpta.core.solver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.callgraph.CallGraph;
import pascal.taie.ir.exp.Var;
import pascal.taie.java.ClassHierarchy;
import pascal.taie.java.TypeManager;
import pascal.taie.java.World;
import pascal.taie.java.classes.JMethod;
import pascal.taie.newpta.core.context.Context;
import pascal.taie.newpta.core.context.ContextSelector;
import pascal.taie.newpta.core.cs.ArrayIndex;
import pascal.taie.newpta.core.cs.CSCallSite;
import pascal.taie.newpta.core.cs.CSManager;
import pascal.taie.newpta.core.cs.CSMethod;
import pascal.taie.newpta.core.cs.CSVar;
import pascal.taie.newpta.core.cs.InstanceField;
import pascal.taie.newpta.core.cs.StaticField;
import pascal.taie.newpta.core.heap.HeapModel;
import pascal.taie.newpta.core.heap.Obj;
import pascal.taie.newpta.set.PointsToSet;
import pascal.taie.newpta.plugin.Plugin;

import java.util.Set;
import java.util.stream.Stream;

public class PointerAnalysisImpl implements PointerAnalysis {

    private static final Logger logger = LogManager.getLogger(PointerAnalysisImpl.class);

    private final ClassHierarchy hierarchy;

    private final TypeManager typeManager;

    private CSManager csManager;

    private Plugin plugin;

    private OnFlyCallGraph callGraph;

    private PointerFlowGraph pointerFlowGraph;

    private HeapModel heapModel;

    private ContextSelector contextSelector;

    private WorkList workList;

    private Set<JMethod> reachableMethods;

    public PointerAnalysisImpl() {
        this.typeManager = World.getTypeManager();
        this.hierarchy = World.getClassHierarchy();
    }
    
    @Override
    public ClassHierarchy getHierarchy() {
        return null;
    }

    @Override
    public HeapModel getHeapModel() {
        return null;
    }

    public void setHeapModel(HeapModel heapModel) {
        this.heapModel = heapModel;
    }

    @Override
    public CSManager getCSManager() {
        return null;
    }

    public void setCSManager(CSManager csManager) {
        this.csManager = csManager;
    }

    @Override
    public ContextSelector getContextSelector() {
        return null;
    }

    public void setContextSelector(ContextSelector contextSelector) {
        this.contextSelector = contextSelector;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CallGraph<CSCallSite, CSMethod> getCallGraph() {
        return null;
    }

    @Override
    public void analyze() {

    }

    @Override
    public void addPointsTo(Context context, Var var, Context heapContext, Obj obj) {

    }

    @Override
    public void addPointsTo(Context context, Var var, PointsToSet pts) {

    }

    @Override
    public void addPointsTo(Context arrayContext, Obj array, Context heapContext, Obj obj) {

    }

    @Override
    public Stream<CSVar> getVars() {
        return null;
    }

    @Override
    public Stream<InstanceField> getInstanceFields() {
        return null;
    }

    @Override
    public Stream<ArrayIndex> getArrayIndexes() {
        return null;
    }

    @Override
    public Stream<StaticField> getStaticFields() {
        return null;
    }
}
