/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C)  2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C)  2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.analysis.ci;

import bamboo.callgraph.CallGraph;
import bamboo.callgraph.CallKind;
import bamboo.callgraph.Edge;
import bamboo.pta.analysis.ProgramManager;
import bamboo.pta.analysis.heap.HeapModel;
import bamboo.pta.element.CallSite;
import bamboo.pta.element.Method;
import bamboo.pta.element.Obj;
import bamboo.pta.element.Type;
import bamboo.pta.element.Variable;
import bamboo.pta.statement.Allocation;
import bamboo.pta.statement.Assign;
import bamboo.pta.statement.Call;
import bamboo.pta.statement.InstanceLoad;
import bamboo.pta.statement.InstanceStore;
import bamboo.pta.statement.Statement;
import bamboo.util.AnalysisException;

import java.util.List;
import java.util.stream.Stream;

public class PointerAnalysis {

    private ProgramManager programManager;

    private OnFlyCallGraph callGraph;

    private PointerFlowGraph pointerFlowGraph;

    private HeapModel heapModel;

    private WorkList workList;

    public void setProgramManager(ProgramManager programManager) {
        this.programManager = programManager;
    }

    public ProgramManager getProgramManager() {
        return programManager;
    }

    public void setHeapModel(HeapModel heapModel) {
        this.heapModel = heapModel;
    }

    public CallGraph<CallSite, Method> getCallGraph() {
        return callGraph;
    }

    public Stream<Var> getVariables() {
        return pointerFlowGraph.getPointers()
                .stream()
                .filter(p -> p instanceof Var)
                .map(p -> (Var) p);
    }

    public Stream<InstanceField> getInstanceFields() {
        return pointerFlowGraph.getPointers()
                .stream()
                .filter(p -> p instanceof bamboo.pta.analysis.ci.InstanceField)
                .map(p -> (InstanceField) p);
    }

    /**
     * Runs pointer analysis algorithm.
     */
    public void solve() {
        initialize();
        analyze();
    }

    /**
     * Initializes pointer analysis.
     */
    private void initialize() {
        callGraph = new OnFlyCallGraph();
        pointerFlowGraph = new PointerFlowGraph();
        workList = new WorkList();
        for (Method entry : programManager.getEntryMethods()) {
            addReachable(entry);
            // must be called after addReachable()
            callGraph.addEntryMethod(entry);
        }
    }

    /**
     * Processes worklist entries until the worklist is empty.
     */
    private void analyze() {
        while (!workList.isEmpty()) {
            while (workList.hasPointerEntries()) {
                WorkList.Entry entry = workList.pollPointerEntry();
                Pointer p = entry.pointer;
                PointsToSet pts = entry.pointsToSet;
                PointsToSet diff = propagate(p, pts);
                if (p instanceof Var) {
                    Var v = (Var) p;
                    processInstanceStore(v, diff);
                    processInstanceLoad(v, diff);
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
//         System.out.println("Propagate "
//                 + Stringify.pointsToSetToString(pointsToSet)
//                 + " to " + Stringify.pointerToString(pointer));
        PointsToSet diff = new PointsToSet();
        for (Obj obj : pointsToSet) {
            if (pointer.getPointsToSet().addObject(obj)) {
                diff.addObject(obj);
            }
        }
        if (!diff.isEmpty()) {
            for (Pointer succ : pointerFlowGraph.getSuccessorsOf(pointer)) {
                workList.addPointerEntry(succ, diff);
            }
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
     * Processes new reachable method.
     */
    private void addReachable(Method method) {
        if (callGraph.addNewMethod(method)) {
            processAllocations(method);
            processLocalAssign(method);
            processStaticCalls(method);
        }
    }

    /**
     * Processes allocations (new statements) in the given method.
     */
    private void processAllocations(Method method) {
        for (Statement stmt : method.getStatements()) {
            if (stmt instanceof Allocation) {
                Allocation alloc = (Allocation) stmt;
                // obtain abstract object
                Obj obj = heapModel.getObj(alloc);
                // obtain lhs variable
                Var lhs = pointerFlowGraph.getVar(alloc.getVar());
                workList.addPointerEntry(lhs, new PointsToSet(obj));
            }
        }
    }

    /**
     * Adds local assign edges of the given method to pointer flow graph.
     */
    private void processLocalAssign(Method method) {
        for (Statement stmt : method.getStatements()) {
            if (stmt instanceof Assign) {
                Assign assign = (Assign) stmt;
                Var from = pointerFlowGraph.getVar(assign.getFrom());
                Var to = pointerFlowGraph.getVar(assign.getTo());
                addPFGEdge(from, to);
            }
        }
    }

    /**
     * Processes instance stores when points-to set of the base variable changes.
     * @param baseVar the base variable
     * @param pts set of new discovered objects pointed by the variable.
     */
    private void processInstanceStore(Var baseVar, PointsToSet pts) {
        Variable var = baseVar.getVariable();
        for (InstanceStore store : var.getStores()) {
            Var from = pointerFlowGraph.getVar(store.getFrom());
            for (Obj baseObj : pts) {
                InstanceField instField = pointerFlowGraph.getInstanceField(
                        baseObj, store.getField());
                addPFGEdge(from, instField);
            }
        }
    }

    /**
     * Processes instance loads when points-to set of the base variable changes.
     * @param baseVar the base variable
     * @param pts set of new discovered objects pointed by the variable.
     */
    private void processInstanceLoad(Var baseVar, PointsToSet pts) {
        Variable var = baseVar.getVariable();
        for (InstanceLoad load : var.getLoads()) {
            Var to = pointerFlowGraph.getVar(load.getTo());
            for (Obj baseObj : pts) {
                InstanceField instField = pointerFlowGraph.getInstanceField(
                        baseObj, load.getField());
                addPFGEdge(instField, to);
            }
        }
    }

    /**
     * Processes instance calls when points-to set of the receiver variable changes.
     * @param recv the receiver variable
     * @param pts set of new discovered objects pointed by the variable.
     */
    private void processCall(Var recv, PointsToSet pts) {
        Variable var = recv.getVariable();
        for (Call call : var.getCalls()) {
            CallSite callSite = call.getCallSite();
            for (Obj recvObj : pts) {
                // resolve callee
                Method callee = resolveCallee(recvObj, callSite);
                // pass receiver object to *this* variable
                Var thisVar = pointerFlowGraph.getVar(callee.getThis());
                workList.addPointerEntry(thisVar, new PointsToSet(recvObj));
                // build call edge
                CallKind callKind = getCallKind(callSite);
                workList.addCallEdge(new Edge<>(callKind, callSite, callee));
            }
        }
    }

    /**
     * Process the call edges in work list.
     */
    private void processCallEdge(Edge<CallSite, Method> edge) {
        if (!callGraph.containsEdge(edge)) {
            callGraph.addEdge(edge);
            Method callee = edge.getCallee();
            addReachable(callee);
            CallSite callSite = edge.getCallSite();
            // pass arguments to parameters
            List<Variable> args = callSite.getArguments();
            List<Variable> params = callee.getParameters();
            for (int i = 0; i < args.size(); ++i) {
                Var arg = pointerFlowGraph.getVar(args.get(i));
                Var param = pointerFlowGraph.getVar(params.get(i));
                addPFGEdge(arg, param);
            }
            // pass results to LHS variable
            if (callSite.getCall().getLHS() != null) {
                Var lhs = pointerFlowGraph.getVar(callSite.getCall().getLHS());
                for (Variable ret : callee.getReturnVariables()) {
                    Var retVar = pointerFlowGraph.getVar(ret);
                    addPFGEdge(retVar, lhs);
                }
            }
        }
    }

    /**
     * Process static calls in given method.
     */
    private void processStaticCalls(Method method) {
        for (Statement stmt : method.getStatements()) {
            if (stmt instanceof Call) {
                CallSite callSite = ((Call) stmt).getCallSite();
                if (callSite.isStatic()) {
                    Method callee = callSite.getMethod();
                    Edge<CallSite, Method> edge =
                            new Edge<>(CallKind.STATIC, callSite, callee);
                    workList.addCallEdge(edge);
                }
            }
        }
    }

    /**
     * Resolves callee by given receiver object and call site.
     */
    private Method resolveCallee(Obj recvObj, CallSite callSite) {
        Type type = recvObj.getType();
        if (callSite.isInterface() || callSite.isVirtual()) {
            return programManager.resolveInterfaceOrVirtualCall(
                    type, callSite.getMethod());
        } else if (callSite.isSpecial()){
            return programManager.resolveSpecialCall(
                    callSite, callSite.getContainerMethod());
        } else {
            throw new AnalysisException("Unknown CallSite: " + callSite);
        }
    }

    /**
     * Returns CallKind of the given call site.
     */
    private CallKind getCallKind(CallSite callSite) {
        if (callSite.isInterface()) {
            return CallKind.INTERFACE;
        } else if (callSite.isVirtual()) {
            return CallKind.VIRTUAL;
        } else if (callSite.isSpecial()) {
            return CallKind.SPECIAL;
        } else if (callSite.isStatic()) {
            return CallKind.STATIC;
        } else {
            throw new AnalysisException("Unknown call site: " + callSite);
        }
    }
}
