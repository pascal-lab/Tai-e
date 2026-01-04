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
import org.objectweb.asm.commons.JSRInlinerAdapter;

import pascal.taie.World;
import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.frontend.java.ir.typing.TypeInference;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.util.graph.Dominators;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * The main class for converting bytecode to Tai-e IR.
 */
public class BytecodeIRBuilder {

    private static final Logger logger = LogManager.getLogger(BytecodeIRBuilder.class);

    private final FrontendTypeSystem typeSystem;

    /**
     * Tai-e IR output
     */
    private IR ir;

    /**
     * The method to be built
     */
    public final JMethod method;

    /**
     * Bytecode input, a bytecode method represented by {@link JSRInlinerAdapter}
     */
    private final JSRInlinerAdapter source;

    /**
     * Manager that manage the creation and naming of Tai-e IR variables
     */
    public final VarManager varManager;

    /**
     * Generated Tai-e IR exception entry ({@link ExceptionEntry})
     */
    private List<ExceptionEntry> exceptionEntries;

    /**
     * Operand stack for stack manipulation operations
     */
    private OperandStack operandStack;

    /**
     * If we build SSA IR. Read from {@link pascal.taie.config.Options}
     */
    private final boolean isSSA;

    /**
     * Manages load/store operations on local variable slots and handles SSA-related transformations.
     */
    private final SlotManager slotManager;

    /**
     * Manages the mapping from bytecode instructions to their generated IR statements.
     */
    private final StmtManager stmtManager;

    /**
     * Process bytecode instructions into Tai-e IR statements
     */
    private BytecodeProcessor bytecodeProcessor;

    /**
     * Dominator and dominator frontier computed for bytecode block graph
     */
    private Dominators<BytecodeBlock> dom;

    /**
     * Build CFG from ASM instructions using {@link BytecodeCFGBuilder}.
     */
    public BytecodeCFG cfg;

    BytecodeIRBuilder(FrontendTypeSystem typeSystem, JMethod method,
                      AsmMethodSource methodSource) {
        this.typeSystem = typeSystem;
        this.method = method;
        this.source = methodSource.adapter();
        assert method.getName().equals(source.name);
        this.isSSA = World.get().getOptions().isSSA();
        this.varManager = new VarManager(method,
                source.localVariables, source.instructions, source.maxLocals);
        this.stmtManager = new StmtManager(isSSA, source.instructions);
        this.slotManager = new SlotManager(method,
                varManager, isSSA, source, stmtManager);
    }

    public void build() {
        if (source.instructions.size() != 0) {
            // Build CFG with rwTable visitor and exception type resolver
            cfg = new BytecodeCFGBuilder(source,
                    slotManager::writeRwTable,
                    this::fromExceptionType)
                    .build();
            dom = new Dominators<>(cfg);
            slotManager.build(cfg, dom);
            operandStack = new OperandStack(method, varManager, cfg, stmtManager);
            bytecodeProcessor = new BytecodeProcessor(typeSystem, varManager,
                    method, isSSA, operandStack, slotManager, stmtManager);
            cfg.getEntry().setInStack(new Stack<>());
            for (BytecodeBlock block : dom.getReversePostOrder()) {
                bytecodeProcessor.processBlock2Stmts(block);
            }
            for (BytecodeBlock block : dom.getReversePostOrder()) {
                if (isSSA) {
                    slotManager.addInDefsForSlotPhis(block);
                }
            }
            new StackPhiResolver(method, isSSA, operandStack, cfg, stmtManager, varManager).resolveStackPhis();
            for (BytecodeBlock block : cfg) {
                stmtManager.buildBlockStmts(block);
            }
            new TypeInference(this, typeSystem).build();
            IRAssembler assembler = new IRAssembler(method, source, typeSystem, varManager, cfg);
            ir = assembler.assembleIR();
        }
    }

    private void verify() {
        for (Var v : varManager.getVars()) {
            assert verifyAllInStmts(v.getInvokes());
            assert verifyAllInStmts(v.getLoadArrays());
            assert verifyAllInStmts(v.getStoreArrays());
            assert verifyAllInStmts(v.getLoadFields());
            assert verifyAllInStmts(v.getStoreFields());
        }

        for (int i = 0; i < varManager.getVars().size(); ++i) {
            Var v = varManager.getVars().get(i);
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

    private ClassType fromExceptionType(String internalName) {
        if (internalName == null) {
            return typeSystem.throwableType();
        } else {
            ReferenceType r = typeSystem.fromAsmInternalName(internalName);
            if (r instanceof ClassType c) {
                return c;
            } else {
                throw new UnsupportedOperationException("Unsupported exception type: " + r);
            }
        }
    }

    public IR getIr() {
        return ir;
    }

    public boolean isSSA() {
        return isSSA;
    }
}
