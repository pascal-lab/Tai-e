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

import pascal.taie.frontend.java.ir.ssa.BCSSA;
import pascal.taie.frontend.java.ir.ssa.FrontendPhiExp;
import pascal.taie.frontend.java.ir.ssa.FrontendPhiStmt;
import pascal.taie.ir.exp.VarMutator;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Stmt;

/**
 * Manages operations on local variable slots, resolving slot reuse through {@link BCSSA}.
 * It handles load/store operations and is responsible for generating {@link FrontendPhiStmt}.
 */
final class SlotManager {
    /**
     * The SSA construction engine that computes Def-Use chains.
     */
    private BCSSA bcssa;

    /**
     * Records all def/use operations about slots in bytecode, use DUIndex to index them.
     */
    private final DUInfo DUInfo;

    /**
     * Maps a definition's DUIndex to its corresponding Var.
     */
    private Var[] reachVars;

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
        // TODO: duInfo must be initialized first, because duTable/count is needed during bytecode visit. Explain better later
        this.DUInfo = new DUInfo(context.method, context.source.instructions.size());
    }

    /**
     * Build DUIndex, BCSSA and initialized var map.
     */
    void initialize() {
        DUInfo.build(context.cfg, context.source.maxLocals);
        bcssa = new BCSSA(context.cfg, context.source.maxLocals, DUInfo, context.isSSA, context.dom);
        bcssa.build();
        initializeVarsForSlots();
    }

    /**
     * Record def/use operations to populate the duTable.
     */
    void writeDUTable(int insnIndex, int slot, boolean read) {
        DUInfo.writeDUTable(insnIndex, slot, read);
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
        int defIndex = bcssa.getReachDef(duIndex);
        assert defIndex != -1;
        if (isFastProcessVar(defIndex)) {
            v = reachVars[defIndex];
        } else {
            int realVar = bcssa.getRealLocalSlot(defIndex);
            assert realVar != -1; // must be phi-connected insn, a local is assigned before
            v = context.varManager.getLocal(realVar);
        }
        assert v != null;
        tryFixVarName(v, slot, insn);
        return v;
    }

    /**
     * Handles a variable store (e.g., ISTORE).
     */
    void storeVar(int slot, AbstractInsnNode insn, OperandStack operandStack) {
        int duIndex = getNextDUIndex(insn);
        Var v;
        if (!bcssa.isDefUsed(duIndex)) {
            // this var is not used, we don't need to generate store stmt
            // still, we need to handle the side effect (e.g. invoke)
            // note: stack may contains `Top`, so don't use `popToEffect`
            operandStack.automaticPopToEffect();
            return;
        }
        if (isFastProcessVar(duIndex)) {
            // load insn will use duTables to get this var
            v = operandStack.popVar();
            // if this var is a local, we need create another copy
            // in case this local var is modified later
            if (context.varManager.isLocal(v) && !context.varManager.isSSAVar(v) && !context.isSSA) {
                Var origin = v;
                v = context.varManager.getTempVar();
                context.stmtManager.associateStmt(insn, Utils.newAssignStmt(context.method, v, origin));
            }
            reachVars[duIndex] = v;
        } else {
            // still use a local var
            int realVar = bcssa.getRealLocalSlot(duIndex);
            v = context.varManager.getLocal(realVar);
            // use this to generate store stmt
            assert v != null;
            Stmt stmt = operandStack.popToVar(v);
            context.stmtManager.associateStmt(insn, stmt);
        }
        tryFixVarName(v, slot, insn);
    }

    /**
     * A specialized store for the exception object at the start of a catch block.
     */
    Var storeCatchVar(AbstractInsnNode insn) {
        int duIndex = getNextDUIndex(insn);
        Var catchVar = isFastProcessVar(duIndex)
                ? context.varManager.getTempVar()
                : context.varManager.getLocal(bcssa.getRealLocalSlot(duIndex));
        reachVars[duIndex] = catchVar;
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
        bcssa.visitLivePhis(block, (phi) -> {
            Var phiVar = context.varManager.getTempVar();
            Var origin = context.varManager.getLocal(phi.getSlot());
            FrontendPhiExp phiExp = new FrontendPhiExp();
            reachVars[phi.getDUIndex()] = phiVar;
            FrontendPhiStmt frontendPhiStmt = new FrontendPhiStmt(origin, phiVar, phiExp);
            context.varManager.setNonSSA(phiVar);
            context.stmtManager.associateStmt(firstInsn, frontendPhiStmt);
            phi.setRealPhi(frontendPhiStmt);
            context.varManager.aliasLocal(phiVar, context.varManager.getSlot(origin));
        });
    }

    /**
     * Fills in the actual arguments for the previously emitted Phi statements.
     */
    void addInDefsForSlotPhis() {
        if (context.isSSA) {
            for (BytecodeBlock block : context.dom.getReversePostOrder()) {
                context.slotManager.addInDefsForSlotPhis(block);
            }
        }
    }

    private void addInDefsForSlotPhis(BytecodeBlock block) {
        bcssa.visitLivePhis(block, (phi) -> {
            FrontendPhiStmt realPhi = (FrontendPhiStmt) phi.getRealPhi();
            assert realPhi != null;
            FrontendPhiExp phiExp = realPhi.getRValue();
            for (int i = 0; i < phi.getInDefs().size(); ++i) {
                int defIndex = phi.getInDefs().get(i);
                Var v = reachVars[defIndex];
                phiExp.addUseAndCorrespondingBlocks(v, phi.getInBlocks().get(i));
            }
        });
    }

    // ========================================================================
    // 3. Internal Implementation
    // ========================================================================

    private void initializeVarsForSlots() {
        reachVars = new Var[bcssa.getMaxDUCount()];
        if (!context.isSSA) {
            context.varManager.enlargeLocal(bcssa.getRealLocalCount(), bcssa.getVarMappingTable());
        }
        // ensure all params is defined at beginning
        for (int i = 0; i < DUInfo.getParamSize(); ++i) {
            if (isFastProcessVar(i)) {
                reachVars[i] = context.varManager.getLocal(i);
                Var current = reachVars[i];
                if (bcssa.canFastProcess(i)) {
                    context.varManager.setSSA(current);
                } else {
                    context.varManager.setNonSSA(current);
                }
            }
        }
    }


    private boolean isFastProcessVar(int duIndex) {
        return context.isSSA || bcssa.canFastProcess(duIndex);
    }

    private void tryFixVarName(Var v, int slot, AbstractInsnNode insn) {
        if (context.varManager.existsLocalVariableTable && VarManager.mayRename(v)) {
            Optional<String> name = context.varManager.getName(slot, insn);
            name.ifPresent((n) -> {
                String realName = context.varManager.tryUseName(n);
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
