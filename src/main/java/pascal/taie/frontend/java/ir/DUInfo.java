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
 * Def-use information
 */
public class DUInfo {

    public enum OccurType {
        USE,
        DEF,
        PARAM
    }

    public interface DUVisitor {
        void visit(int index, OccurType type, int v);
    }

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
    private BytecodeBlock[] rwToBlock;
    private LazyArray<List<BytecodeBlock>> defBlocks;

    DUInfo(JMethod method, int insnSize) {
        this.paramWriteSize = getParamWriteSize(method);
        this.rwTable = new int[insnSize];
        this.rwCount = paramWriteSize;
    }

    public List<BytecodeBlock> getDefBlock(int v) {
        return defBlocks.get(v);
    }

    // TODO: what is DUIndex???
    public int getMaxDUIndex() {
        return rwCount;
    }

    public BytecodeBlock getBlock(int index) {
        return rwToBlock[index];
    }

    public int getParamSize() {
        return paramWriteSize;
    }

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

    /**
     * Record read/write operations to populate the rwTable.
     */
    // TODO: rename var to slotIndex?
    void writeRwTable(int index, int var, boolean read) {
        rwCount++;
        assert var < (1 << 29);
        int rwFlag = read ? 1 << 29 : 1 << 30;
        rwTable[index] = rwTable[index] | var | rwFlag;
    }

    int getBlockStartRWIndex(BytecodeBlock block) {
        return start[block.getIndex()];
    }

    int getBlockEndRWIndex(BytecodeBlock block) {
        return end[block.getIndex()];
    }

    void assertRWIndexValid(int rwIndex, int insnIndex, BytecodeBlock block) {
        assert rwToInsn[rwIndex] == insnIndex;
        assert rwIndex < end[block.getIndex()];
    }

    void build(BytecodeCFG cfg, int maxLocals) {
        rwToInsn = new int[rwCount];
        start = new int[cfg.nodeCount()];
        end = new int[cfg.nodeCount()];
        rwToBlock = new BytecodeBlock[rwCount];
        defBlocks = new LazyArray<>(maxLocals) {
            @Override
            protected List<BytecodeBlock> createElement() {
                return new ArrayList<>();
            }
        };

        int rwIndexCounter = 0;
        BytecodeBlock entry = cfg.getEntry();

        for (int i1 = 0; i1 < paramWriteSize; ++i1) {
            rwToInsn[rwIndexCounter] = -1;
            rwToBlock[rwIndexCounter] = entry;
            rwIndexCounter++;
        }

        for (int n = 0; n < cfg.nodeCount(); ++n) {
            BytecodeBlock curr = cfg.getObject(n);
            start[curr.getIndex()] = rwIndexCounter;
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
                        rwToBlock[rwIndexCounter] = curr;
                        rwToInsn[rwIndexCounter++] = i1;
                    }
                    if (write) {
                        rwToBlock[rwIndexCounter] = curr;
                        rwToInsn[rwIndexCounter++] = i1;
                        defBlocks.get(var).add(curr);
                    }
                }
            }
            end[curr.getIndex()] = rwIndexCounter;
        }
    }

    private int getParamWriteSize(JMethod method) {
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
}
