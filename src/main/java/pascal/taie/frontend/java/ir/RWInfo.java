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
 * Records all read/write operations about slots in bytecode, use RWIndex to index them.
 */
public class RWInfo {

    /**
     * Type of read/write operations.
     */
    public enum OccurType {
        READ,
        WRITE,
        PARAM
    }

    /**
     * Visitor interface for traversing read/write operations.
     */
    public interface RWVisitor {
        void visit(int rwIndex, OccurType type, int slot);
    }

    /**
     * Number of implicit writes for method parameters at entry.
     */
    private final int paramWriteSize;

    /**
     * Records read/write operations for each bytecode instruction index.
     */
    private final int[] insn2RWInfo;

    /**
     * Total count of variable read/write operations, including parameters.
     */
    private int rwCount;

    /**
     * Maps BlockIndex to the starting RWIndex of its operations.
     */
    private int[] block2StartRW;

    /**
     * Maps BlockIndex to the ending RWIndex of its operations.
     */
    private int[] block2EndRW;

    /**
     * Maps RWIndex to its bytecode instruction index.
     */
    private int[] rw2Insn;

    /**
     * Maps RWIndex to its bytecode block.
     */
    private BytecodeBlock[] rw2Block;

    /**
     * Maps slot to the Blocks that define it.
     */
    private LazyArray<List<BytecodeBlock>> slot2DefBlocks;

    /**
     * {@link #insn2RWInfo} and {@link #rwCount} should be initialized first, and then built by {@link #writeRwTable}
     * They should be built before {@link #build}
     */
    RWInfo(JMethod method, int insnSize) {
        this.paramWriteSize = getParamWriteSize(method);
        this.insn2RWInfo = new int[insnSize];
        this.rwCount = paramWriteSize;
    }

    public List<BytecodeBlock> getDefBlocks(int slot) {
        return slot2DefBlocks.get(slot);
    }

    public int getMaxRWIndex() {
        return rwCount;
    }

    public BytecodeBlock getBlock(int rwIndex) {
        return rw2Block[rwIndex];
    }

    public int getParamSize() {
        return paramWriteSize;
    }

    /**
     * Traverse read/write operations using visitor.
     */
    public void visit(BytecodeBlock block, RWVisitor visitor) {
        int start1 = block2StartRW[block.getIndex()];
        int end1 = block2EndRW[block.getIndex()];
        for (int rwIndex = start1; rwIndex < end1; ) {
            int insnIndex = rw2Insn[rwIndex];
            int rw = insn2RWInfo[insnIndex];
            int slot = rw & ((1 << 29) - 1);
            boolean read = (rw & (1 << 29)) != 0;
            boolean write = (rw & (1 << 30)) != 0;
            // careful: the order of visit is important
            // and iinc can both read and write
            if (read) {
                visitor.visit(rwIndex, OccurType.READ, slot);
                rwIndex++;
            }
            // don't use `else if`, iinc can both read and write
            if (write) {
                visitor.visit(rwIndex, OccurType.WRITE, slot);
                rwIndex++;
            }
        }
    }

    /**
     * Record read/write operations to populate the {@link #insn2RWInfo} and {@link #rwCount}.
     */
    void writeRwTable(int insnIndex, int slot, boolean read) {
        rwCount++;
        assert slot < (1 << 29);
        int rwFlag = read ? 1 << 29 : 1 << 30;
        insn2RWInfo[insnIndex] = insn2RWInfo[insnIndex] | slot | rwFlag;
    }

    int getBlockStartRWIndex(BytecodeBlock block) {
        return block2StartRW[block.getIndex()];
    }

    int getBlockEndRWIndex(BytecodeBlock block) {
        return block2EndRW[block.getIndex()];
    }

    /**
     * Assert the rwIndex is corresponding to the insnIndex and block
     */
    void assertRWIndexValid(int rwIndex, int insnIndex, BytecodeBlock block) {
        assert rw2Insn[rwIndex] == insnIndex;
        assert rwIndex < block2EndRW[block.getIndex()];
    }

    /**
     * Builds the internal indexing structures ({@link #rw2Insn}, {@link #slot2DefBlocks},  etc.)
     */
    void build(BytecodeCFG cfg, int maxLocals) {
        rw2Insn = new int[rwCount];
        block2StartRW = new int[cfg.nodeCount()];
        block2EndRW = new int[cfg.nodeCount()];
        rw2Block = new BytecodeBlock[rwCount];
        slot2DefBlocks = new LazyArray<>(maxLocals) {
            @Override
            protected List<BytecodeBlock> createElement() {
                return new ArrayList<>();
            }
        };

        int rwIndexCounter = 0;
        BytecodeBlock entry = cfg.getEntry();

        for (int i = 0; i < paramWriteSize; ++i) {
            rw2Insn[rwIndexCounter] = -1;
            rw2Block[rwIndexCounter] = entry;
            rwIndexCounter++;
        }

        for (int n = 0; n < cfg.nodeCount(); ++n) {
            BytecodeBlock curr = cfg.getObject(n);
            block2StartRW[curr.getIndex()] = rwIndexCounter;
            int size = curr.getInsns().size();
            int start1 = curr.getInsns().getStart();
            for (int j = 0; j < size; ++j) {
                int i = j + start1;
                int rw = insn2RWInfo[i];
                if (rw != 0) {
                    int slot = rw & ((1 << 29) - 1);
                    boolean read = (rw & (1 << 29)) != 0;
                    boolean write = (rw & (1 << 30)) != 0;
                    if (read) {
                        rw2Block[rwIndexCounter] = curr;
                        rw2Insn[rwIndexCounter++] = i;
                    }
                    if (write) {
                        rw2Block[rwIndexCounter] = curr;
                        rw2Insn[rwIndexCounter++] = i;
                        slot2DefBlocks.get(slot).add(curr);
                    }
                }
            }
            block2EndRW[curr.getIndex()] = rwIndexCounter;
        }
        assert rwIndexCounter == rwCount;
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
