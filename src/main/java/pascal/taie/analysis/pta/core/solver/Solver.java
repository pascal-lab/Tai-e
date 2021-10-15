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

package pascal.taie.analysis.pta.core.solver;

import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.ArrayIndex;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.NativeObjs;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.analysis.pta.pts.PointsToSetFactory;
import pascal.taie.config.AnalysisOptions;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeManager;

/**
 * Common functionalities for implementing context-sensitive
 * pointer analysis solver.
 */
public abstract class Solver {

    protected AnalysisOptions options;

    /**
     * Only analyzes application code.
     */
    protected boolean onlyApp;

    protected final ClassHierarchy hierarchy;

    protected final TypeManager typeManager;

    protected final NativeObjs nativeObjs;

    protected CSManager csManager;

    protected Plugin plugin;

    protected OnFlyCallGraph callGraph;

    protected PointerFlowGraph pointerFlowGraph;

    protected HeapModel heapModel;

    protected ContextSelector contextSelector;

    protected PointerAnalysisResult result;

    protected Solver() {
        this.typeManager = World.getTypeManager();
        this.hierarchy = World.getClassHierarchy();
        this.nativeObjs = new NativeObjs(typeManager);
    }

    public AnalysisOptions getOptions() {
        return options;
    }

    public void setOptions(AnalysisOptions options) {
        this.options = options;
    }

    public ClassHierarchy getHierarchy() {
        return hierarchy;
    }

    public TypeManager getTypeManager() {
        return typeManager;
    }

    public NativeObjs getNativeObjs() {
        return nativeObjs;
    }

    public CSManager getCSManager() {
        return csManager;
    }

    public void setCSManager(CSManager csManager) {
        this.csManager = csManager;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    public HeapModel getHeapModel() {
        return heapModel;
    }

    public void setHeapModel(HeapModel heapModel) {
        this.heapModel = heapModel;
    }

    public ContextSelector getContextSelector() {
        return contextSelector;
    }

    public void setContextSelector(ContextSelector contextSelector) {
        this.contextSelector = contextSelector;
    }

    public CallGraph<CSCallSite, CSMethod> getCallGraph() {
        return callGraph;
    }

    /**
     * Starts this solver.
     */
    public abstract void solve();

    public PointsToSet getPointsToSetOf(Pointer pointer) {
        return pointer.getPointsToSet();
    }

    // ---------- side-effect APIs (begin) ----------
    public abstract void addPointsTo(Pointer pointer, PointsToSet pts);

    public void addPointsTo(Pointer pointer, CSObj csObj) {
        addPointsTo(pointer, PointsToSetFactory.make(csObj));
    }

    /**
     * Adds a context-sensitive variable points-to relation.
     *
     * @param context     context of the method which contains the variable
     * @param var         the variable
     * @param heapContext heap context for the object
     * @param obj         the object to be added
     */
    public void addVarPointsTo(Context context, Var var,
                               Context heapContext, Obj obj) {
        addVarPointsTo(context, var,
                csManager.getCSObj(heapContext, obj));
    }

    public void addVarPointsTo(Context context, Var var, CSObj csObj) {
        addPointsTo(csManager.getCSVar(context, var), csObj);
    }

    public void addVarPointsTo(Context context, Var var, PointsToSet pts) {
        addPointsTo(csManager.getCSVar(context, var), pts);
    }

    /**
     * Adds a context-sensitive array index points-to relation.
     *
     * @param arrayContext heap context of the array object
     * @param array        the array object
     * @param heapContext  heap context for the element
     * @param obj          the element to be stored into the array
     */
    public void addArrayPointsTo(Context arrayContext, Obj array,
                                 Context heapContext, Obj obj) {
        CSObj csArray = csManager.getCSObj(arrayContext, array);
        ArrayIndex arrayIndex = csManager.getArrayIndex(csArray);
        CSObj elem = csManager.getCSObj(heapContext, obj);
        addPointsTo(arrayIndex, elem);
    }

    /**
     * Adds static field points-to relations.
     *
     * @param field the static field
     * @param pts   the objects to be added to the points-to set of the field.
     */
    public void addStaticFieldPointsTo(JField field, PointsToSet pts) {
        assert field.isStatic();
        addPointsTo(csManager.getStaticField(field), pts);
    }

    /**
     * Adds an edge "source -> target" to the PFG.
     */
    public void addPFGEdge(Pointer source, Pointer target, PointerFlowEdge.Kind kind) {
        addPFGEdge(source, target, null, kind);
    }

    /**
     * Adds an edge "source -> target" to the PFG.
     * If type is not null, then we need to filter out assignable objects
     * in source points-to set.
     */
    public abstract void addPFGEdge(Pointer source, Pointer target, Type type,
                                    PointerFlowEdge.Kind kind);

    /**
     * Adds a call edge.
     *
     * @param edge the added edge.
     */
    public abstract void addCallEdge(Edge<CSCallSite, CSMethod> edge);

    /**
     * Adds a context-sensitive method.
     *
     * @param csMethod the added context-sensitive method.
     */
    public abstract void addCSMethod(CSMethod csMethod);

    /**
     * Analyzes the initializer of given class.
     *
     * @param cls the class to be initialized.
     */
    public abstract void initializeClass(JClass cls);

    // ---------- side-effect APIs (end) ----------

    public PointerAnalysisResult getResult() {
        if (result == null) {
            result = new PointerAnalysisResultImpl(csManager, callGraph);
        }
        return result;
    }
}
