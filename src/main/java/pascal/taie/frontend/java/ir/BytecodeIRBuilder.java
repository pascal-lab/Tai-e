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

package pascal.taie.frontend.java.ir;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.frontend.java.ir.typing.TypeInference;
import pascal.taie.ir.IR;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.graph.Dominators;

/**
 * The main class for converting bytecode to Tai-e IR.
 */
class BytecodeIRBuilder {

    private static final Logger logger = LogManager.getLogger(BytecodeIRBuilder.class);

    /**
     * The shared context holding all resources and state for the IR building process.
     */
    private final IRBuilderContext context;

    BytecodeIRBuilder(FrontendTypeSystem typeSystem, JMethod method,
                      AsmMethodSource methodSource) {
        this.context = new IRBuilderContext(method, methodSource, typeSystem);
    }

    IR build() {
        if (context.source.instructions.size() == 0) {
            return null;
        }
        // 1. build cfg, and also construct RwTable (used in slotManager) when building CFG
        context.cfg = new BytecodeCFGBuilder(context.source,
                context.slotManager::writeRwTable, context::getExceptionType)
                .build();
        assert context.cfg != null;

        // 2. build dominators
        context.dom = new Dominators<>(context.cfg);

        // 3. initialize slotManager: compute def-use info, build BCSSA and initialize Vars
        context.slotManager.initialize();

        // 4. process bytecode blocks to statements
        new BytecodeProcessor(context).processBlocks2Stmt();

        // 5. generate all the frontend phis
        context.slotManager.addInDefsForSlotPhis();
        new StackPhiResolver(context).resolveStackPhis();

        // 6. collect stmts for each block
        for (BytecodeBlock block : context.cfg) {
            context.stmtManager.buildBlockStmts(block);
        }

        // 7. type inference
        new TypeInference(context).build();

        // 8. complete stmts and build exception table, finally get IR
        return new IRAssembler(context).assembleIR();
    }
}
