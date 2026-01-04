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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.objectweb.asm.tree.AbstractInsnNode;

import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.frontend.java.ir.ssa.BCSSA;
import pascal.taie.frontend.java.ir.ssa.FrontendPhiExp;
import pascal.taie.frontend.java.ir.ssa.FrontendPhiStmt;
import pascal.taie.frontend.java.ir.ssa.GenericDUInfo;
import pascal.taie.ir.exp.ExpMutator;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.LazyArray;

/**
 * Manages operations on local variable slots, resolving slot reuse through def-use analysis.
 * It handles load/store operations and is responsible for generating {@link FrontendPhiStmt}.
 */
final class SlotManager {

    /**
     * Number of implicit writes for method parameters at entry.
     */
    private final int paramWriteSize;

    /**
     * Records read/write operations for each bytecode instruction index.
     */
    private final int[] rwTable;

    /**
     * Total count of variable read/write operations, including parameters.
     */
    private int rwCount;

    /**
     * Maps BlockIndex to the starting RWIndex of its operations.
     */
    private int[] start;

    /**
     * Maps BlockIndex to the ending RWIndex of its operations.
     */
    private int[] end;

    /**
     * Maps RWIndex to its bytecode instruction index.
     */
    private int[] rwToInsn;

    /**
     * The SSA construction engine that computes Def-Use chains.
     */
    private BCSSA bcssa;

    /**
     * Maps a definition's RWIndex to its corresponding Var.
     */
    private Var[] reachVars;

    // --- Runtime State ---

    /**
     * The current RWIndex being processed within the current block
     * Incremented as {@link SlotManager#getRWIndex}.
     */
    private int currRWIndex = -1;
    /**
     * The basic block that is currently being processed
     */
    private BytecodeBlock currBlock = null;

    /**
     * The shared context holding all resources and state for the IR building process.
     */
    private final BytecodeIRBuildContext context;

    // ========================================================================
    // 1. Construction & Build Phase
    // ========================================================================

    SlotManager(BytecodeIRBuildContext context) {
        this.context = context;

        this.paramWriteSize = getParamWriteSize();
        this.rwTable = new int[context.source.instructions.size()];
        this.rwCount = paramWriteSize;
    }

    /**
     * Build RWIndex, BCSSA and initialized var map.
     */
    void initialize() {
        GenericDUInfo duInfo = buildRWIndexAndDUInfo();
        buildBCSSA(duInfo);
        initializeVarsForSlots();
    }

    /**
     * Record read/write operations to populate the rwTable.
     */
    void writeRwTable(int index, int var, boolean read) {
        rwCount++;
        assert var < (1 << 29);
        int rwFlag = read ? 1 << 29 : 1 << 30;
        rwTable[index] = rwTable[index] | var | rwFlag;
    }

    // ========================================================================
    // 2. Execution Phase API (for processing bytecode)
    // ========================================================================

    /**
     * Prepares for processing a new block.
     */
    void enterBlock(BytecodeBlock block) {
        assert currRWIndex == -1;
        assert currBlock == null;
        currRWIndex = start[block.getIndex()];
        currBlock = block;
    }

    /**
     * Finalizes processing for the current block.
     */
    void exitBlock() {
        assert currRWIndex == end[currBlock.getIndex()];
        currRWIndex = -1;
        currBlock = null;
    }

    /**
     * Resolves a variable load (e.g., ILOAD) to its corresponding Var.
     */
    Var loadVar(int slot, AbstractInsnNode insn) {
        int rwIndex = getRWIndex(insn);
        Var v;
        int defIndex = bcssa.getReachDef(rwIndex);
        assert defIndex != -1; // wtf? undefined variable?
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
        int rwIndex = getRWIndex(insn);
        Var v;
        if (!bcssa.isDefUsed(rwIndex)) {
            // this var is not used, we don't need to generate store stmt
            // still, we need to handle the side effect (e.g. invoke)
            // note: stack may contains `Top`, so don't use `popToEffect`
            operandStack.automaticPopToEffect();
            return;
        }
        if (isFastProcessVar(rwIndex)) {
            // load insn will use rwTables to get this var
            v = operandStack.popVar();
            // if this var is a local, we need create another copy
            // in case this local var is modified later
            if (context.varManager.isLocal(v) && !context.varManager.isSSAVar(v) && !context.isSSA) {
                Var origin = v;
                v = context.varManager.getTempVar();
                context.stmtManager.associateStmt(insn, Utils.newAssignStmt(context.method, v, origin));
            }
            reachVars[rwIndex] = v;
        } else {
            // still use a local var
            int realVar = bcssa.getRealLocalSlot(rwIndex);
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
        int rwIndex = getRWIndex(insn);
        Var catchVar = isFastProcessVar(rwIndex)
                ? context.varManager.getTempVar()
                : context.varManager.getLocal(bcssa.getRealLocalSlot(rwIndex));
        reachVars[rwIndex] = catchVar;
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
            Var origin = context.varManager.getLocal(phi.getVar());
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

    private void addInDefsForSlotPhis(BytecodeBlock bb) {
        bcssa.visitLivePhis(bb, (phi) -> {
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

    private DUInfo buildRWIndexAndDUInfo() {
        rwToInsn = new int[rwCount];
        start = new int[context.cfg.nodeCount()];
        end = new int[context.cfg.nodeCount()];

        BytecodeBlock[] rwToBlock = new BytecodeBlock[rwCount];
        int counter = 0;
        BytecodeBlock entry = context.cfg.getEntry();
        LazyArray<List<BytecodeBlock>> defBlocks = new LazyArray<>(context.source.maxLocals) {
            @Override
            protected List<BytecodeBlock> createElement() {
                return new ArrayList<>();
            }
        };

        for (int i1 = 0; i1 < paramWriteSize; ++i1) {
            rwToInsn[counter] = -1;
            rwToBlock[counter] = entry;
            counter++;
        }

        for (int n = 0; n < context.cfg.nodeCount(); ++n) {
            BytecodeBlock curr = context.cfg.getObject(n);
            start[curr.getIndex()] = counter;
            int size = curr.getInsns().size();
            int start1 = curr.getInsns().getStart();
            for (int j = 0; j < size; ++j) {
                int i1 = j + start1;
                int rw = rwTable[i1];
                if (rw != 0) {
                    int var = rw & ((1 << 29) - 1);
                    boolean read = (rw & (1 << 29)) != 0;
                    boolean write = (rw & (1 << 30)) != 0;
                    if (read) {
                        rwToBlock[counter] = curr;
                        rwToInsn[counter++] = i1;
                    }
                    if (write) {
                        rwToBlock[counter] = curr;
                        rwToInsn[counter++] = i1;
                        defBlocks.get(var).add(curr);
                    }
                }
            }
            end[curr.getIndex()] = counter;
        }
        return new DUInfo(rwToBlock, defBlocks, counter);
    }

    private void buildBCSSA(GenericDUInfo duInfo) {
        bcssa = new BCSSA(context.cfg, context.source.maxLocals, duInfo, context.isSSA, context.dom);
        bcssa.build();
    }

    private void initializeVarsForSlots() {
        reachVars = new Var[bcssa.getMaxDUCount()];
        if (!context.isSSA) {
            context.varManager.enlargeLocal(bcssa.getRealLocalCount(), bcssa.getVarMappingTable());
        }
        // ensure all params is defined at beginning
        for (int i = 0; i < paramWriteSize; ++i) {
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


    private boolean isFastProcessVar(int v) {
        return context.isSSA || bcssa.canFastProcess(v);
    }

    private void tryFixVarName(Var v, int slot, AbstractInsnNode insn) {
        if (context.varManager.existsLocalVariableTable && VarManager.mayRename(v)) {
            Optional<String> name = context.varManager.getName(slot, insn);
            name.ifPresent((n) -> {
                String realName = context.varManager.tryUseName(n);
                ExpMutator.setName(v, realName);
            });
        }
    }

    private int getRWIndex(AbstractInsnNode insn) {
        assert rwToInsn[currRWIndex] == getInsnIndex(insn);
        assert currRWIndex < end[currBlock.getIndex()];
        return currRWIndex++;
    }

    private int getInsnIndex(AbstractInsnNode insn) {
        assert insn != null;
        return context.source.instructions.indexOf(insn);
    }

    private int getParamWriteSize() {
        int curr = context.method.isStatic() ? 0 : 1;
        for (int i = 0; i < context.method.getParamTypes().size(); ++i) {
            Type type = context.method.getParamTypes().get(i);
            if (FrontendTypeSystem.isTwoWord(type)) {
                curr += 2;
            } else {
                curr += 1;
            }
        }
        return curr;
    }

    /**
     * An inner class that implements the GenericDUInfo interface to bridge
     * the gap between the manager's internal indexing and the BCSSA algorithm.
     */
    private class DUInfo implements GenericDUInfo {
        private final BytecodeBlock[] rwToBlock;
        private final LazyArray<List<BytecodeBlock>> defBlocks;
        private final int counter;

        public DUInfo(BytecodeBlock[] rwToBlock, LazyArray<List<BytecodeBlock>> defBlocks, int counter) {
            this.rwToBlock = rwToBlock;
            this.defBlocks = defBlocks;
            this.counter = counter;
        }

        @Override
        public List<BytecodeBlock> getDefBlock(int v) {
            return defBlocks.get(v);
        }

        @Override
        public int getMaxDuIndex() {
            return counter;
        }

        @Override
        public void visit(BytecodeBlock block, DUVisitor visitor) {
            int start1 = start[block.getIndex()];
            int end1 = end[block.getIndex()];
            for (int i = start1; i < end1; ) {
                int index = rwToInsn[i];
                int rw = rwTable[index];
                int var = rw & ((1 << 29) - 1);
                boolean read = (rw & (1 << 29)) != 0;
                boolean write = (rw & (1 << 30)) != 0;
                // careful: the order of visit is important
                // and iinc can both read and write
                if (read) {
                    visitor.visit(i, OccurType.USE, var);
                    i++;
                }
                // don't use `else if`, iinc can both read and write
                if (write) {
                    visitor.visit(i, OccurType.DEF, var);
                    i++;
                }
            }
        }

        @Override
        public BytecodeBlock getBlock(int index) {
            return rwToBlock[index];
        }

        @Override
        public int getParamSize() {
            return paramWriteSize;
        }
    }
}
