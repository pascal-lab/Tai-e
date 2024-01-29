package pascal.taie.frontend.newfrontend.ssa;

import pascal.taie.frontend.newfrontend.GenericDUInfo;
import pascal.taie.frontend.newfrontend.IBasicBlock;
import pascal.taie.frontend.newfrontend.SparseSet;
import pascal.taie.util.collection.Maps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public class FastVarSplitting<Block extends IBasicBlock> {

    public class SemiPhi {
        final int var;

        final List<Integer> inDefs;

        final List<Block> inBlocks = new ArrayList<>();

        final List<SemiPhi> owned = new ArrayList<>();

        final Block belongsTo;

        final int index;

        boolean used = false;

        boolean valid = true;

        int newName;

        Object realPhi;

        SemiPhi(int var, List<Integer> inDefs, Block belongsTo, int index) {
            this.var = var;
            this.inDefs = inDefs;
            this.belongsTo = belongsTo;
            this.index = index;
        }

        void addInDefs(Block b, int reachDef) {
            inDefs.add(reachDef);
            inBlocks.add(b);
            if (isSSADef(reachDef)) {
                SemiPhi p = getPhiByIndex(reachDef);
                p.owned.add(this);
            } else {
                if (defOwned[reachDef] == null) {
                    defOwned[reachDef] = new ArrayList<>();
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

        public List<Integer> getInDefs() {
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

    private final List<List<SemiPhi>> phis;

    private final GenericDUInfo<Block> info;

    private int[] duReachDef;

    // duReachDef[i] = j, j def dom i

    private final static int UNDEFINED = -1;

    private final int varSize;

    private final int[] renames;

    private int newMaxLocal;

    private int[] varMappingTable;

    private final List<Integer>[] defOwned;

    private final List<SemiPhi> allPhis;

    private final boolean useSSA;

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
        this.phis = new ArrayList<>(graph.size());
        this.renames = new int[info.getMaxDuIndex()];
        this.allPhis = new ArrayList<>();
        this.useSSA = useSSA;
        Arrays.fill(renames, UNDEFINED);

        for (int i = 0; i < graph.size(); i++) {
            Block b = graph.getNode(i);
            assert b.getIndex() == graph.getIndex(b);
        }

        defOwned = new List[info.getMaxDuIndex()];
    }

    public void build() {
        int phiCount = phiInsertion();
        travLink(phiCount);
        pruneAndRenaming(phiCount);
    }

    private void spreadingUsed(SemiPhi phi) {
        for (int def : phi.inDefs) {
            if (isSSADef(def)) {
                SemiPhi p = getPhiByIndex(def);
                if (!p.used) {
                    p.setUsed();
                    spreadingUsed(p);
                }
            }
        }
    }

    void pruneAndRenaming(int phiCount) {
        // pass 1: prune, or spreading `used` flag
        for (int i = 0; i < graph.size(); i++) {
            for (SemiPhi phi : phis.get(i)) {
                if (phi.used) {
                    spreadingUsed(phi);
                }
            }
        }
        // pass 2: generate new names for each cluster and spreading through used phi functions
        // only needed when perform splitting instead of SSA
        if (!useSSA) {
            int newVarIndex = varSize;
            boolean[] oldVarUsed = new boolean[varSize];
            boolean[] visited = new boolean[phiCount];
            Map<Integer, Integer> varMapping = Maps.newMap();
            for (int i = 0; i < graph.size(); i++) {
                for (SemiPhi phi : phis.get(i)) {
                    if (phi.used && !visited[phi.index]) {
                        int newVarName;
                        if (!oldVarUsed[phi.var]) {
                            oldVarUsed[phi.var] = true;
                            newVarName = phi.var;
                        } else {
                            newVarName = newVarIndex++;
                        }
                        varMapping.put(newVarName, phi.var);
                        biDfs(phi, visited, newVarName);
                    }
                }
            }
            newMaxLocal = newVarIndex;
            varMappingTable = new int[newMaxLocal];
            for (int i = 0; i < newMaxLocal; i++) {
                varMappingTable[i] = varMapping.getOrDefault(i, i);
            }
        }
    }

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

        for (int def : phi.inDefs) {
            assert def != -1;
            if (isSSADef(def)) {
                SemiPhi p = getPhiByIndex(def);
                biDfs(p, visited, varIndex);
            } else {
                renames[def] = varIndex;
                if (defOwned[def] != null) {
                    for (int owned : defOwned[def]) {
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
                if (isSSADef(reachDef)) {
                    SemiPhi phi = getPhiByIndex(duReachDef[index]);
                    phi.setUsed();
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
            for (SemiPhi phi : phis.get(graph.getIntEntry())) {
                if (phi.var == i) {
                    phi.addInDefs(null, i);
                }
            }
        }
        for (int node : domTreeDfsSeq) {
            Block current = graph.getNode(node);
            if (phis.get(node) != null) {
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
            return isSSADef(insnIndex1) || insnIndex1 < insnIndex2;
        } else {
            return blockDominates(b1, b2);
        }
    }

    private boolean blockDominates(Block b1, Block b2) {
        return dom.dominates(b1.getIndex(), b2.getIndex());
    }

    private Block getDuBlocks(int udIndex) {
        if (!isSSADef(udIndex)) {
            return info.getBlock(udIndex);
        } else {
            return getPhiByIndex(udIndex).belongsTo;
        }
    }

    private boolean isSSADef(int duIndex) {
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
        for (int i = 0; i < graph.size(); i++) {
            phis.add(new ArrayList<>());
        }

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
                        SemiPhi phi = new SemiPhi(v, new ArrayList<>(3), graph.getNode(node), phiCount++);
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

    public int getRealLocalSlot(int rwIndex) {
        if (rwIndex >= renames.length) {
            // this is a phi node
            return getPhiByIndex(rwIndex).newName;
        }
        return renames[rwIndex];
    }

    public boolean canFastProcess(int rwIndex) {
        return getRealLocalSlot(rwIndex) == UNDEFINED;
    }

    public int[] getVarMappingTable() {
        return varMappingTable;
    }

    public int getReachDef(int rwIndex) {
        return duReachDef[rwIndex];
    }

    public void visitLivePhis(Block b, Consumer<SemiPhi> consumer) {
        for (SemiPhi phi : phis.get(graph.getIndex(b))) {
            if (phi.used) {
                consumer.accept(phi);
            }
        }
    }

    public int getMaxDUCount() {
        return info.getMaxDuIndex() + allPhis.size();
    }

    public int[] getPostOrderSeq() {
        return dom.getPostOrder();
    }
}
