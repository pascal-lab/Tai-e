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

package pascal.taie.frontend.java.ir.typing;

import pascal.taie.frontend.java.ir.IRBuilderContext;

/**
 * Pruning-based type inference based on the constraint graph.
 */
public class TypeInference {

    private final IRBuilderContext context;

    public TypeInference(IRBuilderContext context) {
        this.context = context;
    }

    public void inferTypes() {
        TypeFlowGraph graph = new TypeFlowGraph(context.typeSystem, context.varManager.getAllVars().size());
        graph.initialize(context.method, context.cfg, context.varManager);
        graph.inferTypes();
        boolean needInsertCast = graph.applyInferredTypes();
        if (needInsertCast) {
            CastingInserter inserter = new CastingInserter(context);
            inserter.build();
        }
    }
}
