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

package pascal.taie.frontend.java.ir.ssa;

import pascal.taie.frontend.java.ir.BytecodeBlock;
import pascal.taie.frontend.java.ir.DUInfo;
import pascal.taie.util.collection.IntList;
import pascal.taie.util.collection.LazyArray;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.graph.Dominators;
import pascal.taie.util.graph.IndexedGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;

public class BCSSA {

    public class SemiPhi {
        final int slot;

        final IntList inDefs;

        final List<BytecodeBlock> inBlocks = new ArrayList<>();

        final List<SemiPhi> owned = new ArrayList<>();

        final BytecodeBlock belongsTo;

        final int index;

        boolean used = false;

        boolean valid = true;

        int newName;

        Object realPhi;

        SemiPhi(int slot, IntList inDefs, BytecodeBlock belongsTo, int index) {
            this.slot = slot;
            this.inDefs = inDefs;
            this.belongsTo = belongsTo;
            this.index = index;
        }

        void addInDefs(BytecodeBlock b, int reachDef) {
            inDefs.add(reachDef);
            inBlocks.add(b);
            if (isPhiDef(reachDef)) {
                SemiPhi p = getPhiByIndex(reachDef);
                p.owned.add(this);
            } else {
                if (def2OwnedDU[reachDef] == null) {
                    def2OwnedDU[reachDef] = new IntList(4);
                }
                def2OwnedDU[reachDef].add(getPhiDUIndex(index));
            }
        }

        void setUsed() {
            assert valid;
            used = true;
        }

        void setInvalid() {
            this.valid = false;
        }

        public int getSlot() {
            return slot;
        }

        public IntList getInDefs() {
            return inDefs;
        }

        public List<BytecodeBlock> getInBlocks() {
            return inBlocks;
        }

        public boolean isUsed() {
            return used;
        }

        public int getDUIndex() {
            return getPhiDUIndex(index);
        }

        public void setRealPhi(Object realPhi) {
            this.realPhi = realPhi;
        }

        public Object getRealPhi() {
            return realPhi;
        }
    }

    private final IndexedGraph<BytecodeBlock> cfg;

    private final Dominators.DominatorFrontiers df;

    private final Dominators<BytecodeBlock> dom;

    private final LazyArray<List<SemiPhi>> block2phis;

    private final DUInfo duInfo;

    private int[] duReachDef;

    // duReachDef[i] = j, j def dom i

    private static final int UNDEFINED = -1;

    private final int slotSize;

    // TODO: better name
    private final int[] renames;

    private int newMaxLocal;

    private int[] varMappingTable;

    private final IntList[] def2OwnedDU;

    private final List<SemiPhi> allPhis;

    private final boolean isSSA;

    private final boolean[] usedDef;

    // TODO: Rename var to slot?
    public BCSSA(IndexedGraph<BytecodeBlock> cfg,
                 int slotSize,
                 DUInfo duInfo,
                 boolean isSSA,
                 Dominators<BytecodeBlock> dom) {
        this.cfg = cfg;
        this.slotSize = slotSize;
        this.dom = dom;
        this.df = dom.getDomFront();
        this.duInfo = duInfo;
        this.block2phis = new LazyArray<>(cfg.nodeCount()) {
            @Override
            protected List<SemiPhi> createElement() {
                return new ArrayList<>();
            }
        };
        this.allPhis = new ArrayList<>();
        this.renames = new int[duInfo.getMaxDUIndex()];
        this.isSSA = isSSA;
        this.usedDef = new boolean[duInfo.getMaxDUIndex()];
        Arrays.fill(renames, UNDEFINED);
        this.def2OwnedDU = new IntList[duInfo.getMaxDUIndex()];
    }

    public void build() {
        int phiCount = phiInsertion();
        travLink(phiCount);
        pruneAndRenaming(phiCount);
    }

    private void spreadingUsed(SemiPhi phi) {
        for (int i = 0; i < phi.inDefs.size(); ++i) {
            int def = phi.inDefs.get(i);
            if (isPhiDef(def)) {
                SemiPhi p = getPhiByIndex(def);
                if (!p.used) {
                    p.setUsed();
                    spreadingUsed(p);
                }
            } else {
                usedDef[def] = true;
            }
        }
    }

    private void pruneAndRenaming(int phiCount) {
        // pass 1: prune, or spreading `used` flag
        for (int i = 0; i < cfg.nodeCount(); i++) {
            if (!block2phis.contains(i)) {
                continue;
            }
            for (SemiPhi phi : block2phis.get(i)) {
                if (phi.used) {
                    spreadingUsed(phi);
                }
            }
        }
        // pass 2: generate new names for each cluster and spreading through used phi functions
        // only needed when perform splitting instead of SSA
        if (!isSSA) {
            paramToName = new int[duInfo.getMaxDUIndex()];
            Arrays.fill(paramToName, UNDEFINED);
            int varIndex = 0;
            boolean[] visited = new boolean[phiCount];
            Map<Integer, Integer> varIndexToOriginSlot = Maps.newMap();
            for (int i = 0; i < cfg.nodeCount(); i++) {
                if (!block2phis.contains(i)) {
                    continue;
                }
                for (SemiPhi phi : block2phis.get(i)) {
                    if (phi.used && !visited[phi.index]) {
                        varIndexToOriginSlot.put(varIndex, phi.slot);
                        biDfs(phi, visited, varIndex);
                        varIndex++;
                    }
                }
            }

            reSlot = new int[varIndex];
            Arrays.fill(reSlot, UNDEFINED);
            boolean[] useOriginSlot = new boolean[slotSize];
            for (int i = 0; i < duInfo.getParamSize(); i++) {
                if (paramToName[i] != UNDEFINED) {
                    reSlot[paramToName[i]] = i;
                    useOriginSlot[i] = true;
                }
            }
            newMaxLocal = slotSize;
            Map<Integer, Integer> newSlotToOldSlot = Maps.newMap();
            for (int i = 0; i < varIndex; i++) {
                if (reSlot[i] == UNDEFINED) {
                    int oldSlot = varIndexToOriginSlot.get(i);
                    // param slot should not be reused
                    if (!useOriginSlot[oldSlot] && oldSlot >= duInfo.getParamSize()) {
                        useOriginSlot[oldSlot] = true;
                        reSlot[i] = oldSlot;
                    } else {
                        reSlot[i] = newMaxLocal++;
                        newSlotToOldSlot.put(reSlot[i], oldSlot);
                    }
                }
            }

            varMappingTable = new int[newMaxLocal];
            for (int i = 0; i < newMaxLocal; i++) {
                varMappingTable[i] = newSlotToOldSlot.getOrDefault(i, i);
            }
        }
    }

    private int[] reSlot;

    private int[] paramToName;

    void biDfs(SemiPhi phi, boolean[] visited, int varIndex) {
        if (!phi.used) {
            return;
        }
        if (visited[phi.index]) {
            return;
        }
        visited[phi.index] = true;
        phi.newName = varIndex;
        for (SemiPhi p : phi.owned) {
            biDfs(p, visited, varIndex);
        }

        for (int i = 0; i < phi.inDefs.size(); i++) {
            int def = phi.inDefs.get(i);
            assert def != -1;
            if (isPhiDef(def)) {
                SemiPhi p = getPhiByIndex(def);
                biDfs(p, visited, varIndex);
            } else {
                renames[def] = varIndex;
                if (def < duInfo.getParamSize()) {
                    paramToName[def] = varIndex;
                }
                if (def2OwnedDU[def] != null) {
                    IntList ownedList = def2OwnedDU[def];
                    for (int j = 0; j < ownedList.size(); ++j) {
                        int owned = ownedList.get(j);
                        if (!visited[getPhiByIndex(owned).index]) {
                            biDfs(getPhiByIndex(owned), visited, varIndex);
                        }
                    }
                }
            }
        }
    }

    private void travLink(int phiCount) {
        duReachDef = new int[duInfo.getMaxDUIndex() + phiCount];
        int[] varReachDef = new int[slotSize];
        Arrays.fill(duReachDef, UNDEFINED);
        Arrays.fill(varReachDef, UNDEFINED);
        DUInfo.DUVisitor varDUVisitor = (duIndex, type, slot) -> {
            if (type == DUInfo.OccurType.USE) {
                int before = varReachDef[slot];
                assert before > -100000;
                updateReachingDef(slot, duIndex, varReachDef);
                int reachDef = varReachDef[slot];
                assert reachDef != UNDEFINED;
                duReachDef[duIndex] = reachDef;
                if (isPhiDef(reachDef)) {
                    SemiPhi phi = getPhiByIndex(duReachDef[duIndex]);
                    phi.setUsed();
                } else {
                    usedDef[reachDef] = true;
                }
            } else {
                updateReachingDef(slot, duIndex, varReachDef);
                duReachDef[duIndex] = varReachDef[slot];
                varReachDef[slot] = duIndex;
            }
        };

        // before starting, we need to inject params
        for (int i = 0; i < duInfo.getParamSize(); i++) {
            // params are always defined at the beginning of the method
            // and should be labeled as 0, 1, 2, 3, ... in the du index
            varReachDef[i] = i;
            // if the param is used in a phi node, we need to update the reaching def
            if (!block2phis.contains(cfg.getIndex(cfg.getEntry()))) {
                continue;
            }
            for (SemiPhi phi : block2phis.get(cfg.getIndex(cfg.getEntry()))) {
                if (phi.slot == i) {
                    phi.addInDefs(null, i);
                }
            }
        }
        for (BytecodeBlock current : dom.getDomTreePreOrder()) {
            int node = cfg.getIndex(current);
            if (block2phis.contains(node)) {
                for (SemiPhi phi : block2phis.get(node)) {
                    int phiIndex = getPhiDUIndex(phi.index);
                    updateReachingDef(phi.slot, phiIndex, varReachDef);
                    duReachDef[phiIndex] = varReachDef[phi.slot];
                    varReachDef[phi.slot] = phiIndex;
                }
            }
            duInfo.visit(current, varDUVisitor);
            for (BytecodeBlock succ : cfg.getSuccsOf(current)) {
                int succI = cfg.getIndex(succ);
                if (!block2phis.contains(succI)) {
                    continue;
                }
                for (SemiPhi phi : block2phis.get(succI)) {
                    int varIndex = phi.slot;
                    updateReachingDefForBlockEnd(varIndex, varReachDef, current);
                    int reachDef = varReachDef[varIndex];
                    if (reachDef == UNDEFINED) {
                        phi.setInvalid();
                    } else {
                        phi.addInDefs(current, reachDef);
                    }
                }
            }
        }
    }

    public void updateReachingDefForBlockEnd(int v, int[] varReachDef, BytecodeBlock block) {
        int r = varReachDef[v];
        if (r == UNDEFINED) {
            return;
        }
        BytecodeBlock b = getDuBlocks(r);
        if (b == block) {
            return;
        } else {
            while (!dom.dominates(b, block)) {
                r = duReachDef[r];
                if (r == UNDEFINED) {
                    break;
                }
                b = getDuBlocks(r);
            }
        }
        varReachDef[v] = r;
    }

    public void updateReachingDef(int v, int insnIndex, int[] varReachDef) {
        int r = varReachDef[v];
        while (!(r == UNDEFINED || dominates(r, insnIndex))) {
            r = duReachDef[r];
        }
        varReachDef[v] = r;
    }

    /**
     * Check if insnIndex1 dominates insnIndex2
     */
    private boolean dominates(int insnIndex1, int insnIndex2) {
        BytecodeBlock b1 = getDuBlocks(insnIndex1);
        BytecodeBlock b2 = getDuBlocks(insnIndex2);
        if (b1 == b2) {
            return isPhiDef(insnIndex1) || insnIndex1 < insnIndex2;
        } else {
            return dom.dominates(b1, b2);
        }
    }

    private BytecodeBlock getDuBlocks(int udIndex) {
        if (!isPhiDef(udIndex)) {
            return duInfo.getBlock(udIndex);
        } else {
            return getPhiByIndex(udIndex).belongsTo;
        }
    }

    private boolean isPhiDef(int duIndex) {
        return duIndex >= duInfo.getMaxDUIndex();
    }

    private int getPhiDUIndex(int phiIndex) {
        return phiIndex + duInfo.getMaxDUIndex();
    }

    private SemiPhi getPhiByIndex(int duIndex) {
        int phiIndex = duIndex - duInfo.getMaxDUIndex();
        return allPhis.get(phiIndex);
    }

    private int phiInsertion() {
        int phiCount = 0;

        Queue<Integer> current = new ArrayDeque<>();
        for (int slot = 0; slot < slotSize; ++slot) {
            List<BytecodeBlock> defBlocks = duInfo.getDefBlocks(slot);
            for (BytecodeBlock block : defBlocks) {
                current.add(cfg.getIndex(block));
            }
            while (!current.isEmpty()) {
                BytecodeBlock block = cfg.getObject(current.poll());
                for (int node : df.get(cfg.getIndex(block))) {
                    if (!isInserted(node, slot)) {
                        SemiPhi phi = new SemiPhi(slot, new IntList(4), cfg.getObject(node), phiCount++);
                        block2phis.get(node).add(phi);
                        allPhis.add(phi);
                        assert phiCount == allPhis.size();
                        current.add(node);
                    }
                }
            }
        }
        return phiCount;
    }

    private boolean isInserted(int node, int v) {
        if (!block2phis.contains(node)) {
            return false;
        }
        for (SemiPhi phi : block2phis.get(node)) {
            if (phi.slot == v) {
                return true;
            }
        }
        return false;
    }

    public int getRealLocalCount() {
        return newMaxLocal;
    }

    public int getRealLocalName(int duIndex) {
        if (duIndex >= renames.length) {
            // this is a phi node
            return getPhiByIndex(duIndex).newName;
        }
        return renames[duIndex];
    }

    public int getRealLocalSlot(int duIndex) {
        if (duIndex < duInfo.getParamSize()) {
            return duIndex;
        }
        return reSlot[getRealLocalName(duIndex)];
    }

    public boolean canFastProcess(int duIndex) {
        return getRealLocalName(duIndex) == UNDEFINED;
    }

    public int[] getVarMappingTable() {
        return varMappingTable;
    }

    public int getReachDef(int duIndex) {
        return duReachDef[duIndex];
    }

    public void visitLivePhis(BytecodeBlock block, Consumer<SemiPhi> consumer) {
        int index = cfg.getIndex(block);
        if (!block2phis.contains(index)) {
            return;
        }
        for (SemiPhi phi : block2phis.get(index)) {
            if (phi.used) {
                consumer.accept(phi);
            }
        }
    }

    public int getMaxDUCount() {
        return duInfo.getMaxDUIndex() + allPhis.size();
    }

    public boolean isDefUsed(int def) {
        // we must ensure if def1 and def2 belongs to the same phi-web,
        // they should have the same used flag
        // this will be ensured by the algorithm,
        // in stage 2, only the def has been used will be marked as used will be visited
        if (!isPhiDef(def)) {
            return usedDef[def];
        } else {
            return getPhiByIndex(def).used;
        }
    }
}
