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
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.graph.Dominators;

import java.util.List;
import java.util.Stack;

/**
 * The main class for converting bytecode to Tai-e IR.
 */
public class BytecodeIRBuilder {

    private static final Logger logger = LogManager.getLogger(BytecodeIRBuilder.class);

    /**
     * Tai-e IR output
     */
    private IR ir;

    /**
     * The shared context holding all resources and state for the IR building process.
     */
    private final BytecodeIRBuildContext context;

    BytecodeIRBuilder(FrontendTypeSystem typeSystem, JMethod method,
                      AsmMethodSource methodSource) {
        this.context = new BytecodeIRBuildContext(method, methodSource, typeSystem);
    }

    public void build() {
        if (context.source.instructions.size() != 0) {
            context.cfg = new BytecodeCFGBuilder(context.source, context.slotManager::writeRwTable, context::getExceptionType)
                    .build();
            assert context.cfg != null;
            context.dom = new Dominators<>(context.cfg);
            context.slotManager.build(context.cfg, context.dom);
            var bytecodeProcessor = new BytecodeProcessor(context);
            context.cfg.getEntry().setInStack(new Stack<>());
            for (BytecodeBlock block : context.dom.getReversePostOrder()) {
                bytecodeProcessor.processBlock2Stmts(block);
            }
            for (BytecodeBlock block : context.dom.getReversePostOrder()) {
                if (context.isSSA) {
                    context.slotManager.addInDefsForSlotPhis(block);
                }
            }
            new StackPhiResolver(context).resolveStackPhis();
            for (BytecodeBlock block : context.cfg) {
                context.stmtManager.buildBlockStmts(block);
            }
            new TypeInference(context).build();
            IRAssembler assembler = new IRAssembler(context);
            ir = assembler.assembleIR();
        }
    }

    private void verify() {
        for (Var v : context.varManager.getVars()) {
            assert verifyAllInStmts(v.getInvokes());
            assert verifyAllInStmts(v.getLoadArrays());
            assert verifyAllInStmts(v.getStoreArrays());
            assert verifyAllInStmts(v.getLoadFields());
            assert verifyAllInStmts(v.getStoreFields());
        }

        for (int i = 0; i < context.varManager.getVars().size(); ++i) {
            Var v = context.varManager.getVars().get(i);
            assert v.getIndex() == i;
        }
    }

    private <T extends Stmt> boolean verifyAllInStmts(List<T> stmts) {
        return stmts.stream().allMatch(this::verifyInStmts);
    }

    private boolean verifyInStmts(Stmt stmt) {
        return stmt.getIndex() != -1 &&
                this.ir.getStmts().size() > stmt.getIndex() &&
                this.ir.getStmts().get(stmt.getIndex()) == stmt;
    }

    public IR getIR() {
        return ir;
    }
}
