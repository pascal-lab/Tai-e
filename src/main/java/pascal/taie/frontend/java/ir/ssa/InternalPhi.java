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
    private final SSATransform SSATransform;

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

    InternalPhi(SSATransform SSATransform, int slot, BytecodeBlock block, int phiIndex) {
        this.SSATransform = SSATransform;
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
        if (SSATransform.isPhiDef(defIndex)) {
            InternalPhi p = SSATransform.getPhiByIndex(defIndex);
            p.outPhis.add(this);
        } else {
            if (SSATransform.def2OutPhis[defIndex] == null) {
                SSATransform.def2OutPhis[defIndex] = new IntList(4);
            }
            SSATransform.def2OutPhis[defIndex].add(SSATransform.getPhiDUIndex(phiIndex));
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
        return SSATransform.getPhiDUIndex(phiIndex);
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
