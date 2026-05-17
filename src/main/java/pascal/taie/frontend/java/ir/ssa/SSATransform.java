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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;

import pascal.taie.frontend.java.ir.BytecodeBlock;
import pascal.taie.frontend.java.ir.DUInfo;
import pascal.taie.util.collection.IntList;
import pascal.taie.util.collection.LazyArray;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.graph.Dominators;
import pascal.taie.util.graph.IndexedGraph;

/**
 * Constructs SSA (reach def info) for bytecode to resolve local variable reuse.
 * It will extend the DUIndex space to include Phi nodes at {@code [maxDUIndex, maxDUIndex + phiIndex)}.
 */
public class SSATransform {

    // ---------------------- Input data -------------------------

    /**
     * The control flow graph
     */
    private final IndexedGraph<BytecodeBlock> cfg;

    /**
     * Dominator information for the CFG
     */
    private final Dominators<BytecodeBlock> dom;

    /**
     * Records all def/use operations about slots in bytecode, use DUIndex to index them.
     */
    private final DUInfo duInfo;

    /**
     * Number of slots in the method
     */
    private final int slotSize;

    /**
     * Whether to build SSA form method (so that each variable has only one definition)
     */
    private final boolean isSSA;

    /**
     * Undefined id (duIndex, clusterId, slot, etc.)
     */
    public static final int UNDEFINED = -1;

    // ---------------------- Core SSA data -------------------------

    /**
     * block2phis[i] = list of phis in block i
     */
    private final LazyArray<List<InternalPhi>> block2phis;

    /**
     * All the phis in the method
     */
    private final List<InternalPhi> allPhis;

    /**
     * du2ReachDef[i] = j, if duIndex j is the reaching definition of duIndex i
     * <ul>
     *   <li> If duIndex i is a use, then j is the def that i will use. </li>
     *   <li> If duIndex i is a def, then j is the previous def that i shadows. </li>
     * </ul>
     */
    private int[] du2ReachDef;

    /**
     * defUsed[i] = true, if the def i is used.
     * Only for non-phi defs, the usage about phis are recorded in its `used` flag.
     */
    private final boolean[] defUsed;

    // ---------------------- Used only when building SSA IR -------------------------

    /**
     * Maps a definition DUIndex to its Phi-Web (Cluster) ID for variable splitting.
     * Only for non-phi defs.
     */
    private final int[] def2ClusterId;

    /**
     * Maps a parameter duIndex to its Phi-Web ID.
     */
    private int[] param2ClusterId;

    /**
     * The size of new slots after re-slotting.
     */
    private int newSlotSize;

    /**
     * Maps a cluster ID to the final slot index allocated to it.
     */
    private int[] clusterId2NewSlot;

    /**
     * Maps a new slot index back to the original bytecode slot index.
     */
    private int[] newSlot2Origin;

    /**
     * def2Phis[i] = list of phi DUIndexes that use the def i.
     * Only for non-phi defs.
     */
    final IntList[] def2OutPhis;

    public SSATransform(IndexedGraph<BytecodeBlock> cfg,
                        int slotSize,
                        DUInfo duInfo,
                        boolean isSSA,
                        Dominators<BytecodeBlock> dom) {
        this.cfg = cfg;
        this.slotSize = slotSize;
        this.dom = dom;
        this.duInfo = duInfo;
        this.block2phis = new LazyArray<>(cfg.nodeCount()) {
            @Override
            protected List<InternalPhi> createElement() {
                return new ArrayList<>();
            }
        };
        this.allPhis = new ArrayList<>();
        this.def2ClusterId = new int[duInfo.getMaxDUIndex()];
        this.isSSA = isSSA;
        this.defUsed = new boolean[duInfo.getMaxDUIndex()];
        Arrays.fill(def2ClusterId, UNDEFINED);
        this.def2OutPhis = new IntList[duInfo.getMaxDUIndex()];
    }

    public void build() {
        insertPhis();
        buildReachDefs();
        spreadPhiUsed();
        if (!isSSA) {
            reSlot();
        }
    }

    public int getNewSlotSize() {
        return newSlotSize;
    }

    public int getNewSlot(int duIndex) {
        if (duIndex < duInfo.getParamSize()) {
            return duIndex;
        }
        return clusterId2NewSlot[getClusterId(duIndex)];
    }

    /**
     * Checks if the definition is isolated (i.e., not part of any Phi-Web/Cluster).
     */
    public boolean isIsolatedDef(int duIndex) {
        return getClusterId(duIndex) == UNDEFINED;
    }

    public int[] getNewSlot2Origin() {
        return newSlot2Origin;
    }

    public int getReachDef(int duIndex) {
        return du2ReachDef[duIndex];
    }

    public void visitUsedInternalPhis(BytecodeBlock block, Consumer<InternalPhi> consumer) {
        int index = cfg.getIndex(block);
        if (!block2phis.contains(index)) {
            return;
        }
        for (InternalPhi phi : block2phis.get(index)) {
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

    // -------- Internal build process --------

    private void insertPhis() {
        for (int slot = 0; slot < slotSize; ++slot) {
            // the indexes of blocks (if they have phi stmts) that define the slot (phi is also a definition)
            Queue<Integer> worklist = new ArrayDeque<>();
            List<BytecodeBlock> defBlocks = duInfo.getDefBlocks(slot);
            for (BytecodeBlock block : defBlocks) {
                worklist.add(cfg.getIndex(block));
            }
            while (!worklist.isEmpty()) {
                BytecodeBlock block = cfg.getObject(worklist.poll());
                for (int dfBlock : dom.getDomFrontier().get(cfg.getIndex(block))) {
                    if (!isPhiInserted(dfBlock, slot)) {
                        InternalPhi phi = new InternalPhi(this, slot, cfg.getObject(dfBlock), allPhis.size());
                        block2phis.get(dfBlock).add(phi);
                        allPhis.add(phi);
                        worklist.add(dfBlock);
                    }
                }
            }
        }
    }

    /**
     * Builds reaching definitions for all def/use operations and phis.
     * Also resolves the inputs of phis.
     */
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
            for (InternalPhi phi : block2phis.get(cfg.getIndex(cfg.getEntry()))) {
                if (phi.getSlot() == i) {
                    phi.addInDefs(null, i);
                }
            }
        }

        for (BytecodeBlock block : dom.getDomTreePreOrder()) {
            // 1. make reach defs for phis in this block (phi is a def)
            int blockIndex = cfg.getIndex(block);
            if (block2phis.contains(blockIndex)) {
                for (InternalPhi phi : block2phis.get(blockIndex)) {
                    int phiDUIndex = phi.getPhiDUIndex();
                    du2ReachDef[phiDUIndex] = getSlotLatestDef(phi.getSlot(), slot2LatestDef, phiDUIndex);
                    slot2LatestDef[phi.getSlot()] = phiDUIndex;
                }
            }

            // 2. make reach defs for normal def/use operations in this block
            duInfo.visit(block, (duIndex, type, slot) -> {
                int reachDef = getSlotLatestDef(slot, slot2LatestDef, duIndex);
                if (type == DUInfo.DUType.USE) {
                    assert reachDef != UNDEFINED;
                    du2ReachDef[duIndex] = reachDef;
                    if (isPhiDef(reachDef)) {
                        InternalPhi phi = getPhiByIndex(reachDef);
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
                for (InternalPhi phi : block2phis.get(succIndex)) {
                    int reachDef = getSlotLatestDef(phi.getSlot(), slot2LatestDef, block);
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
     * Spreads usage flag recursively.
     */
    private void spreadPhiUsed() {
        for (int i = 0; i < cfg.nodeCount(); i++) {
            if (!block2phis.contains(i)) {
                continue;
            }
            for (InternalPhi phi : block2phis.get(i)) {
                if (phi.isUsed()) {
                    spreadPhiUsed(phi);
                }
            }
        }
    }

    /**
     * Allocates new slots for disconnected clusters (phi-webs).
     */
    private void reSlot() {
        // 1. compute clusters
        newSlotSize = slotSize;
        param2ClusterId = new int[duInfo.getParamSize()];
        Arrays.fill(param2ClusterId, UNDEFINED);
        int clusterIdCounter = 0;
        boolean[] visited = new boolean[allPhis.size()];
        Map<Integer, Integer> clusterId2OriginSlot = Maps.newMap();
        for (int i = 0; i < cfg.nodeCount(); i++) {
            if (!block2phis.contains(i)) {
                continue;
            }
            for (InternalPhi phi : block2phis.get(i)) {
                if (phi.isUsed() && !visited[phi.getPhiIndex()]) {
                    clusterId2OriginSlot.put(clusterIdCounter, phi.getSlot());
                    findCluster(phi, visited, clusterIdCounter);
                    clusterIdCounter++;
                }
            }
        }

        // 2. allocate slots for clusters, try to reuse original slots first
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

    private void spreadPhiUsed(InternalPhi phi) {
        for (int i = 0; i < phi.getInDefs().size(); ++i) {
            int def = phi.getInDefs().get(i);
            if (isPhiDef(def)) {
                InternalPhi p = getPhiByIndex(def);
                if (!p.isUsed()) {
                    p.setUsed();
                    spreadPhiUsed(p);
                }
            } else {
                defUsed[def] = true;
            }
        }
    }

    private int getClusterId(int duIndex) {
        if (duIndex >= def2ClusterId.length) {
            // this is a phi node
            return getPhiByIndex(duIndex).getClusterId();
        }
        return def2ClusterId[duIndex];
    }

    private boolean isPhiInserted(int block, int slot) {
        if (!block2phis.contains(block)) {
            return false;
        }
        for (InternalPhi phi : block2phis.get(block)) {
            if (phi.getSlot() == slot) {
                return true;
            }
        }
        return false;
    }

    private BytecodeBlock getBlock(int duIndex) {
        if (!isPhiDef(duIndex)) {
            return duInfo.getBlock(duIndex);
        } else {
            return getPhiByIndex(duIndex).getBlock();
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
        while (!dom.dominates(defBlock, block)) {
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

    /**
     * Performs bidirectional DFS to identify connected clusters (phi-webs).
     */
    void findCluster(InternalPhi phi, boolean[] visited, int clusterId) {
        if (!phi.isUsed()) {
            return;
        }
        if (visited[phi.getPhiIndex()]) {
            return;
        }
        visited[phi.getPhiIndex()] = true;
        phi.setClusterId(clusterId);

        for (InternalPhi p : phi.getOutPhis()) {
            findCluster(p, visited, clusterId);
        }

        for (int i = 0; i < phi.getInDefs().size(); i++) {
            int def = phi.getInDefs().get(i);
            assert def != -1;
            if (isPhiDef(def)) {
                InternalPhi p = getPhiByIndex(def);
                findCluster(p, visited, clusterId);
            } else {
                def2ClusterId[def] = clusterId;
                if (def < duInfo.getParamSize()) {
                    param2ClusterId[def] = clusterId;
                }
                if (def2OutPhis[def] != null) {
                    IntList phis = def2OutPhis[def];
                    for (int j = 0; j < phis.size(); ++j) {
                        InternalPhi p = getPhiByIndex(phis.get(j));
                        if (!visited[p.getPhiIndex()]) {
                            findCluster(p, visited, clusterId);
                        }
                    }
                }
            }
        }
    }

    boolean isPhiDef(int duIndex) {
        return duIndex >= duInfo.getMaxDUIndex();
    }

    int getPhiDUIndex(int phiIndex) {
        return phiIndex + duInfo.getMaxDUIndex();
    }

    InternalPhi getPhiByIndex(int duIndex) {
        int phiIndex = duIndex - duInfo.getMaxDUIndex();
        return allPhis.get(phiIndex);
    }
}
