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
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import pascal.taie.World;
import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.frontend.java.ir.ssa.FrontendPhiStmt;
import pascal.taie.frontend.java.ir.typing.TypeInference;
import pascal.taie.ir.DefaultIR;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.PhiExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Goto;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.PhiStmt;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.SwitchStmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.util.graph.Dominators;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
     * Generated Tai-e IR stmts
     */
    private List<Stmt> stmts;

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
        this.stmts = new ArrayList<>();
    }

    public void build() {
        if (source.instructions.size() != 0) {
            buildCFG();
            buildDom();
            slotManager.build(cfg, dom);
            operandStack = new OperandStack(method, varManager, cfg, stmtManager);
            bytecodeProcessor = new BytecodeProcessor(typeSystem, varManager,
                    method, isSSA, operandStack, slotManager, stmtManager);
            traverseBlocks();
            inferTypes();
            IRAssembler assembler = new IRAssembler(method, source, typeSystem, varManager, cfg);
            assembler.makeStmts(true);
            assembler.makeExceptionTable();
            ir = assembler.getIR();
        }
    }

    private void buildDom() {
        dom = new Dominators<>(cfg);
    }

    private void inferTypes() {
        new TypeInference(this, typeSystem).build();
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
                this.stmts.size() > stmt.getIndex() &&
                this.stmts.get(stmt.getIndex()) == stmt;
    }

//    private void mergeStack1(List<Stmt> auxiliary, Stack<StackItem> nowStack, Stack<StackItem> targetStack) {
//        Exp v = targetStack.pop();
//        if (v instanceof Top) {
//            return;
//        }
//        assert v instanceof Var: "merge target should be var of top";
//        Exp e = peekExp(nowStack);
//        if (e == v) {
//            popExp(nowStack);
//        } else {
//            Stmt stmt = popToVar(nowStack, (Var) v);
//            auxiliary.add(stmt);
//        }
//    }

//    private void mergeStack(BytecodeBlock bb, Stack<Exp> nowStack, Stack<Exp> target) {
//        List<Stmt> auxiliary = new ArrayList<>();
//        Stack<Exp> nowStack1 = new Stack<>();
//        Stack<Exp> target1 = new Stack<>();
//        nowStack1.addAll(nowStack);
//        target1.addAll(target);
//        while (! nowStack1.isEmpty()) {
//            mergeStack1(auxiliary, nowStack1, target1);
//        }
//        appendStackMergeStmts(bb, auxiliary);
//        assert target1.empty();
//    }

    private void buildCFG() {
        // Build CFG with rwTable visitor and exception type resolver
        cfg = new BytecodeCFGBuilder(source,
                slotManager::writeRwTable,
                this::fromExceptionType)
                .build();
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

    private void traverseBlocks() {
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
    }

//    private void setLineNumber() {
//        int currentLineNumber = -1;
//        for (var insn : source.instructions) {
//            if (!(insn instanceof LabelNode)) {
//                if (insn instanceof LineNumberNode l) {
//                    currentLineNumber = l.line;
//                } else {
//                    if (currentLineNumber == -1) {
//                        logger.atDebug().log("[IR] no line number info, method: " + method);
//                        return;
//                    }
//                    var stmt = insn2Stmt[getInsnIndex(insn)];
//                    if (stmt != null) {
//                        stmt.setLineNumber(currentLineNumber);
//                    }
//
//                    var stmts = auxiliaryStmts.get(getInsnIndex(insn));
//                    if (stmts != null) {
//                        for (var s : stmts) {
//                            s.setLineNumber(currentLineNumber);
//                        }
//                    }
//                }
//            }
//        }
//    }

    public IR getIr() {
        return ir;
    }

    public boolean isSSA() {
        return isSSA;
    }
}
