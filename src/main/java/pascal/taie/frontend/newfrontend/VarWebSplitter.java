package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.tree.AbstractInsnNode;
import pascal.taie.analysis.dataflow.fact.DataflowResult;
import pascal.taie.analysis.dataflow.fact.SetFact;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.UnionFindSet;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class VarWebSplitter {

    private final AsmIRBuilder builder;

    private final VarManager varManager;

    private final List<Var> locals;

    private final SetFact<Var> localSet;

    private final Map
            <
                    Var,
                    UnionFindSet<StmtOccur>
            > webs;

    private final Map
            <
                    BytecodeBlock,
                    Map<
                            Var,
                            StmtOccur
                    >
            > block2inDefs;

    private final Map
            <
                    BytecodeBlock,
                    Map<
                            Var,
                            List<StmtOccur>
                    >
            > mayFlowToCatchOfBlocks; // contains all the defs. Used in exception handling.

    private final List<Pair<List<BytecodeBlock>, BytecodeBlock>> tryAndHandlerBlocks;

    private final Map<SplitIndex, Var> split;

    @Nullable
    private final DataflowResult<Stmt, SetFact<Var>> liveVariables;

    public VarWebSplitter(AsmIRBuilder builder) {
        this(builder, null);
    }

    public VarWebSplitter(AsmIRBuilder builder, DataflowResult<Stmt, SetFact<Var>> liveVariables) {
        this.builder = builder;
        this.varManager = builder.manager;
        this.webs = new HashMap<>();
        this.block2inDefs = new HashMap<>();
        this.mayFlowToCatchOfBlocks = new HashMap<>();
        this.tryAndHandlerBlocks = builder.getTryAndHandlerBlocks();
        this.split = Maps.newMap();
        this.locals = varManager.getVars()
                .stream()
                .filter(varManager::isLocal)
                .toList();
        this.liveVariables = liveVariables;
        this.localSet = new SetFact<>(locals);

        assert builder.isFrameUsable() || liveVariables != null;
    }

    public void build() {
        initWebs();
        constructWeb();
        update();
    }

    private void initWebs() {
        for (Var var : locals) {
            webs.put(var, new UnionFindSet<>(new ArrayList<>()));
        }
    }

    public void constructWeb() {
        BytecodeBlock entry = builder.getEntryBlock();

        // construct inDefs by dfs
        constructWebInsideBlock(entry, getPhantomInDefs(entry));
        for (Pair<List<BytecodeBlock>, BytecodeBlock> tryCatchPair : tryAndHandlerBlocks) {
            BytecodeBlock handler = tryCatchPair.second();
            if (! block2inDefs.containsKey(handler)) {
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

    private void mergeDefs(Map<Var, StmtOccur> succInDef, Map<Var, StmtOccur> predOutDef) {
        for (Var var : succInDef.keySet()) {
            UnionFindSet<StmtOccur> web = webs.get(var);
            assert predOutDef.containsKey(var);
            web.union(predOutDef.get(var), succInDef.get(var));
        }
    }

    private Map<Var, StmtOccur> washDefs(BytecodeBlock block, Map<Var, StmtOccur> inDef) {
        Map<Var, StmtOccur> res = Maps.newMap(inDef.size());
        for (Var var : getDefsAtStartOfBlock(block)) {
            assert inDef.containsKey(var);
            res.put(var, inDef.get(var));
        }
        return res;
    }

    private Map<Var, StmtOccur> getPhantomInDefs(BytecodeBlock entry) {
        boolean isInTry = entry.isInTry();
        Map<Var, StmtOccur> res = new HashMap<>();
        Map<Var, List<StmtOccur>> mayFlowToCatch = isInTry ? new HashMap<>() : null;
        Kind phantomType = entry == builder.getEntryBlock() ? Kind.PARAM : Kind.PHANTOM;
        for (Var var : getDefsAtStartOfBlock(entry)) { // initialization.
            Copy phantom = new Copy(var, var);
            StmtOccur e = new StmtOccur(entry, -1, phantom, phantomType);
            UnionFindSet<StmtOccur> web = webs.get(var);
            assert web != null;
            web.addElement(e);
            res.put(var, e);
            if (isInTry) {
                // collect phantom to mayFlowToCatch
                List<StmtOccur> varDefs = List.of(e);
                mayFlowToCatch.put(var, varDefs);
            }
        }
        if (isInTry) {
            mayFlowToCatchOfBlocks.put(entry, mayFlowToCatch);
        }
        return res;
    }

    private void constructWebInsideBlock(BytecodeBlock block, Map<Var, StmtOccur> inDefs) {
        boolean isInTry = block.isInTry();
        Map<Var, List<StmtOccur>> mayFlowToCatch = isInTry ? new HashMap<>(inDefs.size()) : null;
        Map<Var, StmtOccur> currentDefs;
        int size = block.inEdges().size();
        if (size > 1) {
            if (block2inDefs.containsKey(block)) {
                Map<Var, StmtOccur> otherInDefs = block2inDefs.get(block);
                mergeDefs(otherInDefs, inDefs);
                return;
            } else {
                Map<Var, StmtOccur> realInDef = washDefs(block, inDefs);
                block2inDefs.put(block, realInDef);
                currentDefs = new HashMap<>(realInDef);
            }
        }  else {
            if (block == builder.getEntryBlock() && this.block2inDefs.get(block) != null) {
                mergeDefs(block2inDefs.get(block), inDefs);
                return;
            }
            assert block2inDefs.get(block) == null;
            block2inDefs.put(block, inDefs);
            currentDefs = new HashMap<>(inDefs);
        }

        if (isInTry) {
            currentDefs.forEach((key, value) -> {
                List<StmtOccur> occurs = new ArrayList<>();
                occurs.add(value);
                mayFlowToCatch.put(key, occurs);
            });
        }

        List<Stmt> stmts = getStmts(block);
        for (int i = 0; i < stmts.size(); ++i) {
            Stmt stmt = stmts.get(i);
            // uses first
            Collection<RValue> uses = stmt.getUses();
            for (RValue r : uses) {
                if (r instanceof Var use && localSet.contains(use)) {
                    StmtOccur e = new StmtOccur(block, i, stmt, Kind.USE);
                    UnionFindSet<StmtOccur> unionFind = webs.get(use);
                    unionFind.addElement(e);
                    assert currentDefs.get(use) != null;
                    unionFind.union(e, currentDefs.get(use));
                }
            }

            Var def = stmt.getDef()
                    .flatMap(l -> l instanceof Var ? Optional.of((Var) l) : Optional.empty())
                    .filter(localSet::contains)
                    .orElse(null);
            if (def != null) {
                StmtOccur e = new StmtOccur(block, i, stmt, Kind.DEF);
                UnionFindSet<StmtOccur> unionFind = webs.get(def);
                unionFind.addElement(e);
                currentDefs.put(def, e);

                if (isInTry) {
                    List<StmtOccur> varDefs =
                            mayFlowToCatch.computeIfAbsent(def, k -> new ArrayList<>());
                    varDefs.add(e);
                }
            }
        }

        if (isInTry) {
            mayFlowToCatchOfBlocks.put(block, mayFlowToCatch);
        }

        for (BytecodeBlock bytecodeBlock : block.outEdges()) {
            constructWebInsideBlock(bytecodeBlock, currentDefs);
        }
    }

    private void constructWebBetweenTryAndHandler(BytecodeBlock tryBlock, BytecodeBlock handler) {
        Map<Var, List<StmtOccur>> blockAllDefs = mayFlowToCatchOfBlocks.get(tryBlock);
        Map<Var, StmtOccur> handlerInDefs = block2inDefs.get(handler);
        for (Var var : getDefsAtStartOfBlock(handler)) {
            var web = webs.get(var);
            assert blockAllDefs.containsKey(var);
            for (var p : blockAllDefs.get(var)) {
                web.union(p, handlerInDefs.get(var));
            }
        }
    }

    private Map<Var, List<ReplaceSource>> spiltVariable() {
        Map<Var, List<ReplaceSource>> res = Maps.newMap();
        webs.forEach((var, web) -> {
            int slot = varManager.getSlot(var);
            Var[] currentVar = new Var[]{var};
            int[] count = new int[]{0};
            web.getDisjointSets()
                    .stream()
                    .sorted((a, b) -> Integer.compare(b.size(), a.size()))
                    .forEach(s -> {
                        List<ReplaceSource> sources = new ArrayList<>();
                        s.forEach(p -> {
                            if (p.second() != Kind.PHANTOM) {
                                sources.add(new ReplaceSource(p, var));
                            }
                        });
                        if (sources.isEmpty()) {
                            return;
                        }
                        count[0]++;
                        Stream<AbstractInsnNode> origins = sources.stream()
                                .filter(source -> source.index().second() != Kind.PARAM)
                                .map(source -> {
                                    BytecodeBlock block = source.index().block();
                                    int index = source.index().index();
                                    return block.getOrig(index);
                                });
                        currentVar[0] = varManager.splitLocal(var, count[0], slot, origins);
                        if (count[0] != 1) {
                            res.put(currentVar[0], sources);
                            StmtOccur rep = s.iterator().next();
                            split.put(new SplitIndex(var, web.findRoot(rep)), currentVar[0]);
                        }
                    });
        });
        return res;
    }

    private void update() {
        Map<Var, List<ReplaceSource>> m = spiltVariable();
        Map<Var, Var> defMap = Maps.newMap();
        Map<Var, Var> useMap = Maps.newMap();
        Lenses lenses = new Lenses(builder.method, useMap, defMap);
        m.forEach((target, sources) -> {
            for (ReplaceSource source : sources) {
                useMap.clear();
                defMap.clear();
                Kind kind = source.index().second();
                if (kind == Kind.DEF) {
                    defMap.put(source.old(), target);
                } else if (kind == Kind.USE) {
                    useMap.put(source.old(), target);
                } else if (kind == Kind.PARAM) {
                    varManager.replaceParam(source.old(), target);
                    // don't set stmt
                    continue;
                } else {
                    throw new UnsupportedOperationException();
                }

                BytecodeBlock block = source.index().block();
                int idx = source.index().index();
                Stmt oldStmt = block.getStmts().get(idx);
                Stmt newStmt = lenses.subSt(oldStmt);
                handleSideEffects(oldStmt, newStmt);
                block.getStmts().set(idx, newStmt);
            }
        });

        if (builder.isFrameUsable()) {
            updateInDefs();
        }
    }

    private void handleSideEffects(Stmt oldStmt, Stmt newStmt) {
        if (oldStmt instanceof Return r) {
            assert newStmt instanceof Return;
            varManager.getRetVars().remove(r.getValue());
            varManager.getRetVars().add(((Return) newStmt).getValue());
        }
    }

    private void updateInDefs() {
        for (BytecodeBlock block : builder.blockSortedList) {
            if (block.getFrame() == null) {
                continue;
            }
            Map<Var, StmtOccur> inDefs = block2inDefs.get(block);
            assert inDefs != null;
            Map<Integer, Var> frameLocalVar = Maps.newMap();
            for (Pair<Integer, Var> p : varManager.getDefsBeforeStartOfABlock(block)) {
                int idx = p.first();
                Var v = p.second();
                StmtOccur real = inDefs.get(v);
                Var realVar;
                realVar = split.get(new SplitIndex(v, webs.get(v).findRoot(real)));
                frameLocalVar.put(idx, realVar != null ? realVar : v);
            }
            block.setFrameLocalVar(frameLocalVar);
        }
    }

    private Iterable<Var> getDefsAtStartOfBlock(BytecodeBlock block) {
        if (builder.isFrameUsable()) {
            if (block.getFrame() == null) {
                assert block.inEdges().size() == 1 && block.inEdges().get(0).fallThrough() == block ||
                        block.inEdges().stream().allMatch(b -> b.getFrame() == null) && !block.isCatch();
                return varManager.getParamThis();
            }

            return varManager.getDefsBeforeStartOfABlock(block).stream().map(Pair::second).toList();
        } else {
            SetFact<Var> inFact = getInFact(block);
            inFact.intersect(localSet);
            return inFact;
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

    private record StmtOccur(BytecodeBlock block, int index, Stmt first, Kind second) {}

    private record ReplaceSource(StmtOccur index, Var old) {}

    private record SplitIndex(Var v, StmtOccur rep) {}
}
