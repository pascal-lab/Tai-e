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

import java.util.ArrayList;
import java.util.List;

import pascal.taie.frontend.java.ir.BytecodeBlock;
import pascal.taie.util.collection.IntList;

/**
 * Represents a Phi in the {@link SSATransform}.
 */
public class InternalPhi {

    /**
     * Constructs SSA (reach def info) for bytecode to resolve local variable reuse.
     */
    private final SSATransform ssaTransform;

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
    private final List<InternalPhi> outPhis = new ArrayList<>();

    /**
     * whether this phi is used
     */
    private boolean used = false;

    /**
     * whether this phi is valid, i.e., all its input defs are not UNDEFINED
     */
    private boolean valid = true;

    /**
     * the cluster (or phi-web) this phi belongs to
     */
    private int clusterId;

    /**
     * the frontend phi statement corresponding to this phi
     */
    private FrontendPhiStmt frontendPhi;

    InternalPhi(SSATransform ssaTransform, int slot, BytecodeBlock block, int phiIndex) {
        this.ssaTransform = ssaTransform;
        this.slot = slot;
        this.block = block;
        this.phiIndex = phiIndex;
    }

    /**
     * Add an input def to this phi.
     */
    void addInDefs(BytecodeBlock defblock, int defIndex) {
        inDefs.add(defIndex);
        inBlocks.add(defblock);
        if (ssaTransform.isPhiDef(defIndex)) {
            InternalPhi p = ssaTransform.getPhiByIndex(defIndex);
            p.outPhis.add(this);
        } else {
            if (ssaTransform.def2OutPhis[defIndex] == null) {
                ssaTransform.def2OutPhis[defIndex] = new IntList(4);
            }
            ssaTransform.def2OutPhis[defIndex].add(ssaTransform.getPhiDUIndex(phiIndex));
        }
    }

    void setInvalid() {
        this.valid = false;
    }

    void setUsed() {
        assert valid;
        used = true;
    }

    boolean isUsed() {
        return used;
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

    public int getPhiDUIndex() {
        return ssaTransform.getPhiDUIndex(phiIndex);
    }

    int getPhiIndex() {
        return phiIndex;
    }

    void setClusterId(int clusterId) {
        this.clusterId = clusterId;
    }

    int getClusterId() {
        return clusterId;
    }

    BytecodeBlock getBlock() {
        return block;
    }

    List<InternalPhi> getOutPhis() {
        return outPhis;
    }

    public void setFrontendPhi(FrontendPhiStmt frontendPhi) {
        this.frontendPhi = frontendPhi;
    }

    public FrontendPhiStmt getFrontendPhi() {
        return frontendPhi;
    }
}
