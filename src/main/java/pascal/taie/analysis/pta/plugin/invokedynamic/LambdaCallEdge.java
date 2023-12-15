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

package pascal.taie.analysis.pta.plugin.invokedynamic;

import pascal.taie.analysis.graph.callgraph.OtherEdge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.Var;

import java.util.List;

/**
 * Represents call edge on lambda functional object.
 * The edge carries the information about invokedynamic invocation site
 * where the lambda functional object was created.
 */
class LambdaCallEdge extends OtherEdge<CSCallSite, CSMethod> {

    private final InvokeDynamic lambdaIndy;

    private final Context lambdaContext;

    LambdaCallEdge(CSCallSite csCallSite, CSMethod callee,
                   InvokeDynamic lambdaIndy, Context lambdaContext) {
        super(csCallSite, callee);
        this.lambdaIndy = lambdaIndy;
        this.lambdaContext = lambdaContext;
    }

    List<Var> getCapturedArgs() {
        return lambdaIndy.getArgs();
    }

    Context getLambdaContext() {
        return lambdaContext;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        LambdaCallEdge that = (LambdaCallEdge) o;
        return lambdaIndy.equals(that.lambdaIndy) &&
                lambdaContext.equals(that.lambdaContext);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + lambdaIndy.hashCode();
        result = 31 * result + lambdaContext.hashCode();
        return result;
    }
}
