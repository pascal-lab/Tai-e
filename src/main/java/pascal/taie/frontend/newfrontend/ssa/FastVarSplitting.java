package pascal.taie.frontend.newfrontend.ssa;

import pascal.taie.frontend.newfrontend.GenericDUInfo;
import pascal.taie.frontend.newfrontend.IBasicBlock;
import pascal.taie.frontend.newfrontend.SparseSet;
import pascal.taie.frontend.newfrontend.data.IntList;
import pascal.taie.frontend.newfrontend.data.SparseArray;
import pascal.taie.util.collection.Maps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public class FastVarSplitting<Block extends IBasicBlock> {

    public class SemiPhi {
        final int var;

        final IntList inDefs;

        final List<Block> inBlocks = new ArrayList<>();

        final List<SemiPhi> owned = new ArrayList<>();

        final Block belongsTo;

        final int index;

        boolean used = false;

        boolean valid = true;

        int newName;

        Object realPhi;

        SemiPhi(int var, IntList inDefs, Block belongsTo, int index) {
            this.var = var;
            this.inDefs = inDefs;
            this.belongsTo = belongsTo;
            this.index = index;
        }

        void addInDefs(Block b, int reachDef) {
            inDefs.add(reachDef);
            inBlocks.add(b);
            if (isPhiDef(reachDef)) {
                SemiPhi p = getPhiByIndex(reachDef);
                p.owned.add(this);
            } else {
                if (defOwned[reachDef] == null) {
                    defOwned[reachDef] = new IntList(4);
                }
                defOwned[reachDef].add(getPhiDuIndex(index));
            }
        }

        void setUsed() {
            assert valid;
            used = true;
        }

        void setInvalid() {
            this.valid = false;
        }

        public int getVar() {
            return var;
        }

        public IntList getInDefs() {
            return inDefs;
        }

        public List<Block> getInBlocks() {
            return inBlocks;
        }

        public boolean isUsed() {
            return used;
        }

        public int getDUIndex() {
            return getPhiDuIndex(index);
        }

        public void setRealPhi(Object realPhi) {
            this.realPhi = realPhi;
        }

        public Object getRealPhi() {
            return realPhi;
        }
    }

    private final IndexedGraph<Block> graph;

    private final Dominator.DominatorFrontiers df;

    private final Dominator<Block> dom;

    private final SparseArray<List<SemiPhi>> phis;

    private final GenericDUInfo<Block> info;

    private int[] duReachDef;

    // duReachDef[i] = j, j def dom i

    private final static int UNDEFINED = -1;

    private final int varSize;

    private final int[] renames;

    private int newMaxLocal;

    private int[] varMappingTable;

    private final IntList[] defOwned;

    private final List<SemiPhi> allPhis;

    private final boolean useSSA;

    private final boolean[] used;

    public FastVarSplitting(IndexedGraph<Block> graph,
                            int varSize,
                            GenericDUInfo<Block> info,
                            boolean useSSA,
                            Dominator<Block> dom) {
        this.graph = graph;
        this.varSize = varSize;
        this.dom = dom;
        this.df = dom.getDF();
        this.info = info;
        this.phis = new SparseArray<>(graph.size()) {
            @Override
            protected List<SemiPhi> createInstance() {
                return new ArrayList<>();
            }
        };
        this.renames = new int[info.getMaxDuIndex()];
        this.allPhis = new ArrayList<>();
        this.useSSA = useSSA;
        this.used = new boolean[info.getMaxDuIndex()];
        Arrays.fill(renames, UNDEFINED);

        for (int i = 0; i < graph.size(); i++) {
            Block b = graph.getNode(i);
            assert b.getIndex() == graph.getIndex(b);
        }

        defOwned = new IntList[info.getMaxDuIndex()];
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
                used[def] = true;
            }
        }
    }

    void pruneAndRenaming(int phiCount) {
        // pass 1: prune, or spreading `used` flag
        for (int i = 0; i < graph.size(); i++) {
            if (!phis.has(i)) continue;
            for (SemiPhi phi : phis.get(i)) {
                if (phi.used) {
                    spreadingUsed(phi);
                }
            }
        }
        // pass 2: generate new names for each cluster and spreading through used phi functions
        // only needed when perform splitting instead of SSA
        if (!useSSA) {
            paramToName = new int[info.getMaxDuIndex()];
            Arrays.fill(paramToName, UNDEFINED);
            int varIndex = 0;
            boolean[] visited = new boolean[phiCount];
            Map<Integer, Integer> varIndexToOriginSlot = Maps.newMap();
            for (int i = 0; i < graph.size(); i++) {
                if (!phis.has(i)) continue;
                for (SemiPhi phi : phis.get(i)) {
                    if (phi.used && !visited[phi.index]) {
                        varIndexToOriginSlot.put(varIndex, phi.var);
                        biDfs(phi, visited, varIndex);
                        varIndex++;
                    }
                }
            }

            reSlot = new int[varIndex];
            Arrays.fill(reSlot, UNDEFINED);
            boolean[] useOriginSlot = new boolean[varSize];
            for (int i = 0; i < info.getParamSize(); i++) {
                if (paramToName[i] != UNDEFINED) {
                    reSlot[paramToName[i]] = i;
                    useOriginSlot[i] = true;
                }
            }
            newMaxLocal = varSize;
            Map<Integer, Integer> newSlotToOldSlot = Maps.newMap();
            for (int i = 0; i < varIndex; i++) {
                if (reSlot[i] == UNDEFINED) {
                    int oldSlot = varIndexToOriginSlot.get(i);
                    // param slot should not be reused
                    if (!useOriginSlot[oldSlot] && oldSlot >= info.getParamSize()) {
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
                if (def < info.getParamSize()) {
                    paramToName[def] = varIndex;
                }
                if (defOwned[def] != null) {
                    IntList ownedList = defOwned[def];
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

    public void travLink(int phiCount) {
        duReachDef = new int[info.getMaxDuIndex() + phiCount];
        int[] varReachDef = new int[varSize];
        Arrays.fill(duReachDef, UNDEFINED);
        Arrays.fill(varReachDef, UNDEFINED);
        GenericDUInfo.DUVisitor varDUVisitor = (index, type, v) -> {
            if (type == GenericDUInfo.OccurType.USE) {
                int before = varReachDef[v];
                assert before > -100000;
                updateReachingDef(v, index, varReachDef);
                int reachDef = varReachDef[v];
                assert reachDef != UNDEFINED;
                duReachDef[index] = reachDef;
                if (isPhiDef(reachDef)) {
                    SemiPhi phi = getPhiByIndex(duReachDef[index]);
                    phi.setUsed();
                } else {
                    used[reachDef] = true;
                }
            } else {
                updateReachingDef(v, index, varReachDef);
                duReachDef[index] = varReachDef[v];
                varReachDef[v] = index;
            }
        };

        int[] domTreeDfsSeq = dom.getDomTreeDfsSeq();
        // before starting, we need to inject params
        for (int i = 0; i < info.getParamSize(); i++) {
            // params are always defined at the beginning of the method
            // and should be labeled as 0, 1, 2, 3, ... in the du index
            varReachDef[i] = i;
            // if the param is used in a phi node, we need to update the reaching def
            if (!phis.has(graph.getIntEntry())) continue;
            for (SemiPhi phi : phis.get(graph.getIntEntry())) {
                if (phi.var == i) {
                    phi.addInDefs(null, i);
                }
            }
        }
        for (int node : domTreeDfsSeq) {
            Block current = graph.getNode(node);
            if (phis.has(node)) {
                for (SemiPhi phi : phis.get(node)) {
                    int phiIndex = getPhiDuIndex(phi.index);
                    updateReachingDef(phi.var, phiIndex, varReachDef);
                    duReachDef[phiIndex] = varReachDef[phi.var];
                    varReachDef[phi.var] = phiIndex;
                }
            }
            info.visit(current, varDUVisitor);
            for (int i = 0; i < graph.getMergedOutEdgesCount(node); i++) {
                int succIndex = graph.getMergedOutEdge(node, i);
                if (!phis.has(succIndex)) continue;
                for (SemiPhi phi : phis.get(succIndex)) {
                    int varIndex = phi.var;
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

    public void updateReachingDefForBlockEnd(int v, int[] varReachDef, Block block) {
        int r = varReachDef[v];
        if (r == UNDEFINED) {
            return;
        }
        Block b = getDuBlocks(r);
        if (b == block) {
            return;
        } else {
            while (!blockDominates(b, block)) {
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
        Block b1 = getDuBlocks(insnIndex1);
        Block b2 = getDuBlocks(insnIndex2);
        if (b1 == b2) {
            return isPhiDef(insnIndex1) || insnIndex1 < insnIndex2;
        } else {
            return blockDominates(b1, b2);
        }
    }

    private boolean blockDominates(Block b1, Block b2) {
        return dom.dominates(b1.getIndex(), b2.getIndex());
    }

    private Block getDuBlocks(int udIndex) {
        if (!isPhiDef(udIndex)) {
            return info.getBlock(udIndex);
        } else {
            return getPhiByIndex(udIndex).belongsTo;
        }
    }

    private boolean isPhiDef(int duIndex) {
        return duIndex >= info.getMaxDuIndex();
    }

    private int getPhiDuIndex(int phiIndex) {
        return phiIndex + info.getMaxDuIndex();
    }

    private SemiPhi getPhiByIndex(int duIndex) {
        int phiIndex = duIndex - info.getMaxDuIndex();
        return allPhis.get(phiIndex);
    }

    private int phiInsertion() {
        int phiCount = 0;

        SparseSet current = new SparseSet(graph.size(), graph.size());
        for (int v = 0; v < varSize; ++v) {
            List<Block> defBlocks = info.getDefBlock(v);
            for (Block block : defBlocks) {
                current.add(block.getIndex());
            }
            while (!current.isEmpty()) {
                Block block = graph.getNode(current.removeLast());
                for (int node : df.get(block.getIndex())) {
                    if (!isInserted(node, v)) {
                        SemiPhi phi = new SemiPhi(v, new IntList(4), graph.getNode(node), phiCount++);
                        phis.get(node).add(phi);
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
        if (!phis.has(node)) {
            return false;
        }
        for (SemiPhi phi : phis.get(node)) {
            if (phi.var == v) {
                return true;
            }
        }
        return false;
    }

    public int getRealLocalCount() {
        return newMaxLocal;
    }

    public int getRealLocalName(int rwIndex) {
        if (rwIndex >= renames.length) {
            // this is a phi node
            return getPhiByIndex(rwIndex).newName;
        }
        return renames[rwIndex];
    }

    public int getRealLocalSlot(int rwIndex) {
        if (rwIndex < info.getParamSize()) {
            return rwIndex;
        }
        return reSlot[getRealLocalName(rwIndex)];
    }

    public boolean canFastProcess(int rwIndex) {
        return getRealLocalName(rwIndex) == UNDEFINED;
    }

    public int[] getVarMappingTable() {
        return varMappingTable;
    }

    public int getReachDef(int rwIndex) {
        return duReachDef[rwIndex];
    }

    public void visitLivePhis(Block b, Consumer<SemiPhi> consumer) {
        int index = graph.getIndex(b);
        if (!phis.has(index)) {
            return;
        }
        for (SemiPhi phi : phis.get(index)) {
            if (phi.used) {
                consumer.accept(phi);
            }
        }
    }

    public int getMaxDUCount() {
        return info.getMaxDuIndex() + allPhis.size();
    }

    public boolean isDefUsed(int def) {
        // we must ensure if def1 and def2 belongs to the same phi-web,
        // they should have the same used flag
        // this will be ensured by the algorithm,
        // in stage 2, only the def has been used will be marked as used will be visited
        if (!isPhiDef(def)) {
            return used[def];
        } else {
            return getPhiByIndex(def).used;
        }
    }
}
