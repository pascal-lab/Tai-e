package pascal.taie.frontend.newfrontend;

import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

public class VarWebSplitter {

    private final AsmIRBuilder builder;

    private final VarManager varManager;

    private final int[][] block2inDefs;

    private final int[][] block2outDefs;

    private final Map
            <
                    BytecodeBlock,
                    List<List<Integer>> // slot -> [Defs]
                    > mayFlowToCatchOfBlocks; // contains all the defs. Used in exception handling.

    private final List<Pair<List<BytecodeBlock>, BytecodeBlock>> tryAndHandlerBlocks;

    private final Var[] locals;

    private final Colors colors;

    private final BytecodeBlock entry;

    public VarWebSplitter(AsmIRBuilder builder) {
        this.builder = builder;
        this.varManager = builder.manager;
        int blockSize = builder.blockSortedList.size();
        this.block2inDefs = new int[blockSize][];
        this.block2outDefs = new int[blockSize][];
        this.mayFlowToCatchOfBlocks = Maps.newHybridMap();
        this.tryAndHandlerBlocks = builder.getTryAndHandlerBlocks();
        this.locals = varManager.getLocals();
        this.colors = new Colors(locals.length,
                locals.length * builder.blockSortedList.size());
        this.entry = builder.getEntryBlock();
        // first, remove all local variable from ret set
        // we'll add them back after splitting
        Set<Var> ret = varManager.getRetVars();
        for (Var v : locals) {
            ret.remove(v);
        }
    }

    public void build() {
        constructWeb();
        update();
    }

    public void constructWeb() {
        // construct inDefs by dfs
//        constructWebInsideBlock(entry);
//        for (Pair<List<BytecodeBlock>, BytecodeBlock> tryCatchPair : tryAndHandlerBlocks) {
//            BytecodeBlock handler = tryCatchPair.second();
//            if (!block2inDefs.containsKey(handler)) {
//                constructWebInsideBlock(handler);
//            }
//        }

        for (int i = 0; i < builder.blockSortedList.size(); ++i) {
            BytecodeBlock bb = builder.blockSortedList.get(i);
            bb.setIndex(i);
            constructWebInsideBlock(bb);
        }

        // merge inDefs of catch block
        for (Pair<List<BytecodeBlock>, BytecodeBlock> tryCatchPair : tryAndHandlerBlocks) {
            List<BytecodeBlock> trys = tryCatchPair.first();
            BytecodeBlock handler = tryCatchPair.second();
            for (var t : trys) {
                constructWebBetweenTryAndHandler(t, handler);
            }
        }
    }

    private void mergeTwoDefs(int[] outDef, int[] inDef) {
        assert outDef.length == inDef.length;
        for (int i = 0; i < inDef.length; ++i) {
            if (inDef[i] != Colors.NOT_EXIST && outDef[i] != Colors.NOT_EXIST) {
                colors.mergeTwoColor(i, outDef[i], inDef[i]);
            }
        }
    }

    private boolean isVarNotExistsInFrame(BytecodeBlock block, int slot) {
        if (builder.isFrameUsable()) {
            return !block.isLocalExistInFrame(slot);
        }
        assert false;
        return false;
    }

    private boolean canInferLiveVar(BytecodeBlock block) {
        return block.getFrame() != null;
    }

    private int getParamThisSize() {
        if (varManager.getParams().isEmpty()) {
            return varManager.getThisVar() == null ? 0 : 1;
        } else {
            int preParamSize = varManager.getParams().size();
            return varManager.getParams()
                    .get(preParamSize - 1)
                    .getIndex() + 1;
        }
    }

    private int[] getPhantomInDefs(BytecodeBlock entry) {
        int[] res = getEmptyColors();
        boolean isEntry = entry == this.entry;

        List<Var> allPhantoms = isEntry ? varManager.getParamThis() : List.of(locals);
        Kind phantomType = isEntry ? Kind.PARAM : Kind.PHANTOM;

        for (Var v : allPhantoms) {
            int slot = VarManager.getSlotFast(v);
            if (phantomType == Kind.PHANTOM && canInferLiveVar(entry) &&
                    isVarNotExistsInFrame(entry, slot)) {
                continue;
            }
            if (phantomType == Kind.PARAM && canInferLiveVar(entry) &&
                    isVarNotExistsInFrame(entry, slot)) {
                continue;
            }
            int color = colors.getNewColor(slot);
            StmtOccur occur = new StmtOccur(entry, -1, phantomType, slot, color);
            colors.noticeOneOccur(occur);
            res[slot] = color;
        }

        return res;
    }

    private int[] getEmptyColors() {
        int[] res = new int[locals.length];
        Arrays.fill(res, Colors.NOT_EXIST);
        return res;
    }

    private int[] getInDefs(BytecodeBlock bb) {
        if (bb.getIndex() == -1) {
            return null;
        } else {
            return block2inDefs[bb.getIndex()];
        }
    }

    private int[] getOutDefs(BytecodeBlock bb) {
        if (bb.getIndex() == -1) {
            return null;
        } else {
            return block2outDefs[bb.getIndex()];
        }
    }

    private void constructWebInsideBlock(BytecodeBlock block) {
        boolean isInTry = block.isInTry();
        int[] currentDefs;
//        int size = block.inEdges().size();
//        if (size > 1 || block == builder.getEntryBlock()) {
//            if (block2inDefs.containsKey(block)) {
//                int[] otherInDefs = block2inDefs.get(block);
//                mergeDefs(otherInDefs, inDefs);
//                return;
//            } else {
//                int[] realInDef;
//                if (block == builder.getEntryBlock()) {
//                    realInDef = ;
//                } else {
//                    realInDef = washDefs(block, inDefs);
//                }
//                block2inDefs.put(block, realInDef);
//                currentDefs = realInDef.clone();
//            }
//        } else {
//            assert block2inDefs.get(block) == null;
//            block2inDefs.put(block, inDefs);
//            currentDefs = inDefs.clone();
//        }
//        if (block != this.entry &&
//                block.inEdges().stream().allMatch(block2outDefs::containsKey)) {
//            List<int[]> outDefs = new ArrayList<>();
//            for (BytecodeBlock bb : block.inEdges()) {
//                int[] outDef = block2outDefs.get(bb);
//                outDefs.add(outDef);
//            }
//            currentDefs = mergeDefs(block, outDefs);
//        } else {
//            currentDefs = getPhantomInDefs(block);
//            for (BytecodeBlock bb : block.inEdges()) {
//                if (block2outDefs.containsKey(bb)) {
//                    int[] outDef = block2outDefs.get(bb);
//                    mergeTwoDefs(outDef, currentDefs);
//                }
//            }
//        }
        if (block.inEdges().isEmpty() || block == entry) {
            currentDefs = getPhantomInDefs(block);
        } else if (block.inEdges().size() == 1 &&
                block.inEdges().get(0).getIndex() != -1) {
            int[] in = getOutDefs(block.inEdges().get(0));
            assert in != null;
            currentDefs = in.clone();
        } else {
            currentDefs = new int[locals.length];
            for (int j = 0; j < locals.length; ++j) {
                currentDefs[j] = colors.getNewColor(j);
            }
            for (int i = 0; i < block.inEdges().size(); ++i) {
                int[] out = getOutDefs(block.inEdges().get(i));
                if (out != null) {
                    for (int j = 0; j < locals.length; ++j) {
                        if (out[j] == Colors.NOT_EXIST) {
                            currentDefs[j] = Colors.NOT_EXIST;
                        }
                        if (currentDefs[j] != Colors.NOT_EXIST) {
                            colors.mergeTwoColor(j, out[j], currentDefs[j]);
                        }
                    }
                }
            }
        }
        block2inDefs[block.getIndex()] = currentDefs.clone();
        List<List<Integer>> mayFlowToCatch;
        if (isInTry) {
            mayFlowToCatch = mayFlowToCatchOfBlocks.computeIfAbsent(block,
                    (b) -> {
                        List<List<Integer>> res = new ArrayList<>();
                        for (int i = 0; i < locals.length; ++i) {
                            List<Integer> slotDefs = new ArrayList<>();
                            if (currentDefs[i] != Colors.NOT_EXIST) {
                                slotDefs.add(currentDefs[i]);
                            }
                            res.add(slotDefs);
                        }
                        return res;
                    });
        } else {
            mayFlowToCatch = null;
        }

        List<Stmt> stmts = getStmts(block);
        for (int i = 0; i < stmts.size(); ++i) {
            Stmt stmt = stmts.get(i);
            // uses first
            int finalI = i;
            StmtVarVisitor.visitUse(stmt, (use) -> {
                if (varManager.isLocalFast(use)) {
                    int slot = VarManager.getSlotFast(use);
                    int color = currentDefs[slot];
                    assert color != Colors.NOT_EXIST;
                    StmtOccur occur = new StmtOccur(block, finalI, Kind.USE, slot, color);
                    colors.noticeOneOccur(occur);
                }
            });

            StmtVarVisitor.visitDef(stmt, (def) -> {
                if (varManager.isLocalFast(def)) {
                    int slot = VarManager.getSlotFast(def);
                    int newColor = colors.getNewColor(slot);
                    StmtOccur occur = new StmtOccur(block, finalI, Kind.DEF, slot, newColor);
                    colors.noticeOneOccur(occur);
                    currentDefs[slot] = newColor;

                    if (isInTry) {
                        mayFlowToCatch.get(slot).add(newColor);
                    }
                }
            });
        }


        for (BytecodeBlock bytecodeBlock : block.outEdges()) {
            int[] inDefs = getInDefs(bytecodeBlock);
            assert !(bytecodeBlock.getIndex() != -1 && inDefs == null);
            if (inDefs != null) {
                mergeTwoDefs(currentDefs, inDefs);
            }
        }

        block2outDefs[block.getIndex()] = currentDefs;
    }

    private void constructWebBetweenTryAndHandler(BytecodeBlock tryBlock, BytecodeBlock handler) {
        List<List<Integer>> blockAllDefs = mayFlowToCatchOfBlocks.get(tryBlock);
        assert blockAllDefs != null;
        int[] handlerInDefs = getInDefs(handler);
        assert handlerInDefs != null;
        for (int i = 0; i < locals.length; ++i) {
            int inColor = handlerInDefs[i];
            if (inColor != Colors.NOT_EXIST) {
                for (int outColor : blockAllDefs.get(i)) {
                    colors.mergeTwoColor(i, outColor, inColor);
                }
            }
        }
    }

    // TODO: now var name is too random, need to fix it
    private Var[] splitLocals() {
        int allColorSize = colors.getAllColorCount();
        Var[] res = new Var[allColorSize];

        var all = colors.build();
        for (int i = 0; i < locals.length; ++i) {
//            List<Integer> allColors = colors.getAllColors(i);
//            Map<Integer, Integer> visited = Maps.newMap();
//            for (int color : allColors) {
//                int rootColor = colors.getRootColor(slot, color);
//                int currentCount = colors.getColorCount(color);
//                visited.compute(rootColor, (k, v) ->
//                        (v == null) ? currentCount : currentCount + v);
//            }
//
//            AtomicInteger index = new AtomicInteger();
//            visited.entrySet()
//                    .stream()
//                    .filter(e -> e.getValue() != 0)
//                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
//                    .forEach((e) -> {
//                        int color = e.getKey();
//                        Var v = varManager.splitLocal(slot, index.incrementAndGet());
//                        res[color] = v;
//                    });

//            for (int color : allColors) {
//                res[color] = res[colors.getRootColor(slot, color)];
//            }
            var g = all.get(i);
            int maxPos = 0;
            int maxVal = -1;
            for (int pos = 0; pos < g.size(); ++pos) {
                int val = g.get(pos).get(0);
                if (val > maxVal) {
                    maxVal = val;
                    maxPos = pos;
                }
            }
            int index = 1;
            for (var s : g) {
                int idx = index == maxPos + 1 ? 1 : index + 1;
                if (s.size() > 1) {
                    Var v = varManager.splitLocal(i, idx);
                    for (int j = 1; j < s.size(); ++j) {
                        res[s.get(j)] = v;
                    }
                }
                index++;
            }
        }

        // note: there may be null in `res`
        // but if `res[i]` is null, that means color `i` does not occur in code
        return res;
    }

    private void update() {
        Var[] color2Local = splitLocals();
        Map<Var, Var> defMap = Maps.newMap();
        Map<Var, Var> useMap = Maps.newMap();
        Lenses lenses = new Lenses(builder.method, useMap, defMap);
        for (StmtOccur source : colors.getOccurs()) {
            useMap.clear();
            defMap.clear();
            Kind kind = source.second();

            if (kind == Kind.PHANTOM) {
                continue;
            }

            BytecodeBlock block = source.block();
            int idx = source.index();
            Stmt oldStmt = idx == -1 ? null : block.getStmts().get(idx);

            int slot = source.slot();
            int color = source.color();

            Var old = locals[slot];
            Var target = color2Local[color];
            assert old != null && target != null;

            // try to correct var name if possible
            if (varManager.existsLocalVariableTable) {
                if (idx != -1 && target.getName().startsWith(VarManager.LOCAL_PREFIX)) {
                    Optional<String> maybeName = varManager.getName(slot, block.getOrig(idx));
                    maybeName.ifPresent(s -> varManager.fixName(target, s));
                }
            }

            if (old == target) {
                handleSideEffects(oldStmt);
                continue;
            }

            // this stmt occur should be subst with new var
            // 1. build sigma
            if (kind == Kind.DEF) {
                defMap.put(old, target);
            } else if (kind == Kind.USE) {
                useMap.put(old, target);
            } else if (kind == Kind.PARAM) {
                varManager.replaceParam(old, target);
                // don't set stmt
                continue;
            }
            // 2. exec subst
            assert oldStmt != null;
            Stmt newStmt = lenses.subSt(oldStmt);
            handleSideEffects(newStmt);
            block.getStmts().set(idx, newStmt);
        }
    }

    private void handleSideEffects(Stmt newStmt) {
        if (newStmt instanceof Return r) {
            varManager.getRetVars().add(r.getValue());
        }
    }

//    private SetFact<Var> getInFact(BytecodeBlock block) {
//        assert liveVariables != null;
//        if (block.getStmts().isEmpty()) {
//            assert block.outEdges().size() == 1;
//            assert block.fallThrough() != null;
//            return getInFact(block.fallThrough());
//        } else {
//            return liveVariables.getInFact(getStmts(block).get(0));
//        }
//    }

    private List<Stmt> getStmts(BytecodeBlock block) {
        return block.getStmts();
    }

    private enum Kind {
        DEF,
        USE,
        PHANTOM, // PHANTOM for the fake Copy in each block: x = x, used as a connector
        PARAM
    }

    private record StmtOccur(BytecodeBlock block, int index, Kind second, int slot, int color) {
    }

    private static class Colors {

        private final List<StmtOccur> occurs;

        private final List<MergeGraphNode> g;

        private int counter;

        private final int maxLocal;

        static final int NOT_EXIST = -1;

        Colors(int maxLocal, int stmtSize) {
            occurs = new ArrayList<>(stmtSize);
            counter = 0;
            this.maxLocal = maxLocal;
            g = new ArrayList<>();
        }

        void noticeOneOccur(StmtOccur occur) {
            occurs.add(occur);
            int color = occur.color();
            g.get(color).incr();
            if (occur.second != Kind.PHANTOM) {
                g.get(color).u();
            }
        }

        int getNewColor(int slot) {
            int newColor = counter++;
            g.add(new MergeGraphNode(newColor, slot));
            return newColor;
        }

        void mergeTwoColor(int slot, int color1, int color2) {
            assert g.get(color1).slot == g.get(color2).slot;
            if (color1 == color2) {
                return;
            }
            g.get(color1).outEdges.add(g.get(color2));
            g.get(color2).inEdges.add(g.get(color1));
        }

        int getAllColorCount() {
            return counter;
        }

        /**
         * @return a list, in the form:
         * <p>[slot] -> [web1, web2, ...]</p>
         * <p>[web]  -> [ count, color1, color2 ]</p>
         */
        List<List<List<Integer>>> build() {
            boolean[] visited = new boolean[getAllColorCount()];
            for (int i = 0; i < getAllColorCount(); ++i) {
                if (visited[i] || !g.get(i).used) {
                    continue;
                }
                Queue<MergeGraphNode> queue = new LinkedList<>();
                queue.add(g.get(i));
                while (!queue.isEmpty()) {
                    MergeGraphNode node = queue.poll();
                    int color = node.color;
                    if (!visited[color]) {
                        visited[color] = true;
                        boolean before = g.get(color).used;
                        g.get(color).u();
                        if (!before) {
                            queue.addAll(node.inEdges);
                        } else {
                            for (MergeGraphNode n : node.inEdges) {
                                if (!n.used) {
                                    queue.add(n);
                                }
                            }
                        }
                    }
                }
            }

            List<List<List<Integer>>> connected = new ArrayList<>();
            for (int i = 0; i < maxLocal; ++i) {
                connected.add(new ArrayList<>());
            }
            Arrays.fill(visited, false);
            for (int i = 0; i < getAllColorCount(); ++i) {
                MergeGraphNode node = g.get(i);
                if (visited[i] || !node.used) {
                    continue;
                }
                List<Integer> current = new ArrayList<>();
                Queue<MergeGraphNode> queue1 = new LinkedList<>();
                queue1.add(node);
                current.add(node.count);
                int slot = node.slot;
                while (!queue1.isEmpty()) {
                    MergeGraphNode now = queue1.poll();
                    assert now.slot == slot;
                    int c = now.color;
                    if (!visited[c]) {
                        visited[c] = true;
                        int currentCount = current.get(0);
                        int nodeCount = now.count;
                        current.set(0, currentCount + nodeCount);
                        current.add(c);
                        for (MergeGraphNode node1 : now.outEdges) {
                            if (node1.used) {
                                queue1.add(node1);
                            }
                        }
                        for (MergeGraphNode node1 : now.inEdges) {
                            if (node1.used) {
                                queue1.add(node1);
                            }
                        }
                    }
                }
                if (current.get(0) != 0) {
                    connected.get(slot).add(current);
                }
            }
            return connected;
        }

        List<StmtOccur> getOccurs() {
            return occurs;
        }
    }

    private static class MergeGraphNode {
        int color;

        int count;

        boolean used;

        int slot;

        List<MergeGraphNode> inEdges;

        List<MergeGraphNode> outEdges;

        MergeGraphNode(int color, int slot) {
            this.color = color;
            this.slot = slot;
            inEdges = new ArrayList<>();
            outEdges = new ArrayList<>();
            count = 0;
            used = false;
        }

        void incr() {
            count++;
        }

        void u() {
            used = true;
        }
    }
}
