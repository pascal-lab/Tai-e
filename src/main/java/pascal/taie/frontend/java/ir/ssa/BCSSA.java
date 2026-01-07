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

// TODO: comment about phi duIndex. It will contains semiPhis
public class BCSSA {

    public class SemiPhi {

        /**
         * the slot this phi corresponds to
         */
        private final int slot;

        /**
         * the block this phi belongs to
         */
        private final BytecodeBlock block;

        /**
         * the index of this phi in allPhis list
         */
        private final int phiIndex;

        /**
         * the input defs
         */
        private final IntList inDefs = new IntList(4);

        /**
         * the blocks of input defs
         */
        private final List<BytecodeBlock> inBlocks = new ArrayList<>();

        /**
         * the phis that use it as input
         */
        private final List<SemiPhi> outPhis = new ArrayList<>();

        /**
         * whether this phi is used
         */
        private boolean used = false;

        /**
         * whether this phi is valid, i.e., all its input defs are not UNDEFINED
         */
        private boolean valid = true;

        private int clusterId;

        private FrontendPhiStmt frontendPhi;

        SemiPhi(int slot, BytecodeBlock block, int phiIndex) {
            this.slot = slot;
            this.block = block;
            this.phiIndex = phiIndex;
        }

        void addInDefs(BytecodeBlock block, int defIndex) {
            inDefs.add(defIndex);
            inBlocks.add(block);
            if (isPhiDef(defIndex)) {
                SemiPhi p = getPhiByIndex(defIndex);
                p.outPhis.add(this);
            } else {
                if (def2Phis[defIndex] == null) {
                    def2Phis[defIndex] = new IntList(4);
                }
                def2Phis[defIndex].add(BCSSA.this.getPhiDUIndex(phiIndex));
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

        public int getPhiDUIndex() {
            return BCSSA.this.getPhiDUIndex(phiIndex);
        }

        public void setFrontendPhi(FrontendPhiStmt frontendPhi) {
            this.frontendPhi = frontendPhi;
        }

        public FrontendPhiStmt getFrontendPhi() {
            return frontendPhi;
        }
    }

    private final IndexedGraph<BytecodeBlock> cfg;

    private final Dominators.DominatorFrontiers df;

    private final Dominators<BytecodeBlock> dom;

    private final LazyArray<List<SemiPhi>> block2phis;

    private final DUInfo duInfo;

    /**
     * du2ReachDef[i] = j, if duIndex j is the reaching definition of duIndex i
     * <ul>
     *   <li> If duIndex i is a use, then j is the def that i will use. </li>
     *   <li> If duIndex i is a def, then j is the previous def that i shadows. </li>
     * </ul>
     */
    private int[] du2ReachDef;

    private static final int UNDEFINED = -1;

    private final int slotSize;

    private final int[] def2ClusterId;

    private final int[] param2ClusterId;

    private int newSlotSize;

    private int[] clusterId2NewSlot;

    private int[] newSlot2Origin;

    /**
     * def2Phis[i] = list of phi DUIndexes that use the def i
     */
    private final IntList[] def2Phis;

    private final List<SemiPhi> allPhis;

    private final boolean isSSA;

    /**
     * defUsed[i] = true, if the def i is used.
     * Only for non-phi defs, the usage about phis are recorded in {@link SemiPhi#used} flag.
     */
    private final boolean[] defUsed;

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
        this.def2ClusterId = new int[duInfo.getMaxDUIndex()];
        this.isSSA = isSSA;
        this.defUsed = new boolean[duInfo.getMaxDUIndex()];
        Arrays.fill(def2ClusterId, UNDEFINED);
        this.def2Phis = new IntList[duInfo.getMaxDUIndex()];
        this.newSlotSize = slotSize;
        this.param2ClusterId = new int[duInfo.getParamSize()];
        Arrays.fill(param2ClusterId, UNDEFINED);
    }

    public void build() {
        insertSemiPhi();
        buildReachDefs();
        spreadPhiUsed();
        if (!isSSA) {
            reSlot();
        }
    }

    private void spreadPhiUsed(SemiPhi phi) {
        for (int i = 0; i < phi.inDefs.size(); ++i) {
            int def = phi.inDefs.get(i);
            if (isPhiDef(def)) {
                SemiPhi p = getPhiByIndex(def);
                if (!p.isUsed()) {
                    p.setUsed();
                    spreadPhiUsed(p);
                }
            } else {
                defUsed[def] = true;
            }
        }
    }

    private void reSlot() {
        // generate new slots for each cluster and spreading through used phi functions
        int clusterIdCounter = 0;
        boolean[] visited = new boolean[allPhis.size()];
        Map<Integer, Integer> clusterId2OriginSlot = Maps.newMap();
        for (int i = 0; i < cfg.nodeCount(); i++) {
            if (!block2phis.contains(i)) {
                continue;
            }
            for (SemiPhi phi : block2phis.get(i)) {
                if (phi.isUsed() && !visited[phi.phiIndex]) {
                    clusterId2OriginSlot.put(clusterIdCounter, phi.slot);
                    findCluster(phi, visited, clusterIdCounter);
                    clusterIdCounter++;
                }
            }
        }

        clusterId2NewSlot = new int[clusterIdCounter];
        Arrays.fill(clusterId2NewSlot, UNDEFINED);
        boolean[] useOriginSlot = new boolean[slotSize];
        for (int i = 0; i < duInfo.getParamSize(); i++) {
            if (param2ClusterId[i] != UNDEFINED) {
                clusterId2NewSlot[param2ClusterId[i]] = i;
                useOriginSlot[i] = true;
            }
        }
        Map<Integer, Integer> newSlot2Origin = Maps.newMap();
        for (int i = 0; i < clusterIdCounter; i++) {
            if (clusterId2NewSlot[i] == UNDEFINED) {
                int oldSlot = clusterId2OriginSlot.get(i);
                // param slot should not be reused
                if (!useOriginSlot[oldSlot] && oldSlot >= duInfo.getParamSize()) {
                    useOriginSlot[oldSlot] = true;
                    clusterId2NewSlot[i] = oldSlot;
                } else {
                    clusterId2NewSlot[i] = newSlotSize++;
                    newSlot2Origin.put(clusterId2NewSlot[i], oldSlot);
                }
            }
        }

        this.newSlot2Origin = new int[newSlotSize];
        for (int i = 0; i < newSlotSize; i++) {
            this.newSlot2Origin[i] = newSlot2Origin.getOrDefault(i, i);
        }
    }

    private void spreadPhiUsed() {
        for (int i = 0; i < cfg.nodeCount(); i++) {
            if (!block2phis.contains(i)) {
                continue;
            }
            for (SemiPhi phi : block2phis.get(i)) {
                if (phi.isUsed()) {
                    spreadPhiUsed(phi);
                }
            }
        }
    }

    // use biDfs
    void findCluster(SemiPhi phi, boolean[] visited, int clusterId) {
        if (!phi.isUsed()) {
            return;
        }
        if (visited[phi.phiIndex]) {
            return;
        }
        visited[phi.phiIndex] = true;
        phi.clusterId = clusterId;

        for (SemiPhi p : phi.outPhis) {
            findCluster(p, visited, clusterId);
        }

        for (int i = 0; i < phi.inDefs.size(); i++) {
            int def = phi.inDefs.get(i);
            assert def != -1;
            if (isPhiDef(def)) {
                SemiPhi p = getPhiByIndex(def);
                findCluster(p, visited, clusterId);
            } else {
                def2ClusterId[def] = clusterId;
                if (def < duInfo.getParamSize()) {
                    param2ClusterId[def] = clusterId;
                }
                if (def2Phis[def] != null) {
                    IntList phis = def2Phis[def];
                    for (int j = 0; j < phis.size(); ++j) {
                        SemiPhi p = getPhiByIndex(phis.get(j));
                        if (!visited[p.phiIndex]) {
                            findCluster(p, visited, clusterId);
                        }
                    }
                }
            }
        }
    }

    private void buildReachDefs() {
        du2ReachDef = new int[getMaxDUIndexWithPhi()];
        int[] slot2LatestDef = new int[slotSize];
        Arrays.fill(du2ReachDef, UNDEFINED);
        Arrays.fill(slot2LatestDef, UNDEFINED);

        // before starting, inject reach defs caused by params
        for (int i = 0; i < duInfo.getParamSize(); i++) {
            // params are always defined at the beginning of the method
            // and should be labeled as 0, 1, 2, 3, ... in the DUIndex
            slot2LatestDef[i] = i;
            // if the param is used in a phi node in entry, we need to update the reaching def
            if (!block2phis.contains(cfg.getIndex(cfg.getEntry()))) {
                continue;
            }
            for (SemiPhi phi : block2phis.get(cfg.getIndex(cfg.getEntry()))) {
                if (phi.slot == i) {
                    phi.addInDefs(null, i);
                }
            }
        }

        for (BytecodeBlock block : dom.getDomTreePreOrder()) {
            // 1. make reach defs for phis in this block (phi is a def)
            int blockIndex = cfg.getIndex(block);
            if (block2phis.contains(blockIndex)) {
                for (SemiPhi phi : block2phis.get(blockIndex)) {
                    int phiDUIndex = getPhiDUIndex(phi.phiIndex);
                    du2ReachDef[phiDUIndex] = getSlotLatestDef(phi.slot, slot2LatestDef, phiDUIndex);
                    slot2LatestDef[phi.slot] = phiDUIndex;
                }
            }

            // 2. make reach defs for normal def/use operations in this block
            duInfo.visit(block, (duIndex, type, slot) -> {
                int reachDef = getSlotLatestDef(slot, slot2LatestDef, duIndex);
                if (type == DUInfo.DUType.USE) {
                    assert reachDef != UNDEFINED;
                    du2ReachDef[duIndex] = reachDef;
                    if (isPhiDef(reachDef)) {
                        SemiPhi phi = getPhiByIndex(reachDef);
                        phi.setUsed();
                    } else {
                        defUsed[reachDef] = true;
                    }
                } else {
                    du2ReachDef[duIndex] = reachDef;
                    slot2LatestDef[slot] = duIndex;
                }
            });

            // 3. add indefs to the phis in the successor blocks
            for (BytecodeBlock succ : cfg.getSuccsOf(block)) {
                int succIndex = cfg.getIndex(succ);
                if (!block2phis.contains(succIndex)) {
                    continue;
                }
                for (SemiPhi phi : block2phis.get(succIndex)) {
                    int reachDef = getSlotLatestDef(phi.slot, slot2LatestDef, block);
                    if (reachDef == UNDEFINED) {
                        phi.setInvalid();
                    } else {
                        phi.addInDefs(block, reachDef);
                    }
                }
            }
        }
    }

    /**
     * Get the reach def for slot.
     * We CAN NOT just return slot2LatestDef[slot].
     * Instead, we should find the reach def that dominates the block along the def chain.
     */
    private int getSlotLatestDef(int slot, int[] slot2LatestDef, BytecodeBlock block) {
        int def = slot2LatestDef[slot];
        if (def == UNDEFINED || getBlock(def) == block) {
            return def;
        }
        BytecodeBlock defBlock = getBlock(def);
        while (!(defBlock != block && dom.dominates(defBlock, block))) {
            def = du2ReachDef[def];
            if (def == UNDEFINED) {
                break;
            }
            defBlock = getBlock(def);
        }
        slot2LatestDef[slot] = def;
        return def;
    }

    /**
     * Get the reach def for slot.
     * We CAN NOT just return slot2LatestDef[slot].
     * Instead, we should find the reach def that dominates duIndex along the def chain.
     */
    private int getSlotLatestDef(int slot, int[] slot2LatestDef, int duIndex) {
        int def = slot2LatestDef[slot];
        // find the reaching def that dominates duIndex, or UNDEFINED
        while (!(def == UNDEFINED || dominates(def, duIndex))) {
            def = du2ReachDef[def];
        }
        slot2LatestDef[slot] = def;
        return def;
    }

    /**
     * Check if duIndex1 dominates duIndex2
     */
    private boolean dominates(int duIndex1, int duIndex2) {
        BytecodeBlock b1 = getBlock(duIndex1);
        BytecodeBlock b2 = getBlock(duIndex2);
        if (b1 == b2) {
            return isPhiDef(duIndex1) || duIndex1 < duIndex2;
        } else {
            return dom.dominates(b1, b2);
        }
    }

    private BytecodeBlock getBlock(int duIndex) {
        if (!isPhiDef(duIndex)) {
            return duInfo.getBlock(duIndex);
        } else {
            return getPhiByIndex(duIndex).block;
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

    private void insertSemiPhi() {
        for (int slot = 0; slot < slotSize; ++slot) {
            // the indexes of blocks (if they have phi stmts) that define the slot (phi is also a definition)
            Queue<Integer> worklist = new ArrayDeque<>();
            List<BytecodeBlock> defBlocks = duInfo.getDefBlocks(slot);
            for (BytecodeBlock block : defBlocks) {
                worklist.add(cfg.getIndex(block));
            }
            while (!worklist.isEmpty()) {
                BytecodeBlock block = cfg.getObject(worklist.poll());
                for (int dfBlock : df.get(cfg.getIndex(block))) {
                    if (!isPhiInserted(dfBlock, slot)) {
                        SemiPhi phi = new SemiPhi(slot, cfg.getObject(dfBlock), allPhis.size());
                        block2phis.get(dfBlock).add(phi);
                        allPhis.add(phi);
                        worklist.add(dfBlock);
                    }
                }
            }
        }
    }

    private boolean isPhiInserted(int block, int slot) {
        if (!block2phis.contains(block)) {
            return false;
        }
        for (SemiPhi phi : block2phis.get(block)) {
            if (phi.slot == slot) {
                return true;
            }
        }
        return false;
    }

    public int getNewSlotSize() {
        return newSlotSize;
    }

    private int getClusterId(int duIndex) {
        if (duIndex >= def2ClusterId.length) {
            // this is a phi node
            return getPhiByIndex(duIndex).clusterId;
        }
        return def2ClusterId[duIndex];
    }

    public int getNewSlot(int duIndex) {
        if (duIndex < duInfo.getParamSize()) {
            return duIndex;
        }
        return clusterId2NewSlot[getClusterId(duIndex)];
    }

    // TODO: comment and improve naming (the meaning is linear? I think...)
    public boolean canFastProcess(int duIndex) {
        return getClusterId(duIndex) == UNDEFINED;
    }

    public int[] getNewSlot2Origin() {
        return newSlot2Origin;
    }

    public int getReachDef(int duIndex) {
        return du2ReachDef[duIndex];
    }

    public void visitLivePhis(BytecodeBlock block, Consumer<SemiPhi> consumer) {
        int index = cfg.getIndex(block);
        if (!block2phis.contains(index)) {
            return;
        }
        for (SemiPhi phi : block2phis.get(index)) {
            if (phi.isUsed()) {
                consumer.accept(phi);
            }
        }
    }

    public int getMaxDUIndexWithPhi() {
        return duInfo.getMaxDUIndex() + allPhis.size();
    }

    public boolean isDefUsed(int def) {
        // we must ensure if def1 and def2 belongs to the same phi-web (or cluster),
        // they should have the same used flag
        // this will be ensured by the algorithm,
        // in stage 2, only the def has been used will be marked as used will be visited
        if (isPhiDef(def)) {
            return getPhiByIndex(def).isUsed();
        } else {
            return defUsed[def];
        }
    }
}
