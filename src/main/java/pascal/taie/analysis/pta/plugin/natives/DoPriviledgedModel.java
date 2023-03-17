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
import pascal.taie.analysis.pta.plugin.util.AbstractIRModel;
import pascal.taie.ir.exp.InvokeInterface;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;

import java.util.List;
import java.util.Map;
import java.util.Objects;

class DoPriviledgedModel extends AbstractIRModel {

    /**
     * Map from artificial invocation of run() to corresponding
     * invocation doPriviledged(...).
     */
    private final Map<Invoke, Invoke> run2DoPriv = Maps.newMap();

    private final CSManager csManager;

    DoPriviledgedModel(Solver solver) {
        super(solver);
        csManager = solver.getCSManager();
    }

    @Override
    protected void registerIRGens() {
        MethodRef privilegedActionRun = Objects.requireNonNull(
                hierarchy.getJREMethod("<java.security.PrivilegedAction: java.lang.Object run()>"))
                .getRef();
        JMethod doPrivileged1 = hierarchy.getJREMethod("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedAction)>");
        registerIRGen(doPrivileged1, invoke -> doPrivileged(invoke, privilegedActionRun));

        JMethod doPrivileged2 = hierarchy.getJREMethod("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedAction,java.security.AccessControlContext)>");
        registerIRGen(doPrivileged2, invoke -> doPrivileged(invoke, privilegedActionRun));

        MethodRef privilegedExceptionActionRun = Objects.requireNonNull(
                hierarchy.getJREMethod("<java.security.PrivilegedExceptionAction: java.lang.Object run()>"))
                .getRef();
        JMethod doPrivileged3 = hierarchy.getJREMethod("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction)>");
        registerIRGen(doPrivileged3, invoke -> doPrivileged(invoke, privilegedExceptionActionRun));

        JMethod doPrivileged4 = hierarchy.getJREMethod("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction,java.security.AccessControlContext)>");
        registerIRGen(doPrivileged4, invoke -> doPrivileged(invoke, privilegedExceptionActionRun));
    }

    private List<Stmt> doPrivileged(Invoke invoke, MethodRef run) {
        Invoke invokeRun = new Invoke(invoke.getContainer(),
                new InvokeInterface(run, invoke.getInvokeExp().getArg(0), List.of()),
                invoke.getResult());
        run2DoPriv.put(invokeRun, invoke);
        return List.of(invokeRun);
    }

    /**
     * Connects doPrivileged(...) invocation to the corresponding run() method
     * which is the callee of the corresponding run().
     */
    void handleNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        Invoke invoke = edge.getCallSite().getCallSite();
        Invoke doPrivilegedInvoke = run2DoPriv.get(invoke);
        if (doPrivilegedInvoke != null) {
            CSCallSite csCallSite = csManager.getCSCallSite(
                    edge.getCallSite().getContext(), doPrivilegedInvoke);
            solver.addCallEdge(new DoPrivilegedCallEdge(csCallSite, edge.getCallee()));
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
