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
import pascal.taie.java.types.Type;
import pascal.taie.newpta.core.context.Context;
import pascal.taie.newpta.core.context.ContextSelector;
import pascal.taie.newpta.core.cs.ArrayIndex;
import pascal.taie.newpta.core.cs.CSCallSite;
import pascal.taie.newpta.core.cs.CSManager;
import pascal.taie.newpta.core.cs.CSMethod;
import pascal.taie.newpta.core.cs.CSObj;
import pascal.taie.newpta.core.cs.CSVar;
import pascal.taie.newpta.core.cs.InstanceField;
import pascal.taie.newpta.core.cs.Pointer;
import pascal.taie.newpta.core.cs.StaticField;
import pascal.taie.newpta.core.heap.EnvObjs;
import pascal.taie.newpta.core.heap.HeapModel;
import pascal.taie.newpta.core.heap.Obj;
import pascal.taie.newpta.plugin.Plugin;
import pascal.taie.newpta.set.PointsToSet;
import pascal.taie.newpta.set.PointsToSetFactory;
import pascal.taie.pta.PTAOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.util.CollectionUtils.newSet;

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
        return heapModel;
    }

    public void setHeapModel(HeapModel heapModel) {
        this.heapModel = heapModel;
    }

    @Override
    public CSManager getCSManager() {
        return csManager;
    }

    public void setCSManager(CSManager csManager) {
        this.csManager = csManager;
    }

    @Override
    public ContextSelector getContextSelector() {
        return contextSelector;
    }

    public void setContextSelector(ContextSelector contextSelector) {
        this.contextSelector = contextSelector;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CallGraph<CSCallSite, CSMethod> getCallGraph() {
        return callGraph;
    }

    @Override
    public Stream<CSVar> getVars() {
        return csManager.getCSVars();
    }

    @Override
    public Stream<InstanceField> getInstanceFields() {
        return csManager.getInstanceFields();
    }

    @Override
    public Stream<ArrayIndex> getArrayIndexes() {
        return csManager.getArrayIndexes();
    }

    @Override
    public Stream<StaticField> getStaticFields() {
        return csManager.getStaticFields();
    }

    /**
     * Run pointer analysis algorithm.
     */
    @Override
    public void analyze() {
        plugin.preprocess();
        initialize();
        solve();
        plugin.postprocess();
    }

     /**
     * Initializes pointer analysis.
     */
    private void initialize() {
        callGraph = new OnFlyCallGraph(csManager);
        pointerFlowGraph = new PointerFlowGraph();
        workList = new WorkList();
        reachableMethods = newSet();
//        classInitializer = new ClassInitializer();

        // process program entries (including implicit entries)
        Context defContext = contextSelector.getDefaultContext();
        for (JMethod entry : computeEntries()) {
            // initialize class type of entry methods
//            classInitializer.initializeClass(entry.getDeclaringClass());
            CSMethod csMethod = csManager.getCSMethod(defContext, entry);
            callGraph.addEntryMethod(csMethod);
//            processNewCSMethod(csMethod);
        }
        // setup main arguments
        EnvObjs envObjs = heapModel.getEnvObjs();
        Obj args = envObjs.getMainArgs();
        Obj argsElem = envObjs.getMainArgsElem();
        addPointsTo(defContext, args, defContext, argsElem);
        JMethod main = World.getMainMethod();
        addPointsTo(defContext, main.getNewIR().getParam(0), defContext, args);
        plugin.initialize();
    }

    private Collection<JMethod> computeEntries() {
        List<JMethod> entries = new ArrayList<>();
        entries.add(World.getMainMethod());
        if (PTAOptions.get().analyzeImplicitEntries()) {
            entries.addAll(World.getImplicitEntries());
        }
        return entries;
    }

    /**
     * Processes worklist entries until the worklist is empty.
     */
    private void solve() {
        while (!workList.isEmpty()) {
            while (workList.hasPointerEntries()) {
                WorkList.Entry entry = workList.pollPointerEntry();
                Pointer p = entry.pointer;
                PointsToSet pts = entry.pointsToSet;
                PointsToSet diff = propagate(p, pts);
                if (p instanceof CSVar) {
                    CSVar v = (CSVar) p;
//                    processInstanceStore(v, diff);
//                    processInstanceLoad(v, diff);
//                    processArrayStore(v, diff);
//                    processArrayLoad(v, diff);
//                    processCall(v, diff);
                    plugin.handleNewPointsToSet(v, diff);
                }
            }
            while (workList.hasCallEdges()) {
//                processCallEdge(workList.pollCallEdge());
            }
        }
        plugin.finish();
    }

    /**
     * Propagates pointsToSet to pt(pointer) and its PFG successors,
     * returns the difference set of pointsToSet and pt(pointer).
     */
    private PointsToSet propagate(Pointer pointer, PointsToSet pointsToSet) {
        logger.trace("Propagate {} to {}", pointsToSet, pointer);
        final PointsToSet diff = PointsToSetFactory.make();
        for (CSObj obj : pointsToSet) {
            if (pointer.getPointsToSet().addObject(obj)) {
                diff.addObject(obj);
            }
        }
        if (!diff.isEmpty()) {
            for (PointerFlowEdge edge : pointerFlowGraph.getOutEdgesOf(pointer)) {
                Pointer to = edge.getTo();
                // TODO: use Optional.ifPresentOrElse() after upgrade to Java 9+
                if (edge.getType().isPresent()) {
                    // Checks assignable objects
                    addPointerEntry(to,
                            getAssignablePointsToSet(diff, edge.getType().get()));
                } else {
                    addPointerEntry(to, diff);
                }
            }
        }
        return diff;
    }

    @Override
    public void addPointsTo(Context context, Var var, Context heapContext, Obj obj) {
        CSObj csObj = csManager.getCSObj(heapContext, obj);
        addPointsTo(context, var, PointsToSetFactory.make(csObj));
    }

    @Override
    public void addPointsTo(Context context, Var var, PointsToSet pts) {
        CSVar csVar = csManager.getCSVar(context, var);
        addPointerEntry(csVar, pts);
    }

    @Override
    public void addPointsTo(Context arrayContext, Obj array, Context heapContext, Obj obj) {
        CSObj csArray = csManager.getCSObj(arrayContext, array);
        ArrayIndex arrayIndex = csManager.getArrayIndex(csArray);
        CSObj elem = csManager.getCSObj(heapContext, obj);
        addPointerEntry(arrayIndex, PointsToSetFactory.make(elem));
    }

    /**
     * Add a <pointer, pointsToSet> entry to work-list.
     */
    private void addPointerEntry(Pointer pointer, PointsToSet pointsToSet) {
        workList.addPointerEntry(pointer, pointsToSet);
    }

    /**
     * Given a points-to set pts and a type t, returns the objects of pts
     * which can be assigned to t.
     */
    private PointsToSet getAssignablePointsToSet(PointsToSet pts, Type type) {
        PointsToSet result = PointsToSetFactory.make();
        pts.stream()
                .filter(o -> typeManager.isSubtype(type, o.getObject().getType()))
                .forEach(result::addObject);
        return result;
    }
}
