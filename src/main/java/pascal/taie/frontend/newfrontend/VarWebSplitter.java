package pascal.taie.frontend.newfrontend;

import pascal.taie.analysis.dataflow.fact.DataflowResult;
import pascal.taie.analysis.dataflow.fact.SetFact;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.UnionFindSet;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class VarWebSplitter {

    private final AsmIRBuilder builder;

    private final VarManager varManager;

    private final Map
            <
                    BytecodeBlock,
                    int[]
                    > block2inDefs;

    private final Map
            <
                    BytecodeBlock,
                    List<List<Integer>> // slot -> [Defs]
                    > mayFlowToCatchOfBlocks; // contains all the defs. Used in exception handling.

    private final List<Pair<List<BytecodeBlock>, BytecodeBlock>> tryAndHandlerBlocks;

    @Nullable
    private final DataflowResult<Stmt, SetFact<Var>> liveVariables;

    private final Var[] locals;

    private final Colors colors;

    public VarWebSplitter(AsmIRBuilder builder) {
        this(builder, null);
    }

    public VarWebSplitter(AsmIRBuilder builder, DataflowResult<Stmt, SetFact<Var>> liveVariables) {
        this.builder = builder;
        this.varManager = builder.manager;
        this.block2inDefs = new HashMap<>();
        this.mayFlowToCatchOfBlocks = new HashMap<>();
        this.tryAndHandlerBlocks = builder.getTryAndHandlerBlocks();
        this.liveVariables = liveVariables;
        this.locals = varManager.getLocals();
        this.colors = new Colors(locals.length);

        // first, remove all local variable from ret set
        // we'll add them back after splitting
        List.of(locals).forEach(varManager.getRetVars()::remove);
        assert builder.isFrameUsable() || liveVariables != null;
    }

    public void build() {
        constructWeb();
        update();
    }

    public void constructWeb() {
        BytecodeBlock entry = builder.getEntryBlock();

        // construct inDefs by dfs
        constructWebInsideBlock(entry, getPhantomInDefs(entry));
        for (Pair<List<BytecodeBlock>, BytecodeBlock> tryCatchPair : tryAndHandlerBlocks) {
            BytecodeBlock handler = tryCatchPair.second();
            if (!block2inDefs.containsKey(handler)) {
                constructWebInsideBlock(handler, getPhantomInDefs(handler));
            }
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

    private void mergeDefs(int[] succInDef, int[] predOutDef) {
        for (int i = 0; i < locals.length; ++i) {
            int inColor = succInDef[i];
            if (inColor != Colors.NOT_EXIST) {
                int outColor = predOutDef[i];
                assert outColor != Colors.NOT_EXIST;
                colors.mergeTwoColor(i, inColor, outColor);
            }
        }
    }

    private int[] washDefs(BytecodeBlock block, int[] inDef) {
        int[] res = inDef.clone();
        for (int i = 0; i < locals.length; ++i) {
            if (isVarNotExistsInFrame(block, i)) {
                res[i] = Colors.NOT_EXIST;
            }
        }
        return res;
    }

    private boolean isVarNotExistsInFrame(BytecodeBlock block, int slot) {
        if (builder.isFrameUsable()) {
            return !block.isLocalExistInFrame(slot);
        } else {
            return !getInFact(block).contains(locals[slot]);
        }
    }

    private boolean canInferLiveVar(BytecodeBlock block) {
        return !builder.isFrameUsable() || block.getFrame() != null;
    }

    private int[] getPhantomInDefs(BytecodeBlock entry) {
        int[] res = getEmptyColors();
        boolean isEntry = entry == builder.getEntryBlock();

        List<Var> allPhantoms = isEntry ? varManager.getParamThis() : List.of(locals);
        Kind phantomType = isEntry ? Kind.PARAM : Kind.PHANTOM;

        for (Var v : allPhantoms) {
            int slot = varManager.getSlotFast(v);
            if (phantomType == Kind.PHANTOM &&
                    isVarNotExistsInFrame(entry, slot)) {
                continue;
            }
            if (phantomType == Kind.PARAM && canInferLiveVar(entry) &&
                    isVarNotExistsInFrame(entry, slot)) {
                continue;
            }
            int color = isEntry ? colors.getAllColors(slot).get(0) : colors.getNewColor(slot);
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

    private void constructWebInsideBlock(BytecodeBlock block, int[] inDefs) {
        boolean isInTry = block.isInTry();
        int[] currentDefs;
        int size = block.inEdges().size();
        if (size > 1 || block == builder.getEntryBlock()) {
            if (block2inDefs.containsKey(block)) {
                int[] otherInDefs = block2inDefs.get(block);
                mergeDefs(otherInDefs, inDefs);
                return;
            } else {
                int[] realInDef;
                if (block == builder.getEntryBlock()) {
                    realInDef = inDefs;
                } else {
                    realInDef = washDefs(block, inDefs);
                }
                block2inDefs.put(block, realInDef);
                currentDefs = realInDef.clone();
            }
        } else {
            assert block2inDefs.get(block) == null;
            block2inDefs.put(block, inDefs);
            currentDefs = inDefs.clone();
        }

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
            constructWebInsideBlock(bytecodeBlock, currentDefs);
        }
    }

    private void constructWebBetweenTryAndHandler(BytecodeBlock tryBlock, BytecodeBlock handler) {
        List<List<Integer>> blockAllDefs = mayFlowToCatchOfBlocks.get(tryBlock);
        int[] handlerInDefs = block2inDefs.get(handler);
        for (int i = 0; i < locals.length; ++i) {
            int inColor = handlerInDefs[i];
            if (inColor != Colors.NOT_EXIST) {
                for (int outColor : blockAllDefs.get(i)) {
                    colors.mergeTwoColor(i, inColor, outColor);
                }
            }
        }
    }

    // TODO: further optimize?
    private Var[] splitLocals() {
        int allColorSize = colors.getAllColorCount();
        Var[] res = new Var[allColorSize];

        for (int i = 0; i < locals.length; ++i) {
            int slot = i;
            List<Integer> allColors = colors.getAllColors(i);
            Map<Integer, Integer> visited = Maps.newMap();
            for (int color : allColors) {
                int rootColor = colors.getRootColor(slot, color);
                int currentCount = colors.getColorCount(color);
                visited.compute(rootColor, (k, v) ->
                        (v == null) ? currentCount : currentCount + v);
            }

            AtomicInteger index = new AtomicInteger();
            visited.entrySet()
                    .stream()
                    .filter(e -> e.getValue() != 0)
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .forEach((e) -> {
                        int color = e.getKey();
                        Var v = varManager.splitLocal(slot, index.incrementAndGet());
                        res[color] = v;
                    });

            for (int color : allColors) {
                res[color] = res[colors.getRootColor(slot, color)];
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

    private SetFact<Var> getInFact(BytecodeBlock block) {
        assert liveVariables != null;
        if (block.getStmts().isEmpty()) {
            assert block.outEdges().size() == 1;
            assert block.fallThrough() != null;
            return getInFact(block.fallThrough());
        } else {
            return liveVariables.getInFact(getStmts(block).get(0));
        }
    }

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

        private final List<Integer> colorCount;

        private final List<List<Integer>> allColors;

        private final UnionFindSet<Integer> unionFindSet;

        static final int NOT_EXIST = -1;

        Colors(int maxLocal) {
            occurs = new ArrayList<>();
            colorCount = new ArrayList<>(maxLocal);
            unionFindSet = new UnionFindSet<>(new ArrayList<>());
            allColors = new ArrayList<>();
            for (int i = 0; i < maxLocal; ++i) {
                List<Integer> colorList = new ArrayList<>();
                allColors.add(colorList);
                getNewColor(i);
            }
        }

        void noticeOneOccur(StmtOccur occur) {
            occurs.add(occur);
            int color = occur.color();
            assert color < colorCount.size();
            int oldCount = colorCount.get(color);
            colorCount.set(color, oldCount + 1);
        }

        int getNewColor(int slot) {
            int newColor = colorCount.size();
            colorCount.add(0);
            allColors.get(slot).add(newColor);
            unionFindSet.addElement(newColor);
            return newColor;
        }

        void mergeTwoColor(int slot, int color1, int color2) {
            assert allColors.get(slot).contains(color1) &&
                    allColors.get(slot).contains(color2);
            if (color1 == color2) {
                return;
            }
            unionFindSet.union(color1, color2);
        }

        int getColorCount(int color) {
            return colorCount.get(color);
        }

        List<Integer> getAllColors(int slot) {
            return allColors.get(slot);
        }

        int getAllColorCount() {
            return colorCount.size();
        }

        int getRootColor(int slot, int color) {
            int res = unionFindSet.findRoot(color);
            assert allColors.get(slot).contains(res)
                    && allColors.get(slot).contains(color);
            return res;
        }

        List<StmtOccur> getOccurs() {
            return occurs;
        }
    }
}
