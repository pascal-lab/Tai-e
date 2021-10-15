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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.CallGraphs;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.element.InstanceField;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.cs.element.StaticField;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.analysis.pta.pts.PointsToSetFactory;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

/**
 * A simple solver for context-sensitive pointer analysis.
 * This solver is only for education purpose and not our main pointer analysis
 * solver. To use full-fledged solver, please refer to {@link DefaultSolver}.
 */
public class SimpleSolver extends Solver {

    private static final Logger logger = LogManager.getLogger(SimpleSolver.class);

    private WorkList workList;

    /**
     * Runs pointer analysis algorithm.
     */
    @Override
    public void solve() {
        initialize();
        doSolve();
    }

    /**
     * Initializes pointer analysis.
     */
    private void initialize() {
        callGraph = new OnFlyCallGraph(csManager);
        pointerFlowGraph = new PointerFlowGraph();
        workList = new WorkList();
        plugin.onStart();

        // process program entries (including implicit entries)
        Context defContext = contextSelector.getDefaultContext();
        JMethod main = World.getMainMethod();
        CSMethod csMethod = csManager.getCSMethod(defContext, main);
        callGraph.addEntryMethod(csMethod);
        addReachable(csMethod);
    }

    /**
     * Processes new reachable context-sensitive method.
     */
    private void addReachable(CSMethod csMethod) {
        if (callGraph.addReachableMethod(csMethod)) {
            JMethod method = csMethod.getMethod();
            StmtProcessor stmtProcessor = new StmtProcessor(csMethod);
            method.getIR()
                    .forEach(s -> s.accept(stmtProcessor));
        }
    }

    /**
     * Processes the statements in context-sensitive new reachable methods.
     */
    private class StmtProcessor implements StmtVisitor<Void> {

        private final CSMethod csMethod;

        private final Context context;

        private StmtProcessor(CSMethod csMethod) {
            this.csMethod = csMethod;
            this.context = csMethod.getContext();
        }

        @Override
        public Void visit(New stmt) {
            // obtain context-sensitive heap object
            Obj obj = heapModel.getObj(stmt);
            Context heapContext = contextSelector.selectHeapContext(csMethod, obj);
            addVarPointsTo(context, stmt.getLValue(), heapContext, obj);
            return null;
        }

        @Override
        public Void visit(Copy stmt) {
            CSVar from = csManager.getCSVar(context, stmt.getRValue());
            CSVar to = csManager.getCSVar(context, stmt.getLValue());
            addPFGEdge(from, to, PointerFlowEdge.Kind.LOCAL_ASSIGN);
            return null;
        }

        /**
         * Processes static load.
         */
        @Override
        public Void visit(LoadField stmt) {
            if (stmt.isStatic()) {
                JField field = stmt.getFieldRef().resolve();
                StaticField sfield = csManager.getStaticField(field);
                CSVar to = csManager.getCSVar(context, stmt.getLValue());
                addPFGEdge(sfield, to, PointerFlowEdge.Kind.STATIC_LOAD);
            }
            return null;
        }

        /**
         * Processes static store.
         */
        @Override
        public Void visit(StoreField stmt) {
            if (stmt.isStatic()) {
                JField field = stmt.getFieldRef().resolve();
                StaticField sfield = csManager.getStaticField(field);
                CSVar from = csManager.getCSVar(context, stmt.getRValue());
                addPFGEdge(from, sfield, PointerFlowEdge.Kind.STATIC_STORE);
            }
            return null;
        }

        /**
         * Processes static invocation.
         */
        @Override
        public Void visit(Invoke stmt) {
            if (stmt.isStatic()) {
                JMethod callee = CallGraphs.resolveCallee(null, stmt);
                CSCallSite csCallSite = csManager.getCSCallSite(context, stmt);
                Context calleeCtx = contextSelector.selectContext(csCallSite, callee);
                CSMethod csCallee = csManager.getCSMethod(calleeCtx, callee);
                Edge<CSCallSite, CSMethod> edge =
                        new Edge<>(CallKind.STATIC, csCallSite, csCallee);
                workList.addCallEdge(edge);
            }
            return null;
        }
    }

    @Override
    public void addPFGEdge(Pointer source, Pointer target, PointerFlowEdge.Kind kind) {
        if (pointerFlowGraph.addEdge(source, target, kind)) {
            if (!source.getPointsToSet().isEmpty()) {
                workList.addPointerEntry(target, source.getPointsToSet());
            }
        }
    }

    @Override
    public void addPointsTo(Pointer pointer, PointsToSet pts) {
        workList.addPointerEntry(pointer, pts);
    }

    /**
     * Processes worklist entries until the worklist is empty.
     */
    private void doSolve() {
        while (!workList.isEmpty()) {
            while (workList.hasPointerEntries()) {
                WorkList.Entry entry = workList.pollPointerEntry();
                Pointer p = entry.pointer;
                PointsToSet pts = entry.pointsToSet;
                PointsToSet diff = propagate(p, pts);
                if (p instanceof CSVar) {
                    CSVar v = (CSVar) p;
                    processInstanceStore(v, diff);
                    processInstanceLoad(v, diff);
                    processCall(v, diff);
                    plugin.onNewPointsToSet(v, diff);
                }
            }
            while (workList.hasCallEdges()) {
                processCallEdge(workList.pollCallEdge());
            }
        }
        plugin.onFinish();
    }

    /**
     * Propagates pointsToSet to pt(pointer) and its PFG successors,
     * returns the difference set of pointsToSet and pt(pointer).
     */
    private PointsToSet propagate(Pointer pointer, PointsToSet pointsToSet) {
        logger.trace("Propagate {} to {}", pointsToSet, pointer);
        final PointsToSet diff = PointsToSetFactory.make();
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
     * Processes instance stores when points-to set of the base variable changes.
     *
     * @param baseVar the base variable
     * @param pts     set of new discovered objects pointed by the variable.
     */
    private void processInstanceStore(CSVar baseVar, PointsToSet pts) {
        Context context = baseVar.getContext();
        Var var = baseVar.getVar();
        for (StoreField store : var.getStoreFields()) {
            Var fromVar = store.getRValue();
            CSVar from = csManager.getCSVar(context, fromVar);
            pts.forEach(baseObj -> {
                InstanceField instField = csManager.getInstanceField(
                        baseObj, store.getFieldRef().resolve());
                addPFGEdge(from, instField, PointerFlowEdge.Kind.INSTANCE_STORE);
            });
        }
    }

    /**
     * Processes instance loads when points-to set of the base variable changes.
     *
     * @param baseVar the base variable
     * @param pts     set of new discovered objects pointed by the variable.
     */
    private void processInstanceLoad(CSVar baseVar, PointsToSet pts) {
        Context context = baseVar.getContext();
        Var var = baseVar.getVar();
        for (LoadField load : var.getLoadFields()) {
            Var toVar = load.getLValue();
            CSVar to = csManager.getCSVar(context, toVar);
            pts.forEach(baseObj -> {
                InstanceField instField = csManager.getInstanceField(
                        baseObj, load.getFieldRef().resolve());
                addPFGEdge(instField, to, PointerFlowEdge.Kind.INSTANCE_LOAD);
            });
        }
    }

    /**
     * Processes instance calls when points-to set of the receiver variable changes.
     *
     * @param recv the receiver variable
     * @param pts  set of new discovered objects pointed by the variable.
     */
    private void processCall(CSVar recv, PointsToSet pts) {
        Context context = recv.getContext();
        Var var = recv.getVar();
        for (Invoke callSite : var.getInvokes()) {
            pts.forEach(recvObj -> {
                // resolve callee
                JMethod callee = CallGraphs.resolveCallee(
                        recvObj.getObject().getType(), callSite);
                // select context
                CSCallSite csCallSite = csManager.getCSCallSite(context, callSite);
                Context calleeContext = contextSelector.selectContext(
                        csCallSite, recvObj, callee);
                // build call edge
                CSMethod csCallee = csManager.getCSMethod(calleeContext, callee);
                workList.addCallEdge(new Edge<>(CallGraphs.getCallKind(callSite),
                        csCallSite, csCallee));
                // pass receiver object to *this* variable
                CSVar thisVar = csManager.getCSVar(
                        calleeContext, callee.getIR().getThis());
                workList.addPointerEntry(thisVar, PointsToSetFactory.make(recvObj));
            });
        }
    }

    /**
     * Processes the call edges in work list.
     */
    private void processCallEdge(Edge<CSCallSite, CSMethod> edge) {
        if (!callGraph.containsEdge(edge)) {
            callGraph.addEdge(edge);
            CSMethod csCallee = edge.getCallee();
            addReachable(csCallee);

            Context callerCtx = edge.getCallSite().getContext();
            Invoke callSite = edge.getCallSite().getCallSite();
            Context calleeCtx = csCallee.getContext();
            JMethod callee = csCallee.getMethod();
            InvokeExp invokeExp = callSite.getInvokeExp();
            // pass arguments to parameters
            for (int i = 0; i < invokeExp.getArgCount(); ++i) {
                Var arg = invokeExp.getArg(i);
                Var param = callee.getIR().getParam(i);
                CSVar argVar = csManager.getCSVar(callerCtx, arg);
                CSVar paramVar = csManager.getCSVar(calleeCtx, param);
                addPFGEdge(argVar, paramVar, PointerFlowEdge.Kind.PARAMETER_PASSING);
            }
            // pass results to LHS variable
            Var lhs = callSite.getResult();
            if (lhs != null) {
                CSVar csLHS = csManager.getCSVar(callerCtx, lhs);
                for (Var ret : callee.getIR().getReturnVars()) {
                    CSVar csRet = csManager.getCSVar(calleeCtx, ret);
                    addPFGEdge(csRet, csLHS, PointerFlowEdge.Kind.RETURN);
                }
            }

            plugin.onNewCallEdge(edge);
        }
    }

    // ---------- unused APIs ----------
    @Override
    public void addPFGEdge(Pointer source, Pointer target, Type type, PointerFlowEdge.Kind kind) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addCallEdge(Edge<CSCallSite, CSMethod> edge) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addCSMethod(CSMethod csMethod) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void initializeClass(JClass cls) {
        throw new UnsupportedOperationException();
    }
}
