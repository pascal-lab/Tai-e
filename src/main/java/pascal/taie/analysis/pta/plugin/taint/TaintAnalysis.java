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

package pascal.taie.analysis.pta.plugin.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.analysis.pta.pts.PointsToSetFactory;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.MapUtils;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TaintAnalysis implements Plugin {

    private static final Logger logger = LogManager.getLogger(TaintAnalysis.class);

    private final TaintManager manager = new TaintManager();

    /**
     * Map from method (which causes taint transfer) to set of relevant
     * {@link TaintTransfer}.
     */
    private final Map<JMethod, Set<TaintTransfer>> transfers = MapUtils.newMap();

    /**
     * Map from variable to taint transfer information.
     * The taint objects pointed to by the "key" variable are supposed
     * to be transferred to "value" variable with specified type.
     */
    private final Map<Var, Set<Pair<Var, Type>>> varTransfers = MapUtils.newMap();

    private TaintConfig config;

    private Solver solver;

    private CSManager csManager;

    private Context defaultCtx;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        csManager = solver.getCSManager();
        defaultCtx = solver.getContextSelector().getDefaultContext();
        config = TaintConfig.readConfig(
                solver.getOptions().getString("taint-config"),
                solver.getHierarchy());
        config.getTransfers().forEach(t ->
                MapUtils.addToMapSet(transfers, t.getMethod(), t));
        logger.info(config);
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        Invoke callSite = edge.getCallSite().getCallSite();
        JMethod target = edge.getCallee().getMethod();
        // generate taint value from source call
        Var lhs = callSite.getLValue();
        if (lhs != null && config.getSources().contains(target)) {
            Obj taint = manager.getTaint(callSite, target.getReturnType());
            solver.addVarPointsTo(edge.getCallSite().getContext(), lhs,
                    defaultCtx, taint);
        }
        // process taint transfer
        Set<TaintTransfer> transfers = this.transfers.get(target);
        if (transfers != null) {
            transfers.forEach(transfer -> {
                Var from = getVar(callSite, transfer.getFrom());
                Var to = getVar(callSite, transfer.getTo());
                // when transfer to result variable, and the call site
                // does not have result variable, then "to" is null.
                if (to != null) {
                    Type type = target.getReturnType();
                    MapUtils.addToMapSet(varTransfers, from, new Pair<>(to, type));
                    Context ctx = edge.getCallSite().getContext();
                    CSVar csFrom = csManager.getCSVar(ctx, from);
                    transferTaint(solver.getPointsToSetOf(csFrom), ctx, to, type);
                }
            });
        }
    }

    /**
     * Retrieves variable from a call site and index.
     */
    private static Var getVar(Invoke callSite, int index) {
        InvokeExp invokeExp = callSite.getInvokeExp();
        switch (index) {
            case TaintTransfer.BASE:
                return ((InvokeInstanceExp) invokeExp).getBase();
            case TaintTransfer.RESULT:
                return callSite.getResult();
            default:
                return invokeExp.getArg(index);
        }
    }

    private void transferTaint(PointsToSet pts, Context ctx, Var to, Type type) {
        PointsToSet newTaints = PointsToSetFactory.make();
        pts.objects()
                .map(CSObj::getObject)
                .filter(manager::isTaint)
                .map(manager::getSourceCall)
                .map(source -> manager.getTaint(source, type))
                .map(taint -> csManager.getCSObj(defaultCtx, taint))
                .forEach(newTaints::addObject);
        if (!newTaints.isEmpty()) {
            solver.addVarPointsTo(ctx, to, newTaints);
        }
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        Set<Pair<Var, Type>> transfers = varTransfers.get(csVar.getVar());
        if (transfers != null) {
            transfers.forEach(p -> {
                Var to = p.getFirst();
                Type type = p.getSecond();
                transferTaint(pts, csVar.getContext(), to, type);
            });
        }
    }

    @Override
    public void onFinish() {
        List<TaintFlow> taintFlows = collectTaintFlows();
        taintFlows.forEach(logger::info);
    }

    private List<TaintFlow> collectTaintFlows() {
        PointerAnalysisResult result = solver.getResult();
        List<TaintFlow> taintFlows = new ArrayList<>();
        config.getSinks().forEach(sink -> {
            int i = sink.getIndex();
            result.getCallGraph()
                    .callersOf(sink.getMethod())
                    .forEach(sinkCall -> {
                        Var arg = sinkCall.getInvokeExp().getArg(i);
                        result.getPointsToSet(arg)
                                .stream()
                                .filter(manager::isTaint)
                                .map(manager::getSourceCall)
                                .map(sourceCall -> new TaintFlow(sourceCall, sinkCall, i))
                                .forEach(taintFlows::add);
                    });
        });
        return taintFlows.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toUnmodifiableList());
    }
}
