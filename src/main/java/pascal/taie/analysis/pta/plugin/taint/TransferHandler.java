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
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Transfer;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
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
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles taint transfers in taint analysis.
 */
class TransferHandler extends OnFlyHandler {

    private static final Logger logger = LogManager.getLogger(TransferHandler.class);

    private final Context emptyContext;

    /**
     * Map from method (which causes taint transfer) to set of relevant
     * {@link TaintTransfer}.
     */
    private final MultiMap<JMethod, TaintTransfer> transfers = Maps.newMultiMap();

    private final Map<Type, Transfer> transferFunctions = Maps.newHybridMap();

    private enum Kind {
        VAR_TO_ARRAY, VAR_TO_FIELD, ARRAY_TO_VAR, FIELD_TO_VAR
    }

    private record TransferInfo(Kind kind, Var var, TaintTransfer transfer) {
    }

    private final MultiMap<Var, TransferInfo> transferInfos = Maps.newMultiMap();

    /**
     * Map from a method to {@link Invoke} statements in the method
     * which matches any transfer method.
     * This map matters only when call-site mode is enabled.
     */
    private final MultiMap<JMethod, Invoke> callSiteTransfers = Maps.newMultiMap();

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

    TransferHandler(HandlerContext context) {
        super(context);
        emptyContext = solver.getContextSelector().getEmptyContext();
        context.config().transfers()
                .forEach(t -> this.transfers.put(t.method(), t));
    }

    private void processTransfer(Context context, Invoke callSite, TaintTransfer transfer) {
        IndexRef from = transfer.from();
        IndexRef to = transfer.to();
        Var toVar = InvokeUtils.getVar(callSite, to.index());
        if (toVar == null) {
            return;
        }
        Var fromVar = InvokeUtils.getVar(callSite, from.index());
        CSVar csFrom = csManager.getCSVar(context, fromVar);
        CSVar csTo = csManager.getCSVar(context, toVar);
        if (from.kind() == IndexRef.Kind.VAR) { // Var -> Var/Array/Field
            Kind kind = switch (to.kind()) {
                case VAR -> {
                    Transfer tf = getTransferFunction(transfer.type());
                    solver.addPFGEdge(
                            new TaintTransferEdge(csFrom, csTo),
                            tf);
                    yield null;
                }
                case ARRAY -> Kind.VAR_TO_ARRAY;
                case FIELD -> Kind.VAR_TO_FIELD;
            };
            if (kind != null) {
                TransferInfo info = new TransferInfo(kind, fromVar, transfer);
                transferInfos.put(toVar, info);
                transferTaint(solver.getPointsToSetOf(csTo), context, info);
            }
        } else if (to.kind() == IndexRef.Kind.VAR) { // Array/Field -> Var
            Kind kind = switch (from.kind()) {
                case ARRAY -> Kind.ARRAY_TO_VAR;
                case FIELD -> Kind.FIELD_TO_VAR;
                default -> throw new AnalysisException(); // unreachable
            };
            TransferInfo info = new TransferInfo(kind, toVar, transfer);
            transferInfos.put(fromVar, info);
            transferTaint(solver.getPointsToSetOf(csFrom), context, info);
        } else { // ignore other cases
            logger.warn("TaintTransfer {} -> {} (in {}) is not supported",
                    transfer, from.kind(), to.kind());
        }

        // If the taint is transferred to base or argument, it means
        // that the objects pointed to by "to" were mutated
        // by the invocation. For such cases, we need to propagate the
        // taint to the pointers aliased with "to". The pointers
        // whose objects come from "to" will be naturally handled by
        // pointer analysis, and we just need to specially handle the
        // pointers whose objects flow to "to", i.e., back propagation.
        if (enableBackPropagate
                && to.index() != InvokeUtils.RESULT
                && to.kind() == IndexRef.Kind.VAR
                && !(to.index() == InvokeUtils.BASE
                && transfer.method().isConstructor())) {
            backPropagateTaint(toVar, context);
        }
    }

    private void transferTaint(PointsToSet baseObjs, Context ctx, TransferInfo info) {
        CSVar csVar = csManager.getCSVar(ctx, info.var());
        Transfer tf = getTransferFunction(info.transfer().type());
        switch (info.kind()) {
            case VAR_TO_ARRAY -> {
                baseObjs.objects()
                        .map(csManager::getArrayIndex)
                        .forEach(arrayIndex ->
                                solver.addPFGEdge(
                                        new TaintTransferEdge(csVar, arrayIndex),
                                        tf));
            }
            case VAR_TO_FIELD -> {
                JField f = info.transfer().to().field();
                baseObjs.objects()
                        .map(o -> csManager.getInstanceField(o, f))
                        .forEach(oDotF ->
                                solver.addPFGEdge(
                                        new TaintTransferEdge(csVar, oDotF),
                                        tf));
            }
            case ARRAY_TO_VAR -> {
                baseObjs.objects()
                        .map(csManager::getArrayIndex)
                        .forEach(arrayIndex ->
                                solver.addPFGEdge(
                                        new TaintTransferEdge(arrayIndex, csVar),
                                        tf));
            }
            case FIELD_TO_VAR -> {
                JField f = info.transfer().from().field();
                baseObjs.objects()
                        .map(o -> csManager.getInstanceField(o, f))
                        .forEach(oDotF ->
                                solver.addPFGEdge(
                                        new TaintTransferEdge(oDotF, csVar),
                                        tf));
            }
        }
    }

    private Transfer getTransferFunction(Type toType) {
        return transferFunctions.computeIfAbsent(toType,
                type -> ((edge, input) -> {
                    PointsToSet newTaints = solver.makePointsToSet();
                    input.objects()
                            .map(CSObj::getObject)
                            .filter(manager::isTaint)
                            .map(manager::getSourcePoint)
                            .map(source -> manager.makeTaint(source, type))
                            .map(taint -> csManager.getCSObj(emptyContext, taint))
                            .forEach(newTaints::addObject);
                    return newTaints;
                }));
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
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        if (edge.getKind() == CallKind.OTHER) {
            // skip other call edges, e.g., reflective call edges,
            // which currently cannot be handled for transfer methods
            // TODO: handle OTHER call edges
            return;
        }
        Set<TaintTransfer> tfs = transfers.get(edge.getCallee().getMethod());
        if (!tfs.isEmpty()) {
            Context context = edge.getCallSite().getContext();
            Invoke callSite = edge.getCallSite().getCallSite();
            tfs.forEach(tf -> processTransfer(context, callSite, tf));
        }
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        Context ctx = csVar.getContext();
        transferInfos.get(csVar.getVar()).forEach(info ->
                transferTaint(pts, ctx, info));
    }

    @Override
    public void onNewStmt(Stmt stmt, JMethod container) {
        if (callSiteMode &&
                stmt instanceof Invoke invoke &&
                !invoke.isDynamic()) {
            JMethod callee = invoke.getMethodRef().resolveNullable();
            if (transfers.containsKey(callee)) {
                callSiteTransfers.put(container, invoke);
            }
        }
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        if (callSiteMode) {
            JMethod method = csMethod.getMethod();
            Set<Invoke> callSites = callSiteTransfers.get(method);
            if (!callSites.isEmpty()) {
                Context context = csMethod.getContext();
                callSites.forEach(callSite -> {
                    JMethod callee = callSite.getMethodRef().resolve();
                    transfers.get(callee).forEach(transfer ->
                            processTransfer(context, callSite, transfer));
                });
            }
        }
    }
}
