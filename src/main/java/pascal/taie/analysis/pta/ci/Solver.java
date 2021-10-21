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
import pascal.taie.language.type.Type;
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

    private class StmtProcessor implements StmtVisitor<Void> {

        @Override
        public Void visit(New stmt) {
            VarPtr lhs = pointerFlowGraph.getVarPtr(stmt.getLValue());
            Obj obj = heapModel.getObj(stmt);
            workList.addPointerEntry(lhs, new PointsToSet(obj));
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
        public Void visit(Invoke stmt) {
            if (stmt.isStatic()) {
                JMethod callee = resolveCallee(null, stmt);
                workList.addCallEdge(
                        new Edge<>(CallKind.STATIC, stmt, callee));
            }
            return null;
        }
    }

    /**
     * Processes work-list entries until the work-list is empty.
     */
    private void analyze() {
        while (!workList.isEmpty()) {
            while (workList.hasPointerEntries()) {
                WorkList.Entry entry = workList.pollPointerEntry();
                Pointer p = entry.pointer;
                PointsToSet pts = entry.pointsToSet;
                PointsToSet diff = propagate(p, pts);
                if (p instanceof VarPtr) {
                    VarPtr vp = (VarPtr) p;
                    Var v = vp.getVar();
                    processInstanceStore(v, diff);
                    processInstanceLoad(v, diff);
                    processArrayStore(v, diff);
                    processArrayLoad(v, diff);
                    processCall(v, diff);
                }
            }
            while (workList.hasCallEdges()) {
                processCallEdge(workList.pollCallEdge());
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
                    .forEach(succ -> workList.addPointerEntry(succ, diff));
        }
        return diff;
    }

    /**
     * Adds an edge "from -> to" to the PFG.
     */
    private void addPFGEdge(Pointer from, Pointer to) {
        if (pointerFlowGraph.addEdge(from, to)) {
            if (!from.getPointsToSet().isEmpty()) {
                workList.addPointerEntry(to, from.getPointsToSet());
            }
        }
    }

    /**
     * Processes instance stores when points-to set of the base variable changes.
     *
     * @param var the base variable
     * @param pts set of new discovered objects pointed by the variable.
     */
    private void processInstanceStore(Var var, PointsToSet pts) {
        for (StoreField store : var.getStoreFields()) {
            VarPtr fromPtr = pointerFlowGraph.getVarPtr(store.getRValue());
            pts.forEach(baseObj -> {
                InstanceField instanceField = pointerFlowGraph.getInstanceField(
                        baseObj, store.getFieldRef().resolve());
                addPFGEdge(fromPtr, instanceField);
            });
        }
    }

    /**
     * Processes instance loads when points-to set of the base variable changes.
     *
     * @param var the base variable
     * @param pts set of new discovered objects pointed by the variable.
     */
    private void processInstanceLoad(Var var, PointsToSet pts) {
        for (LoadField load : var.getLoadFields()) {
            VarPtr toPtr = pointerFlowGraph.getVarPtr(load.getLValue());
            pts.forEach(baseObj -> {
                InstanceField instanceField = pointerFlowGraph.getInstanceField(
                        baseObj, load.getFieldRef().resolve());
                addPFGEdge(instanceField, toPtr);
            });
        }
    }

    /**
     * Processes array stores when points-to set of the base variable changes.
     *
     * @param var the base variable
     * @param pts set of new discovered objects pointed by the variable.
     */
    private void processArrayStore(Var var, PointsToSet pts) {
        for (StoreArray store : var.getStoreArrays()) {
            VarPtr fromPtr = pointerFlowGraph.getVarPtr(store.getRValue());
            pts.forEach(array -> {
                ArrayIndex arrayIndex = pointerFlowGraph.getArrayIndex(array);
                addPFGEdge(fromPtr, arrayIndex);
            });
        }
    }

    /**
     * Processes array loads when points-to set of the base variable changes.
     *
     * @param var the base variable
     * @param pts set of new discovered objects pointed by the variable.
     */
    private void processArrayLoad(Var var, PointsToSet pts) {
        for (LoadArray load : var.getLoadArrays()) {
            VarPtr toPtr = pointerFlowGraph.getVarPtr(load.getLValue());
            pts.forEach(array -> {
                ArrayIndex arrayIndex = pointerFlowGraph.getArrayIndex(array);
                addPFGEdge(arrayIndex, toPtr);
            });
        }
    }

    /**
     * Processes instance calls when points-to set of the receiver variable changes.
     *
     * @param var the receiver variable
     * @param pts set of new discovered objects pointed by the variable.
     */
    private void processCall(Var var, PointsToSet pts) {
        for (Invoke callSite : var.getInvokes()) {
            pts.forEach(recvObj -> {
                // build call edge
                JMethod callee = resolveCallee(recvObj.getType(), callSite);
                workList.addCallEdge(new Edge<>(CallGraphs.getCallKind(callSite),
                        callSite, callee));
                // pass receiver object to this variable
                VarPtr thisPtr = pointerFlowGraph.getVarPtr(callee.getIR().getThis());
                PointsToSet recvPts = new PointsToSet(recvObj);
                workList.addPointerEntry(thisPtr, recvPts);
            });
        }
    }

    private JMethod resolveCallee(Type type, Invoke callSite) {
        MethodRef methodRef = callSite.getMethodRef();
        if (callSite.isInterface() || callSite.isVirtual()) {
            return hierarchy.dispatch(type, methodRef);
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
