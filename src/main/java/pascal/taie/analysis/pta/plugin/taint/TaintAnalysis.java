/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.pta.plugin.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.callgraph.CallKind;
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
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class TaintAnalysis implements Plugin {

    private static final Logger logger = LogManager.getLogger(TaintAnalysis.class);

    /**
     * Map from a source method to its result sources.
     */
    private final MultiMap<JMethod, CallSource> callSources = Maps.newMultiMap();

    /**
     * Map from a source method to its parameter sources.
     */
    private final MultiMap<JMethod, ParamSource> paramSources = Maps.newMultiMap();

    /**
     * Map from method (which causes taint transfer) to set of relevant
     * {@link TaintTransfer}.
     */
    private final MultiMap<JMethod, TaintTransfer> transfers = Maps.newMultiMap();

    /**
     * Map from variable to taint transfer information.
     * The taint objects pointed to by the "key" variable are supposed
     * to be transferred to "value" variable with specified type.
     */
    private final MultiMap<Var, Pair<Var, Type>> varTransfers = Maps.newMultiMap();

    /**
     * Whether enable taint back propagation to handle aliases about
     * tainted mutable objects, e.g., char[].
     */
    private final boolean enableBackPropagate = true;

    /**
     * Cache statements generated for back propagation.
     */
    private final Map<Var, List<Stmt>> backPropStmts = Maps.newMap();

    /**
     * Counter for generating temporary variables.
     */
    private int counter = 0;

    private Solver solver;

    private CSManager csManager;

    private Context emptyContext;

    private TaintManager manager;

    private TaintConfig config;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        csManager = solver.getCSManager();
        emptyContext = solver.getContextSelector().getEmptyContext();
        manager = new TaintManager(solver.getHeapModel());
        config = TaintConfig.readConfig(
                solver.getOptions().getString("taint-config"),
                solver.getHierarchy(),
                solver.getTypeSystem());
        logger.info(config);
        config.callSources().forEach(s -> callSources.put(s.method(), s));
        config.paramSources().forEach(s -> paramSources.put(s.method(), s));
        config.transfers().forEach(t -> transfers.put(t.method(), t));
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        // process ResultSource and taint transfer
        Invoke callSite = edge.getCallSite().getCallSite();
        JMethod callee = edge.getCallee().getMethod();
        // generate taint value from source call
        callSources.get(callee).forEach(source -> {
            int index = source.index();
            if (IndexUtils.RESULT == index && callSite.getLValue() == null ||
                    IndexUtils.RESULT != index && edge.getKind() == CallKind.OTHER) {
                return;
            }
            Var var = IndexUtils.getVar(callSite, index);
            SourcePoint sourcePoint = new CallSourcePoint(callSite, index);
            Obj taint = manager.makeTaint(sourcePoint, source.type());
            solver.addVarPointsTo(edge.getCallSite().getContext(), var,
                    emptyContext, taint);
        });
        // process taint transfer
        if (edge.getKind() == CallKind.OTHER) {
            // skip other call edges, e.g., reflective call edges,
            // which currently cannot be handled for transfer methods
            // TODO: handle other call edges
            return;
        }
        transfers.get(callee).forEach(transfer -> {
            Var from = IndexUtils.getVar(callSite, transfer.from());
            Var to = IndexUtils.getVar(callSite, transfer.to());
            // when transfer to result variable, and the call site
            // does not have result variable, then "to" is null.
            if (to != null) {
                Type type = transfer.type();
                varTransfers.put(from, new Pair<>(to, type));
                Context ctx = edge.getCallSite().getContext();
                CSVar csFrom = csManager.getCSVar(ctx, from);
                transferTaint(solver.getPointsToSetOf(csFrom), ctx, to, type);

                // If the taint is transferred to base or argument, it means
                // that the objects pointed to by "to" were mutated
                // by the invocation. For such cases, we need to propagate the
                // taint to the pointers aliased with "to". The pointers
                // whose objects come from "to" will be naturally handled by
                // pointer analysis, and we just need to specially handle the
                // pointers whose objects flow to "to", i.e., back propagation.
                if (enableBackPropagate
                        && transfer.to() != IndexUtils.RESULT
                        && !(transfer.to() == IndexUtils.BASE
                        && transfer.method().isConstructor())) {
                    backPropagateTaint(to, ctx);
                }
            }
        });
    }

    private void transferTaint(PointsToSet pts, Context ctx, Var to, Type type) {
        PointsToSet newTaints = solver.makePointsToSet();
        pts.objects()
                .map(CSObj::getObject)
                .filter(manager::isTaint)
                .map(manager::getSourcePoint)
                .map(source -> manager.makeTaint(source, type))
                .map(taint -> csManager.getCSObj(emptyContext, taint))
                .forEach(newTaints::addObject);
        if (!newTaints.isEmpty()) {
            solver.addVarPointsTo(ctx, to, newTaints);
        }
    }

    private void backPropagateTaint(Var to, Context ctx) {
        CSMethod csMethod = csManager.getCSMethod(ctx, to.getMethod());
        solver.addStmts(csMethod,
                backPropStmts.computeIfAbsent(to, this::getBackPropagateStmts));
    }

    private List<Stmt> getBackPropagateStmts(Var var) {
        // Currently, we handle one case, i.e., var = base.field where
        // var is tainted, and we back propagate taint from var to base.field.
        // For simplicity, we add artificial statement like base.field = var
        // for back propagation.
        JMethod container = var.getMethod();
        List<Stmt> stmts = new ArrayList<>();
        container.getIR().forEach(stmt -> {
            if (stmt instanceof LoadField load) {
                FieldAccess fieldAccess = load.getFieldAccess();
                if (fieldAccess instanceof InstanceFieldAccess ifa) {
                    // found var = base.field;
                    Var base = ifa.getBase();
                    // generate a temp base to avoid polluting original base
                    Var taintBase = getTempVar(container, base.getType());
                    stmts.add(new Copy(taintBase, base)); // %taint-temp = base;
                    // generate field store statements to back propagate taint
                    Var from;
                    Type fieldType = ifa.getType();
                    // since var may point to the objects that are not from
                    // base.field, we use type to filter some spurious objects
                    if (fieldType.equals(var.getType())) {
                        from = var;
                    } else {
                        Var tempFrom = getTempVar(container, fieldType);
                        stmts.add(new Cast(tempFrom, new CastExp(var, fieldType)));
                        from = tempFrom;
                    }
                    // back propagate taint from var to base.field
                    stmts.add(new StoreField(
                            new InstanceFieldAccess(ifa.getFieldRef(), taintBase),
                            from)); // %taint-temp.field = from;
                }
            }
        });
        return stmts.isEmpty() ? List.of() : stmts;
    }

    private Var getTempVar(JMethod container, Type type) {
        String varName = "%taint-temp-" + counter++;
        return new Var(container, varName, type, -1);
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        // process ParamSource
        JMethod method = csMethod.getMethod();
        if (paramSources.containsKey(method)) {
            Context context = csMethod.getContext();
            IR ir = method.getIR();
            paramSources.get(method).forEach(source -> {
                int index = source.index();
                Var param = ir.getParam(index);
                SourcePoint sourcePoint = new ParamSourcePoint(method, index);
                Type type = source.type();
                Obj taint = manager.makeTaint(sourcePoint, type);
                solver.addVarPointsTo(context, param, emptyContext, taint);
            });
        }
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        // process taint transfer
        varTransfers.get(csVar.getVar()).forEach(p -> {
            Var to = p.first();
            Type type = p.second();
            transferTaint(pts, csVar.getContext(), to, type);
        });
    }

    @Override
    public void onFinish() {
        Set<TaintFlow> taintFlows = collectTaintFlows();
        solver.getResult().storeResult(getClass().getName(), taintFlows);
        logger.info("Detected {} taint flow(s):", taintFlows.size());
        taintFlows.forEach(logger::info);
    }

    private Set<TaintFlow> collectTaintFlows() {
        PointerAnalysisResult result = solver.getResult();
        Set<TaintFlow> taintFlows = new TreeSet<>();
        config.sinks().forEach(sink -> {
            int i = sink.index();
            result.getCallGraph()
                    .edgesInTo(sink.method())
                    // TODO: handle other call edges
                    .filter(e -> e.getKind() != CallKind.OTHER)
                    .map(Edge::getCallSite)
                    .forEach(sinkCall -> {
                        Var arg = IndexUtils.getVar(sinkCall, i);
                        SinkPoint sinkPoint = new SinkPoint(sinkCall, i);
                        result.getPointsToSet(arg)
                                .stream()
                                .filter(manager::isTaint)
                                .map(manager::getSourcePoint)
                                .map(sourcePoint -> new TaintFlow(sourcePoint, sinkPoint))
                                .forEach(taintFlows::add);
                    });
        });
        return taintFlows;
    }
}
