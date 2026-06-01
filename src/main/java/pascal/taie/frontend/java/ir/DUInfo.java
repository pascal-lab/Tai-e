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

import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.LazyArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Records all def/use operations about slots in bytecode, use DUIndex to index them.
 */
public class DUInfo {

    /**
     * Type of def/use operations.
     */
    public enum DUType {
        USE,
        DEF,
        PARAM
    }

    /**
     * Visitor interface for traversing def/use operations.
     */
    public interface DUVisitor {
        void visit(int duIndex, DUType type, int slot);
    }

    /**
     * Number of implicit writes for method parameters at entry.
     */
    private final int paramWriteSize;

    /**
     * Records read/write operations for each bytecode instruction index.
     */
    private final int[] insn2DUInfo;

    /**
     * Total count of variable read/write operations, including parameters.
     */
    private int duCount;

    /**
     * Maps BlockIndex to the starting DUIndex of its operations.
     */
    private int[] block2StartDU;

    /**
     * Maps BlockIndex to the ending DUIndex of its operations.
     */
    private int[] block2EndDU;

    /**
     * Maps DUIndex to its bytecode instruction index.
     */
    private int[] du2Insn;

    /**
     * Maps DUIndex to its bytecode block.
     */
    private BytecodeBlock[] du2Block;

    /**
     * Maps slot to the Blocks that write it.
     */
    private LazyArray<List<BytecodeBlock>> slot2DefBlocks;

    /**
     * {@link #insn2DUInfo} and {@link #duCount} should be initialized first, and then built by {@link #recordDUInfo}
     * By the way, they should be built before {@link #build}.
     */
    DUInfo(JMethod method, int insnSize) {
        this.paramWriteSize = getParamWriteSize(method);
        this.insn2DUInfo = new int[insnSize];
        this.duCount = paramWriteSize;
    }

    public List<BytecodeBlock> getDefBlocks(int slot) {
        return slot2DefBlocks.get(slot);
    }

    public int getMaxDUIndex() {
        return duCount;
    }

    public BytecodeBlock getBlock(int duIndex) {
        return du2Block[duIndex];
    }

    public int getParamSize() {
        return paramWriteSize;
    }

    /**
     * Traverse def/use operations using visitor.
     */
    public void visit(BytecodeBlock block, DUVisitor visitor) {
        int start1 = block2StartDU[block.getIndex()];
        int end1 = block2EndDU[block.getIndex()];
        for (int duIndex = start1; duIndex < end1; ) {
            int insnIndex = du2Insn[duIndex];
            int du = insn2DUInfo[insnIndex];
            int slot = du & ((1 << 29) - 1);
            boolean use = (du & (1 << 29)) != 0;
            boolean def = (du & (1 << 30)) != 0;
            // careful: the order of visit is important
            // and iinc can both use and def
            if (use) {
                visitor.visit(duIndex, DUType.USE, slot);
                duIndex++;
            }
            // don't use `else if`, iinc can both use and def
            if (def) {
                visitor.visit(duIndex, DUType.DEF, slot);
                duIndex++;
            }
        }
    }

    /**
     * Record def/use operations to populate the {@link #insn2DUInfo} and {@link #duCount}.
     */
    void recordDUInfo(int insnIndex, int slot, boolean read) {
        duCount++;
        assert slot < (1 << 29);
        int duFlag = read ? 1 << 29 : 1 << 30;
        insn2DUInfo[insnIndex] = insn2DUInfo[insnIndex] | slot | duFlag;
    }

    int getBlockStartDUIndex(BytecodeBlock block) {
        return block2StartDU[block.getIndex()];
    }

    int getBlockEndDUIndex(BytecodeBlock block) {
        return block2EndDU[block.getIndex()];
    }

    /**
     * Assert the duIndex is corresponding to the insnIndex and block
     */
    void assertDUIndexValid(int duIndex, int insnIndex, BytecodeBlock block) {
        assert du2Insn[duIndex] == insnIndex;
        assert duIndex < block2EndDU[block.getIndex()];
    }

    /**
     * Builds the internal indexing structures ({@link #du2Insn}, {@link #slot2DefBlocks},  etc.).
     * The method should be called after {@link #insn2DUInfo} is built by {@link #recordDUInfo}.
     */
    void build(BytecodeCFG cfg, int maxLocals) {
        du2Insn = new int[duCount];
        block2StartDU = new int[cfg.nodeCount()];
        block2EndDU = new int[cfg.nodeCount()];
        du2Block = new BytecodeBlock[duCount];
        slot2DefBlocks = new LazyArray<>(maxLocals) {
            @Override
            protected List<BytecodeBlock> createElement() {
                return new ArrayList<>();
            }
        };

        int duIndexCounter = 0;
        BytecodeBlock entry = cfg.getEntry();

        for (int i = 0; i < paramWriteSize; ++i) {
            du2Insn[duIndexCounter] = -1;
            du2Block[duIndexCounter] = entry;
            duIndexCounter++;
        }

        for (int n = 0; n < cfg.nodeCount(); ++n) {
            BytecodeBlock curr = cfg.getObject(n);
            block2StartDU[curr.getIndex()] = duIndexCounter;
            int size = curr.getInsns().size();
            int start1 = curr.getInsns().getStart();
            for (int j = 0; j < size; ++j) {
                int i = j + start1;
                int du = insn2DUInfo[i];
                if (du != 0) {
                    int slot = du & ((1 << 29) - 1);
                    boolean read = (du & (1 << 29)) != 0;
                    boolean write = (du & (1 << 30)) != 0;
                    if (read) {
                        du2Block[duIndexCounter] = curr;
                        du2Insn[duIndexCounter++] = i;
                    }
                    if (write) {
                        du2Block[duIndexCounter] = curr;
                        du2Insn[duIndexCounter++] = i;
                        slot2DefBlocks.get(slot).add(curr);
                    }
                }
            }
            block2EndDU[curr.getIndex()] = duIndexCounter;
        }
        assert duIndexCounter == duCount;
    }

    private int getParamWriteSize(JMethod method) {
        int size = method.isStatic() ? 0 : 1;
        for (int i = 0; i < method.getParamTypes().size(); ++i) {
            Type type = method.getParamTypes().get(i);
            if (FrontendTypeSystem.isTwoWord(type)) {
                size += 2;
            } else {
                size += 1;
            }
        }
        return size;
    }
}
