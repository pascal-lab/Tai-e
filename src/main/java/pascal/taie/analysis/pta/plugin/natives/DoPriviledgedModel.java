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
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.ir.exp.InvokeInterface;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

class DoPriviledgedModel {

    private static final String[] DO_PRIVILEGED_ACTION_METHODS = {
            "<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedAction)>",
            "<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedAction,java.security.AccessControlContext)>",
    };

    private static final String[] DO_PRIVILEGED_EXCEPTION_ACTION_METHODS = {
            "<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction)>",
            "<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction,java.security.AccessControlContext)>",
    };

    private final Solver solver;

    private final CSManager csManager;

    /**
     * Maps the doPrivileged method to the corresponding action method.
     */
    private final Map<JMethod, MethodRef> doPrivileged2ActionRun;

    /**
     * Caches the fake invokes in the method.
     */
    private final MultiMap<JMethod, Invoke> method2FakeActionInvokes;

    /**
     * Caches the original doPrivileged method of the fake invoke.
     */
    private final Map<Invoke, Invoke> fakeActionInvoke2DoPrivilegedInvoke;

    DoPriviledgedModel(Solver solver) {
        this.solver = solver;
        this.csManager = solver.getCSManager();
        this.doPrivileged2ActionRun = Maps.newMap();
        this.method2FakeActionInvokes = Maps.newMultiMap();
        this.fakeActionInvoke2DoPrivilegedInvoke = Maps.newMap();

        ClassHierarchy hierarchy = solver.getHierarchy();

        MethodRef privilegedActionRun = Objects.requireNonNull(
                        hierarchy.getJREMethod("<java.security.PrivilegedAction: java.lang.Object run()>"))
                .getRef();
        Arrays.stream(DO_PRIVILEGED_ACTION_METHODS)
                .map(hierarchy::getJREMethod)
                .filter(Objects::nonNull)
                .forEach(m -> doPrivileged2ActionRun.put(m, privilegedActionRun));

        MethodRef privilegedExceptionActionRun = Objects.requireNonNull(
                        hierarchy.getJREMethod("<java.security.PrivilegedExceptionAction: java.lang.Object run()>"))
                .getRef();
        Arrays.stream(DO_PRIVILEGED_EXCEPTION_ACTION_METHODS)
                .map(hierarchy::getJREMethod)
                .filter(Objects::nonNull)
                .forEach(m -> doPrivileged2ActionRun.put(m, privilegedExceptionActionRun));
    }

    Collection<JMethod> getDoPrivilegeds() {
        return doPrivileged2ActionRun.keySet();
    }

    /**
     * Finds all doPrivileged invokes in the method, and makes fakes invokes for them.
     */
    public void handleNewMethod(JMethod method) {
        method.getIR().invokes(false).forEach(invoke -> {
            JMethod target = invoke.getMethodRef().resolveNullable();
            if (target != null) {
                MethodRef actionRun = doPrivileged2ActionRun.get(target);
                if (actionRun != null) {
                    Invoke run = new Invoke(invoke.getContainer(),
                            new InvokeInterface(actionRun,
                                    invoke.getInvokeExp().getArg(0), List.of()),
                            invoke.getResult());
                    ;
                    method2FakeActionInvokes.put(method, run);
                    fakeActionInvoke2DoPrivilegedInvoke.put(run, invoke);
                }
            }
        });
    }

    /**
     * Adds all fake invokes into the method.
     */
    @SuppressWarnings("unchecked")
    public void handleNewCSMethod(CSMethod csMethod) {
        Set<?> fakeInvokes = method2FakeActionInvokes.get(csMethod.getMethod());
        if (!fakeInvokes.isEmpty()) {
            solver.addStmts(csMethod, (Collection<Stmt>) fakeInvokes);
        }
    }

    /**
     * Connects the doPrivileged method to the corresponding action method
     * which is the callee of the fake invoke.
     */
    public void handleNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        Invoke invoke = edge.getCallSite().getCallSite();
        Invoke doPrivilegedInvoke = fakeActionInvoke2DoPrivilegedInvoke.get(invoke);
        if (doPrivilegedInvoke != null) {
            solver.addCallEdge(new DoPrivilegedCallEdge(
                    csManager.getCSCallSite(edge.getCallSite().getContext(), doPrivilegedInvoke),
                    edge.getCallee()));
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
