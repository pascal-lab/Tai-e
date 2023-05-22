package pascal.taie.frontend.newfrontend;

import org.checkerframework.checker.nullness.qual.Nullable;
import pascal.taie.analysis.dataflow.fact.DataflowResult;
import pascal.taie.analysis.dataflow.fact.SetFact;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.UnionFindSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
            > inDef;

    private final Map
            <
                    BytecodeBlock,
                    Map<
                            Var,
                            StmtOccur
                    >
            > outDef;

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

    // TODO:
    //   (optional) remove all phantom defs by dfs / bfs traversal
    public VarWebSplitter(AsmIRBuilder builder, DataflowResult<Stmt, SetFact<Var>> liveVariables) {
        this.builder = builder;
        this.varManager = builder.manager;
        this.webs = new HashMap<>();
        this.inDef = new HashMap<>();
        this.outDef = new HashMap<>();
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

    /**
     * @param var the variable you want to get the web information.
     * @return the webs of the variable. WARNING: in each set there may exist element whose Kind is PHANTOM.
     */
    public Collection<Set<StmtOccur>> getWebs(Var var) {
        return webs.get(var).getDisjointSets();
    }

    public void constructWeb() {
        var blocks = builder.blockSortedList;
        for (var block : blocks) {
            if (! inDef.containsKey(block)) {
                constructWebInsideBlock(block, null);
            }
        }

        for (var block : blocks) {
            for (var succ : block.outEdges()) {
                constructWebBetweenBlock(block, succ);
            }
        }

        for (var tryCatchPair : tryAndHandlerBlocks) {
            var trys = tryCatchPair.first();
            var handler = tryCatchPair.second();
            for (var t : trys) {
                constructWebBetweenTryAndHandler(t, handler);
            }
        }
    }

    private void constructWebBetweenBlock(BytecodeBlock pred, BytecodeBlock succ) {
        var predOutDef = outDef.get(pred);
        var succInDef = inDef.get(succ);
        if (predOutDef == succInDef) {
            return;
        }
        for (Var var : succInDef.keySet()) {
            var web = webs.get(var);
            assert predOutDef.containsKey(var);
            web.union(predOutDef.get(var), succInDef.get(var));
        }
    }

    private void constructWebInsideBlock(BytecodeBlock block, Map<Var, StmtOccur> inDef) {
        boolean isInTry = block.isInTry();
        Map<Var, List<StmtOccur>> mayFlowToCatch = isInTry ? new HashMap<>() : null;
        if (inDef == null || isFrameProvided(block)) {
            Kind phantomType = block == builder.getEntryBlock() ? Kind.PARAM : Kind.PHANTOM;
            Map<Var, StmtOccur> phantomDefs = new HashMap<>();
            for (Var var : getDefsAtStartOfBlock(block)) { // initialization.
                Copy phantom = new Copy(var, var);
                StmtOccur e = new StmtOccur(block, -1, phantom, phantomType);
                var web = webs.get(var);
                assert web != null;
                web.addElement(e);
                phantomDefs.put(var, e);
                if (isInTry) {
                    // collect phantom to mayFlowToCatch
                    List<StmtOccur> varDefs = new ArrayList<>();
                    varDefs.add(e);
                    mayFlowToCatch.put(var, varDefs);
                }
            }
            inDef = phantomDefs;
        } else {
            if (mayFlowToCatch != null) {
                inDef.forEach((k, v) -> {
                    List<StmtOccur> temp = new ArrayList<>();
                    temp.add(v);
                    mayFlowToCatch.put(k, temp);
                });
            }
        }

        this.inDef.put(block, inDef);
        Map<Var, StmtOccur> currentDefs = new HashMap<>(inDef);

        List<Stmt> stmts = getStmts(block);
        for (int i = 0; i < stmts.size(); ++i) {
            Stmt stmt = stmts.get(i);
            // uses first
            List<Var> uses = stmt.getUses()
                    .stream()
                    .distinct()
                    .filter(r -> r instanceof Var)
                    .map(r -> (Var) r)
                    .filter(varManager::isLocal)
                    .toList();
            for (Var use : uses) {
                var e = new StmtOccur(block, i, stmt, Kind.USE);
                var unionFind = webs.get(use);
                unionFind.addElement(e);
                assert currentDefs.get(use) != null;
                unionFind.union(e, currentDefs.get(use));
            }

            Var def = stmt.getDef()
                    .flatMap(l -> l instanceof Var ? Optional.of((Var) l) : Optional.empty())
                    .filter(varManager::isLocal)
                    .orElse(null);
            if (def != null) {
                StmtOccur e = new StmtOccur(block, i, stmt, Kind.DEF);
                var unionFind = webs.get(def);
                unionFind.addElement(e);
                var previous = currentDefs.put(def, e);
                // assert previous != null;

                if (isInTry) {
                    var varDefs = mayFlowToCatch.computeIfAbsent(def, k -> new ArrayList<>());
                    varDefs.add(e);
                }
            }
        }

        outDef.put(block, currentDefs);
        if (isInTry) {
            mayFlowToCatchOfBlocks.put(block, mayFlowToCatch);
        }

        if (block.fallThrough() != null && builder.isFrameUsable()) {
            constructWebInsideBlock(block.fallThrough(), currentDefs);
        }
    }

    private void constructWebBetweenTryAndHandler(BytecodeBlock tryBlock, BytecodeBlock handler) {
        var blockAllDefs = mayFlowToCatchOfBlocks.get(tryBlock);
        var handlerInDef = inDef.get(handler);
        for (Var var : getDefsAtStartOfBlock(handler)) {
            var web = webs.get(var);
            assert blockAllDefs.containsKey(var);
            for (var p : blockAllDefs.get(var)) {
                web.union(p, handlerInDef.get(var));
            }
        }
    }

    private Map<Var, List<ReplaceSource>> spiltVariable() {
        Map<Var, List<ReplaceSource>> res = Maps.newMap();
        webs.forEach((var, web) -> {
            Var[] currentVar = new Var[]{var};
            int[] count = new int[]{0};
            web.getDisjointSets()
                    .stream()
                    .sorted((a, b) -> Integer.compare(b.size(), a.size()))
                    .skip(1)
                    .forEach(s -> {
                        List<ReplaceSource> sources = new ArrayList<>();
                        assert ! currentVar[0].getName().startsWith("this");
                        s.forEach(p -> {
                            if (p.second() != Kind.PHANTOM) {
                                sources.add(new ReplaceSource(p, var));
                            }
                        });
                        if (sources.size() == 0) {
                            return;
                        }
                        count[0]++;
                        currentVar[0] = varManager.splitLocal(var, count[0]);
                        res.put(currentVar[0], sources);
                        StmtOccur rep = s.iterator().next();
                        split.put(new SplitIndex(var, web.findRoot(rep)), currentVar[0]);
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
            // assert ! target.getName().startsWith("this");
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

    // TODO: correct handle other side-effects of instr set;
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
            Map<Var, StmtOccur> inDef = this.inDef.get(block);
            assert inDef != null;
            Map<Integer, Var> frameLocalVar = Maps.newMap();
            for (Pair<Integer, Var> p : varManager.getDefsBeforeStartOfABlock(block)) {
                int idx = p.first();
                Var v = p.second();
                StmtOccur real = inDef.get(v);
                Var realVar;
                realVar = split.get(new SplitIndex(v, webs.get(v).findRoot(real)));
                frameLocalVar.put(idx, realVar != null ? realVar : v);
            }
            block.setFrameLocalVar(frameLocalVar);
        }
    }

    private boolean isFrameProvided(BytecodeBlock block) {
        return ! builder.isFrameUsable() || block.getFrame() != null;
    }

    private Iterable<Var> getDefsAtStartOfBlock(BytecodeBlock block) {
        if (builder.isFrameUsable()) {
            if (block.getFrame() == null) {
                assert block.inEdges().size() == 1 && block.inEdges().get(0).fallThrough() == block ||
                        block.inEdges().stream().allMatch(b -> b.getFrame() == null) && !block.isCatch();
                return varManager.getParams();
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
        return liveVariables.getInFact(getStmts(block).get(0));
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
