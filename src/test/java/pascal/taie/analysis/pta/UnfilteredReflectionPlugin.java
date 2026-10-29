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

package pascal.taie.analysis.pta;

import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.graph.flowgraph.FlowKind;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;

/**
 * Stands in for a framework model: it links a reflective call to its target and
 * passes the reflective receiver into the callee's {@code this}, without
 * dispatching per receiver the way Tai-e's own reflection model does.
 */
public class UnfilteredReflectionPlugin implements Plugin {

    private static final String METHOD_INVOKE =
            "<java.lang.reflect.Method: java.lang.Object invoke"
                    + "(java.lang.Object,java.lang.Object[])>";

    private static final String TARGET = "<ArityMismatch$App: void run()>";

    private Solver solver;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
    }

    @Override
    public void onNewStmt(Stmt stmt, JMethod container) {
        if (!(stmt instanceof Invoke invoke) || invoke.isDynamic()) {
            return;
        }
        JMethod callee = solver.getHierarchy().getMethod(TARGET);
        if (callee == null
                || !"invoke".equals(invoke.getMethodRef().getName())
                || !"java.lang.reflect.Method".equals(
                        invoke.getMethodRef().getDeclaringClass().getName())) {
            return;
        }
        Context empty = solver.getContextSelector().getEmptyContext();
        solver.addCallEdge(new Edge<>(CallKind.OTHER,
                solver.getCSManager().getCSCallSite(empty, invoke),
                solver.getCSManager().getCSMethod(empty, callee)));
        solver.addPFGEdge(
                solver.getCSManager().getCSVar(empty, invoke.getInvokeExp().getArg(0)),
                solver.getCSManager().getCSVar(empty, callee.getIR().getThis()),
                FlowKind.THIS_PASSING);
    }
}
