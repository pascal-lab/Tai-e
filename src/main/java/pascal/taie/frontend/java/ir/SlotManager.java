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

import java.util.Optional;

import org.objectweb.asm.tree.AbstractInsnNode;

import pascal.taie.frontend.java.ir.ssa.SSATransform;
import pascal.taie.frontend.java.ir.ssa.FrontendPhiExp;
import pascal.taie.frontend.java.ir.ssa.FrontendPhiStmt;
import pascal.taie.ir.exp.VarMutator;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Stmt;

/**
 * Manages operations on local variable slots, resolving slot reuse through {@link SSATransform}.
 * It handles load/store operations and is responsible for generating {@link FrontendPhiStmt}.
 */
final class SlotManager {
    /**
     * The SSA construction engine that computes Def-Use chains.
     */
    private SSATransform ssaTransform;

    /**
     * Records all def/use operations about slots in bytecode, use DUIndex to index them.
     */
    private final DUInfo DUInfo;

    /**
     * Maps a definition's DUIndex to its corresponding Var.
     */
    private Var[] def2Var;

    /**
     * The shared context holding all resources and state for the IR building process.
     */
    private final IRBuilderContext context;

    // --- Runtime State ---

    /**
     * The current DUIndex being processed within the current block
     * Incremented as {@link SlotManager#getNextDUIndex}.
     */
    private int currDUIndex = -1;
    /**
     * The basic block that is currently being processed
     */
    private BytecodeBlock currBlock = null;

    // ========================================================================
    // 1. Construction & Build Phase
    // ========================================================================

    SlotManager(IRBuilderContext context) {
        this.context = context;
        // duInfo must be initialized first, because recordDUInfo may be called before initialize.
        this.DUInfo = new DUInfo(context.method, context.source.instructions.size());
    }

    /**
     * Build DUIndex, BCSSA and initialized var map.
     * The method should be called after basic du info is built by {@link #recordDUInfo}.
     */
    void initialize() {
        DUInfo.build(context.cfg, context.source.maxLocals);
        ssaTransform = new SSATransform(context.cfg, context.source.maxLocals, DUInfo, context.isSSA, context.dom);
        ssaTransform.build();
        initializeVarsForSlots();
    }

    /**
     * Record def/use operations.
     */
    void recordDUInfo(int insnIndex, int slot, boolean read) {
        DUInfo.recordDUInfo(insnIndex, slot, read);
    }

    // ========================================================================
    // 2. Execution Phase API (for processing bytecode)
    // ========================================================================

    /**
     * Prepares for processing a new block.
     */
    void enterBlock(BytecodeBlock block) {
        assert currBlock == null;
        assert currDUIndex == -1;
        currBlock = block;
        currDUIndex = DUInfo.getBlockStartDUIndex(currBlock);
    }

    /**
     * Finalizes processing for the current block.
     */
    void exitBlock() {
        assert currDUIndex == DUInfo.getBlockEndDUIndex(currBlock);
        currBlock = null;
        currDUIndex = -1;
    }

    /**
     * Resolves a variable load (e.g., ILOAD) to its corresponding Var.
     */
    Var loadVar(int slot, AbstractInsnNode insn) {
        int duIndex = getNextDUIndex(insn);
        Var v;
        int defIndex = ssaTransform.getReachDef(duIndex);
        assert defIndex != -1;
        if (canDirectlyPropagate(defIndex)) {
            v = def2Var[defIndex];
        } else {
            int newSlot = ssaTransform.getNewSlot(defIndex);
            assert newSlot != SSATransform.UNDEFINED; // must be phi-connected insn, a local is assigned before
            v = context.varManager.getVar(newSlot);
        }
        assert v != null;
        tryActualVarName(v, slot, insn);
        return v;
    }

    /**
     * Handles a variable store (e.g., ISTORE).
     */
    void storeVar(int slot, AbstractInsnNode insn, OperandStack operandStack) {
        int duIndex = getNextDUIndex(insn);
        Var v;
        if (!ssaTransform.isDefUsed(duIndex)) {
            // this var is not used, we don't need to generate store stmt
            // still, we need to handle the side effect (e.g. invoke)
            // note: stack may contains `Top`, so don't use `popToEffect`
            operandStack.automaticPopToEffect();
            return;
        }
        if (canDirectlyPropagate(duIndex)) {
            v = operandStack.popVar();
            // if this var is a local, we need create another copy
            // in case this local var is modified later
            if (context.varManager.isForSlot(v) && !context.varManager.isSSAVar(v) && !context.isSSA) {
                Var origin = v;
                v = context.varManager.getTempVar();
                context.stmtManager.associateStmt(insn, Utils.newAssignStmt(context.method, v, origin));
            }
            def2Var[duIndex] = v;
        } else {
            // still use a local var
            int newSlot = ssaTransform.getNewSlot(duIndex);
            v = context.varManager.getVar(newSlot);
            assert v != null;
            Stmt stmt = operandStack.popToVar(v);
            context.stmtManager.associateStmt(insn, stmt);
        }
        tryActualVarName(v, slot, insn);
    }

    /**
     * A specialized store for the exception object at the start of a catch block.
     */
    Var storeCatchVar(AbstractInsnNode insn) {
        int duIndex = getNextDUIndex(insn);
        Var catchVar = canDirectlyPropagate(duIndex)
                ? context.varManager.getTempVar()
                : context.varManager.getVar(ssaTransform.getNewSlot(duIndex));
        def2Var[duIndex] = catchVar;
        context.stmtManager.associateStmt(insn, new Catch(catchVar));
        return catchVar;
    }

    /**
     * Emits Phi statements for slot variables at the beginning of a block.
     */
    void emitSSAPhisForSlot(BytecodeBlock block) {
        assert context.isSSA;
        // should have at least one instruction
        AbstractInsnNode firstInsn = block.getInsns().get(0);
        ssaTransform.visitUsedInternalPhis(block, (phi) -> {
            Var phiVar = context.varManager.getTempVar();
            Var origin = context.varManager.getVar(phi.getSlot());
            FrontendPhiExp phiExp = new FrontendPhiExp();
            def2Var[phi.getPhiDUIndex()] = phiVar;
            FrontendPhiStmt frontendPhiStmt = new FrontendPhiStmt(origin, phiVar, phiExp);
            context.varManager.setNonSSA(phiVar);
            context.stmtManager.associateStmt(firstInsn, frontendPhiStmt);
            phi.setFrontendPhi(frontendPhiStmt);
            context.varManager.aliasForSlot(phiVar);
        });
    }

    /**
     * Fills in the input arguments for the previously emitted Phi statements.
     */
    void addInDefsForSlotPhis() {
        if (context.isSSA) {
            for (BytecodeBlock block : context.dom.getReversePostOrder()) {
                context.slotManager.addInDefsForSlotPhis(block);
            }
        }
    }

    private void addInDefsForSlotPhis(BytecodeBlock block) {
        ssaTransform.visitUsedInternalPhis(block, (phi) -> {
            FrontendPhiStmt realPhi = phi.getFrontendPhi();
            assert realPhi != null;
            FrontendPhiExp phiExp = realPhi.getRValue();
            for (int i = 0; i < phi.getInDefs().size(); ++i) {
                int defIndex = phi.getInDefs().get(i);
                Var v = def2Var[defIndex];
                phiExp.addUseAndCorrespondingBlocks(v, phi.getInBlocks().get(i));
            }
        });
    }

    // ========================================================================
    // 3. Internal Implementation
    // ========================================================================

    private void initializeVarsForSlots() {
        def2Var = new Var[ssaTransform.getMaxDUIndexWithPhi()];
        if (!context.isSSA) {
            context.varManager.makeVarsForNewSlots(ssaTransform.getNewSlotSize(), ssaTransform.getNewSlot2Origin());
        }
        // ensure all params is defined at beginning
        for (int i = 0; i < DUInfo.getParamSize(); ++i) {
            if (canDirectlyPropagate(i)) {
                def2Var[i] = context.varManager.getVar(i);
                Var current = def2Var[i];
                if (ssaTransform.isIsolatedDef(i)) {
                    context.varManager.setSSA(current);
                } else {
                    context.varManager.setNonSSA(current);
                }
            }
        }
    }

    /**
     * Determines whether the variable defined at {@code duIndex} can be propagated directly:
     * skip store and the value is returned later upon loading.
     */
    private boolean canDirectlyPropagate(int duIndex) {
        return context.isSSA || ssaTransform.isIsolatedDef(duIndex);
    }

    private void tryActualVarName(Var v, int slot, AbstractInsnNode insn) {
        if (context.varManager.existLocalVariables() && VarManager.mayRename(v)) {
            Optional<String> name = context.varManager.getName(slot, insn);
            name.ifPresent((n) -> {
                String realName = context.varManager.nameWithSuffix(n);
                VarMutator.setName(v, realName);
            });
        }
    }

    /**
     * The DUIndex is increasing within the bytecode block, so it NEEDs
     * to be accessed in the order of du operations within the bytecode block.
     */
    private int getNextDUIndex(AbstractInsnNode insn) {
        DUInfo.assertDUIndexValid(currDUIndex, context.getInsnIndex(insn), currBlock);
        return currDUIndex++;
    }
}
