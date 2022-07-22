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

package pascal.taie.analysis.pta.plugin.natives;

import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.AbstractModel;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

class DoPriviledgedModel extends AbstractModel {

    private final MethodRef privilegedActionRun;

    private final MethodRef privilegedExceptionActionRun;

    private Set<JMethod> doPrivilegeds;

    DoPriviledgedModel(Solver solver) {
        super(solver);
        privilegedActionRun = Objects.requireNonNull(
                        hierarchy.getJREMethod("<java.security.PrivilegedAction: java.lang.Object run()>"))
                .getRef();
        privilegedExceptionActionRun = Objects.requireNonNull(
                        hierarchy.getJREMethod("<java.security.PrivilegedExceptionAction: java.lang.Object run()>"))
                .getRef();
    }

    Collection<JMethod> getDoPrivilegeds() {
        return doPrivilegeds;
    }

    @Override
    protected void registerVarAndHandler() {
        JMethod doPrivileged1 = hierarchy.getJREMethod("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedAction)>");
        registerRelevantVarIndexes(doPrivileged1, 0);
        registerAPIHandler(doPrivileged1, this::doPrivileged);

        JMethod doPrivileged2 = hierarchy.getJREMethod("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedAction,java.security.AccessControlContext)>");
        registerRelevantVarIndexes(doPrivileged2, 0);
        registerAPIHandler(doPrivileged2, this::doPrivileged);

        JMethod doPrivileged3 = hierarchy.getJREMethod("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction)>");
        registerRelevantVarIndexes(doPrivileged3, 0);
        registerAPIHandler(doPrivileged3, this::doPrivilegedException);

        JMethod doPrivileged4 = hierarchy.getJREMethod("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction,java.security.AccessControlContext)>");
        registerRelevantVarIndexes(doPrivileged4, 0);
        registerAPIHandler(doPrivileged4, this::doPrivilegedException);

        //noinspection ConstantConditions
        doPrivilegeds = Set.of(doPrivileged1, doPrivileged2, doPrivileged3, doPrivileged4);
    }

    private void doPrivileged(CSVar csVar, PointsToSet pts, Invoke invoke) {
        doPrivileged(csVar, pts, invoke, privilegedActionRun);
    }

    private void doPrivilegedException(CSVar csVar, PointsToSet pts, Invoke invoke) {
        doPrivileged(csVar, pts, invoke, privilegedExceptionActionRun);
    }

    /**
     * Model for AccessController.doPrivileged(...).
     */
    private void doPrivileged(CSVar csVar, PointsToSet pts, Invoke invoke, MethodRef run) {
        Context callerCtx = csVar.getContext();
        CSCallSite csCallSite = csManager.getCSCallSite(callerCtx, invoke);
        for (CSObj recvObj : pts) {
            JMethod callee = hierarchy.dispatch(recvObj.getObject().getType(), run);
            if (callee == null) {
                return;
            }
            // select callee context
            Context calleeCtx = selector.selectContext(
                    csCallSite, recvObj, callee);
            // pass receiver object
            solver.addVarPointsTo(calleeCtx, callee.getIR().getThis(), recvObj);
            // pass return value
            Var lhs = invoke.getResult();
            if (lhs != null) {
                CSVar csLhs = csManager.getCSVar(callerCtx, lhs);
                for (Var ret : callee.getIR().getReturnVars()) {
                    CSVar csRet = csManager.getCSVar(calleeCtx, ret);
                    solver.addPFGEdge(csRet, csLhs, PointerFlowEdge.Kind.RETURN);
                }
            }
            // add call edge
            CSMethod csCallee = csManager.getCSMethod(calleeCtx, callee);
            solver.addCallEdge(new DoPrivilegedCallEdge(csCallSite, csCallee));
        }
    }

    /**
     * Represents call edge from AccessController.doPrivileged(...)
     * to the privileged action.
     */
    private static class DoPrivilegedCallEdge extends Edge<CSCallSite, CSMethod> {

        DoPrivilegedCallEdge(CSCallSite csCallSite, CSMethod callee) {
            super(CallKind.OTHER, csCallSite, callee);
        }
    }
}
