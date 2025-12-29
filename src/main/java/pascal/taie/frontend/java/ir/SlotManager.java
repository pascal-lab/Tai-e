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
import java.util.function.BiConsumer;

import org.objectweb.asm.tree.AbstractInsnNode;

import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.frontend.java.ir.ssa.BCSSA;
import pascal.taie.frontend.java.ir.ssa.GenericDUInfo;
import pascal.taie.frontend.java.ir.ssa.VarSSAInfo;
import pascal.taie.ir.exp.ExpMutator;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.LazyArray;
import pascal.taie.util.graph.Dominators;

public class SlotManager {

    private int rwCount;
    private final int[] rwTable;
    private final int paramWrite;

    int[] start;
    int[] end;
    int[] rwToIndex;
    int currRw;

    BCSSA splitting;
    Var[] reachVars;

    private final JMethod method;
    private final VarManager varManager;
    private final boolean isSSA;
    private final VarSSAInfo varSSAInfo;
    private final int maxLocals;

    public SlotManager(JMethod method, VarManager varManager, boolean isSSA, VarSSAInfo varSSAInfo, int maxLocals, int insnSize) {
        this.method = method;
        this.varManager = varManager;
        this.isSSA = isSSA;
        this.varSSAInfo = varSSAInfo;
        this.maxLocals = maxLocals;
        this.paramWrite = getParamWriteSize();
        this.rwTable = new int[insnSize];
        this.rwCount = getParamWriteSize();
    }

    void writeRwTable(int index, int var, boolean read) {
        rwCount++;
        assert var < (1 << 29);
        int rwFlag = read ? 1 << 29 : 1 << 30;
        rwTable[index] = rwTable[index] | var | rwFlag;
    }

    private int getParamWriteSize() {
        int curr = method.isStatic() ? 0 : 1;
        for (int i = 0; i < method.getParamTypes().size(); ++i) {
            Type type = method.getParamTypes().get(i);
            if (FrontendTypeSystem.isTwoWord(type)) {
                curr += 2;
            } else {
                curr += 1;
            }
        }
        return curr;
    }

    void postProcess(BytecodeCFG cfg, Dominators<BytecodeBlock> dom) {
        rwToIndex = new int[rwCount];
        BytecodeBlock[] rwToBlock = new BytecodeBlock[rwCount];
        int counter = 0;
        BytecodeBlock entry = cfg.getEntry();
        start = new int[cfg.nodeCount()];
        end = new int[cfg.nodeCount()];

        LazyArray<List<BytecodeBlock>> defBlocks = new LazyArray<>(maxLocals) {
            @Override
            protected List<BytecodeBlock> createElement() {
                return new ArrayList<>();
            }
        };

        for (int i = 0; i < paramWrite; ++i) {
            rwToIndex[counter] = -1;
            rwToBlock[counter] = entry;
            counter++;
        }

        for (int n = 0; n < cfg.nodeCount(); ++n) {
            BytecodeBlock curr = cfg.getObject(n);
            start[curr.getIndex()] = counter;
            int size = curr.getInsns().size();
            int start1 = curr.getInsns().getStart();
            for (int j = 0; j < size; ++j) {
                int i = j + start1;
                int rw = rwTable[i];
                if (rw != 0) {
                    int var = rw & ((1 << 29) - 1);
                    boolean read = (rw & (1 << 29)) != 0;
                    boolean write = (rw & (1 << 30)) != 0;
                    if (read) {
                        rwToBlock[counter] = curr;
                        rwToIndex[counter++] = i;
                    }
                    if (write) {
                        rwToBlock[counter] = curr;
                        rwToIndex[counter++] = i;
                        defBlocks.get(var).add(curr);
                    }
                }
            }
            end[curr.getIndex()] = counter;
        }

        int finalCounter = counter;
        GenericDUInfo genericDUInfo = new GenericDUInfo() {
            @Override
            public List<BytecodeBlock> getDefBlock(int v) {
                return defBlocks.get(v);
            }

            @Override
            public int getMaxDuIndex() {
                return finalCounter;
            }

            @Override
            public void visit(BytecodeBlock block, DUVisitor visitor) {
                int start1 = start[block.getIndex()];
                int end1 = end[block.getIndex()];
                for (int i = start1; i < end1; ) {
                    int index = rwToIndex[i];
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
                return paramWrite;
            }
        };

        splitting = new BCSSA(cfg, maxLocals, genericDUInfo, isSSA, dom);
        splitting.build();
        reachVars = new Var[splitting.getMaxDUCount()];
        if (!isSSA) {
            varManager.enlargeLocal(splitting.getRealLocalCount(), splitting.getVarMappingTable());
        }
        // ensure all params is defined at beginning
        for (int i = 0; i < paramWrite; ++i) {
            if (isFastProcessVar(i)) {
                reachVars[i] = varManager.getLocal(i);
                Var current = reachVars[i];
                if (splitting.canFastProcess(i)) {
                    varSSAInfo.setSSA(current);
                } else {
                    varSSAInfo.setNonSSA(current);
                }
            }
        }
    }

    void enter(int block) {
        currRw = start[block];
    }

    int visitRW(int block, int index) {
        assert rwToIndex[currRw] == index;
        assert currRw < end[block];
        return currRw++;
    }

    void exit(int block) {
        assert currRw == end[block];
    }

    Var getRWVar(int rwIndex, int slot, AbstractInsnNode insn) {
        Var v;
        int defIndex = splitting.getReachDef(rwIndex);
        assert defIndex != -1; // wtf? undefined variable?
        if (isFastProcessVar(defIndex)) {
            v = reachVars[defIndex];
        } else {
            int realVar = splitting.getRealLocalSlot(defIndex);
            assert realVar != -1; // must be phi-connected insn, a local is assigned before
            v = varManager.getLocal(realVar);
        }
        assert v != null;
        tryFixVarName(v, slot, insn);
        return v;
    }

    void storeRWVar(int rwIndex, int slot, AbstractInsnNode insn, OperandStack operandStack, BiConsumer<AbstractInsnNode, Stmt> assocStmt) {
        Var v;
        if (!splitting.isDefUsed(rwIndex)) {
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
            if (varManager.isLocal(v) && !varSSAInfo.isSSAVar(v) && !isSSA) {
                Var origin = v;
                v = varManager.getTempVar();
                assocStmt.accept(insn, Utils.newAssignStmt(method, v, origin));
            }
            reachVars[rwIndex] = v;
        } else {
            // still use a local var
            int realVar = splitting.getRealLocalSlot(rwIndex);
            v = varManager.getLocal(realVar);
            // use this to generate store stmt
            assert v != null;
            Stmt stmt = operandStack.popToVar(v);
            assocStmt.accept(insn, stmt);
        }
        tryFixVarName(v, slot, insn);
    }

    boolean isFastProcessVar(int v) {
        return isSSA || splitting.canFastProcess(v);
    }

    private void tryFixVarName(Var v, int slot, AbstractInsnNode insn) {
        if (varManager.existsLocalVariableTable && VarManager.mayRename(v)) {
            Optional<String> name = varManager.getName(slot, insn);
            name.ifPresent((n) -> {
                String realName = varManager.tryUseName(n);
                ExpMutator.setName(v, realName);
            });
        }
    }
}
