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

package pascal.taie.analysis.pta.plugin.reflection;

import pascal.taie.analysis.graph.callgraph.OtherEdge;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.ir.exp.Var;

import javax.annotation.Nullable;

/**
 * Represents reflective call edges.
 */
class ReflectiveCallEdge extends OtherEdge<CSCallSite, CSMethod> {

    /**
     * Variable pointing to the array argument of reflective call,
     * which contains the arguments for the reflective target method, i.e.,
     * args for constructor.newInstance(args)/method.invoke(o, args).
     * This field is null for call edges from Class.newInstance().
     */
    @Nullable
    private final Var args;

    ReflectiveCallEdge(CSCallSite csCallSite, CSMethod callee, @Nullable Var args) {
        super(csCallSite, callee);
        this.args = args;
    }

    @Nullable
    Var getArgs() {
        return args;
    }
}
