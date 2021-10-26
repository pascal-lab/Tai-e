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

package pascal.taie.analysis.pta.ci;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.CallGraphs;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.DefaultCallGraph;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.AnalysisException;

import java.util.List;

class Solver {

    private static final Logger logger = LogManager.getLogger(Solver.class);

    private final HeapModel heapModel;

    private DefaultCallGraph callGraph;

    private PointerFlowGraph pointerFlowGraph;

    private WorkList workList;

    private StmtProcessor stmtProcessor;

    private ClassHierarchy hierarchy;

    Solver(HeapModel heapModel) {
        this.heapModel = heapModel;
    }

    /**
     * Runs pointer analysis algorithm.
     */
    void solve() {
        initialize();
        analyze();
    }

    /**
     * Initializes pointer analysis.
     */
    private void initialize() {
        workList = new WorkList();
        pointerFlowGraph = new PointerFlowGraph();
        callGraph = new DefaultCallGraph();
        stmtProcessor = new StmtProcessor();
        hierarchy = World.getClassHierarchy();
        // initialize main method
        JMethod main = World.getMainMethod();
        callGraph.addEntryMethod(main);
        addReachable(main);
    }

    /**
     * Processes new reachable method.
     */
    private void addReachable(JMethod method) {
        if (callGraph.addReachableMethod(method)) {
            method.getIR()
                    .forEach(s -> s.accept(stmtProcessor));
        }
    }

    /**
     * Processes statements in new reachable methods.
     */
    private class StmtProcessor implements StmtVisitor<Void> {

        @Override
        public Void visit(New stmt) {
            VarPtr lhs = pointerFlowGraph.getVarPtr(stmt.getLValue());
            Obj obj = heapModel.getObj(stmt);
            workList.addEntry(lhs, new PointsToSet(obj));
            return null;
        }

        @Override
        public Void visit(Copy stmt) {
            VarPtr lhs = pointerFlowGraph.getVarPtr(stmt.getLValue());
            VarPtr rhs = pointerFlowGraph.getVarPtr(stmt.getRValue());
            addPFGEdge(rhs, lhs);
            return null;
        }

        @Override
        public Void visit(LoadField stmt) {
            if (stmt.isStatic()) {
                VarPtr lhs = pointerFlowGraph.getVarPtr(stmt.getLValue());
                StaticField staticField = pointerFlowGraph
                        .getStaticField(stmt.getFieldRef().resolve());
                addPFGEdge(staticField, lhs);
            }
            return null;
        }

        @Override
        public Void visit(StoreField stmt) {
            if (stmt.isStatic()) {
                StaticField staticField = pointerFlowGraph
                        .getStaticField(stmt.getFieldRef().resolve());
                VarPtr rhs = pointerFlowGraph.getVarPtr(stmt.getRValue());
                addPFGEdge(rhs, staticField);
            }
            return null;
        }

        @Override
        public Void visit(Invoke stmt) {
            if (stmt.isStatic()) {
                JMethod callee = resolveCallee(null, stmt);
                processCallEdge(new Edge<>(CallKind.STATIC, stmt, callee));
            }
            return null;
        }
    }

    /**
     * Processes work-list entries until the work-list is empty.
     */
    private void analyze() {
        while (!workList.isEmpty()) {
            WorkList.Entry entry = workList.pollEntry();
            Pointer p = entry.pointer;
            PointsToSet pts = entry.pointsToSet;
            PointsToSet diff = propagate(p, pts);
            if (p instanceof VarPtr) {
                VarPtr vp = (VarPtr) p;
                Var v = vp.getVar();
                for (Obj o : diff) {
                    processInstanceStore(v, o);
                    processInstanceLoad(v, o);
                    processArrayStore(v, o);
                    processArrayLoad(v, o);
                    processCall(v, o);
                }
            }
        }
    }

    /**
     * Propagates pointsToSet to pt(pointer) and its PFG successors,
     * returns the difference set of pointsToSet and pt(pointer).
     */
    private PointsToSet propagate(Pointer pointer, PointsToSet pointsToSet) {
        logger.trace("Propagate {} to {}", pointsToSet, pointer);
        PointsToSet diff = new PointsToSet();
        pointsToSet.forEach(obj -> {
            if (pointer.getPointsToSet().addObject(obj)) {
                diff.addObject(obj);
            }
        });
        if (!diff.isEmpty()) {
            pointerFlowGraph.succsOf(pointer)
                    .forEach(succ -> workList.addEntry(succ, diff));
        }
        return diff;
    }

    /**
     * Adds an edge "source -> target" to the PFG.
     */
    private void addPFGEdge(Pointer source, Pointer target) {
        if (pointerFlowGraph.addEdge(source, target)) {
            if (!source.getPointsToSet().isEmpty()) {
                workList.addEntry(target, source.getPointsToSet());
            }
        }
    }

    /**
     * Processes instance stores when points-to set of the base variable changes.
     *
     * @param var the base variable
     * @param base new discovered object pointed by the variable.
     */
    private void processInstanceStore(Var var, Obj base) {
        for (StoreField store : var.getStoreFields()) {
            VarPtr fromPtr = pointerFlowGraph.getVarPtr(store.getRValue());
            InstanceField instanceField = pointerFlowGraph.getInstanceField(
                    base, store.getFieldRef().resolve());
            addPFGEdge(fromPtr, instanceField);
        }
    }

    /**
     * Processes instance loads when points-to set of the base variable changes.
     *
     * @param var the base variable
     * @param base new discovered object pointed by the variable.
     */
    private void processInstanceLoad(Var var, Obj base) {
        for (LoadField load : var.getLoadFields()) {
            VarPtr toPtr = pointerFlowGraph.getVarPtr(load.getLValue());
            InstanceField instanceField = pointerFlowGraph.getInstanceField(
                    base, load.getFieldRef().resolve());
            addPFGEdge(instanceField, toPtr);
        }
    }

    /**
     * Processes array stores when points-to set of the base variable changes.
     *
     * @param var the base variable
     * @param array new discovered array object pointed by the variable.
     */
    private void processArrayStore(Var var, Obj array) {
        for (StoreArray store : var.getStoreArrays()) {
            VarPtr fromPtr = pointerFlowGraph.getVarPtr(store.getRValue());
            ArrayIndex arrayIndex = pointerFlowGraph.getArrayIndex(array);
            addPFGEdge(fromPtr, arrayIndex);
        }
    }

    /**
     * Processes array loads when points-to set of the base variable changes.
     *
     * @param var the base variable
     * @param array new discovered array object pointed by the variable.
     */
    private void processArrayLoad(Var var, Obj array) {
        for (LoadArray load : var.getLoadArrays()) {
            VarPtr toPtr = pointerFlowGraph.getVarPtr(load.getLValue());
            ArrayIndex arrayIndex = pointerFlowGraph.getArrayIndex(array);
            addPFGEdge(arrayIndex, toPtr);
        }
    }

    /**
     * Processes instance calls when points-to set of the receiver variable changes.
     *
     * @param var the variable that holds receiver objects
     * @param recv a new discovered object pointed by the variable
     */
    private void processCall(Var var, Obj recv) {
        for (Invoke callSite : var.getInvokes()) {
            // pass receiver object to this variable
            JMethod callee = resolveCallee(recv, callSite);
            VarPtr thisPtr = pointerFlowGraph.getVarPtr(
                    callee.getIR().getThis());
            PointsToSet recvPts = new PointsToSet(recv);
            workList.addEntry(thisPtr, recvPts);
            // build call edge
            processCallEdge(new Edge<>(
                    CallGraphs.getCallKind(callSite), callSite, callee));
        }
    }

    /**
     * Resolves the callee of a call site with the receiver object.
     *
     * @param recv the receiver object of the method call. If the callSite
     *             is static, this parameter is ignored (i.e., can be null).
     * @param callSite the call site to be resolved.
     * @return the resolved callee.
     */
    private JMethod resolveCallee(Obj recv, Invoke callSite) {
        MethodRef methodRef = callSite.getMethodRef();
        if (callSite.isInterface() || callSite.isVirtual()) {
            return hierarchy.dispatch(recv.getType(), methodRef);
        } else if (callSite.isSpecial() || callSite.isStatic()) {
            return methodRef.resolveNullable();
        } else {
            throw new AnalysisException("Cannot resolve Invoke: " + callSite);
        }
    }

    /**
     * Process the call edges in work list.
     */
    private void processCallEdge(Edge<Invoke, JMethod> edge) {
        if (callGraph.addEdge(edge)) {
            JMethod callee = edge.getCallee();
            addReachable(callee);
            Invoke callSite = edge.getCallSite();
            InvokeExp invokeExp = callSite.getInvokeExp();
            // pass arguments to parameters
            List<Var> args = invokeExp.getArgs();
            List<Var> params = callee.getIR().getParams();
            for (int i = 0; i < args.size(); ++i) {
                VarPtr arg = pointerFlowGraph.getVarPtr(args.get(i));
                VarPtr param = pointerFlowGraph.getVarPtr(params.get(i));
                addPFGEdge(arg, param);
            }
            // pass results to LHS variable
            if (callSite.getLValue() != null) {
                VarPtr lhsPtr = pointerFlowGraph.getVarPtr(callSite.getLValue());
                for (Var ret : callee.getIR().getReturnVars()) {
                    VarPtr retPtr = pointerFlowGraph.getVarPtr(ret);
                    addPFGEdge(retPtr, lhsPtr);
                }
            }
        }
    }

    CIPTAResult getResult() {
        return new CIPTAResult(pointerFlowGraph, callGraph);
    }
}
