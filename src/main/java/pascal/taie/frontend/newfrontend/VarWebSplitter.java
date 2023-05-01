package pascal.taie.frontend.newfrontend;

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

    private final Map
            <
                    Var,
                    UnionFindSet<Pair<Stmt, Kind>>
            > webs;

    private final Map
            <
                    BytecodeBlock,
                    Map<
                            Var,
                            Pair<Stmt, Kind>
                    >
            > inUse;

    private final Map
            <
                    BytecodeBlock,
                    Map<
                            Var,
                            Pair<Stmt, Kind>
                    >
            > outDef;

    private final Map
            <
                    BytecodeBlock,
                    Map<
                            Var,
                            List<Pair<Stmt, Kind>>
                    >
            > mayFlowToCatchOfBlocks; // contains all the defs. Used in exception handling.

    private final List<Pair<List<BytecodeBlock>, BytecodeBlock>> tryAndHandlerBlocks;

    private final Map<SplitIndex, Var> splitted;

    // TODO:
    //   1. if classfile major version is less than 50,
    //      then our algorithm may not be able to split some defs
    //   2. only merge and add phantom defs when this var occurs in frame locals
    //   3. (optional) remove all phantom defs by dfs / bfs traversal
    public VarWebSplitter(AsmIRBuilder builder) {
        this.builder = builder;
        this.varManager = builder.manager;
        this.webs = new HashMap<>();
        this.inUse = new HashMap<>();
        this.outDef = new HashMap<>();
        this.mayFlowToCatchOfBlocks = new HashMap<>();
        this.tryAndHandlerBlocks = builder.getTryAndHandlerBlocks();
        this.splitted = Maps.newMap();
        this.locals = varManager.getVars()
                .stream()
                .filter(varManager::isLocal)
                .toList();
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
    public Collection<Set<Pair<Stmt, Kind>>> getWebs(Var var) {
        return webs.get(var).getDisjointSets();
    }

    public void constructWeb() {
        var blocks = builder.blockSortedList;
        for (var block : blocks) {
//            if (! block.isCatch()) { // TODO: remove when exception handling is completed.
//            }
            if (! inUse.containsKey(block)) {
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
//        if (pred.isCatch() || succ.isCatch()) { // TODO: remove when exception handling is completed.
//            return;
//        }
        var predDef = outDef.get(pred);
        var succUse = inUse.get(succ);
        if (predDef == succUse) {
            return;
        }
        for (Var var : succUse.keySet()) {
            if (!predDef.containsKey(var)) {
                continue;
            }
            var web = webs.get(var);
            web.union(predDef.get(var), succUse.get(var));
        }
    }

    private void constructWebInsideBlock(BytecodeBlock block, Map<Var, Pair<Stmt, Kind>> inUse) {
        boolean isInTry = block.isInTry();
        Map<Var, List<Pair<Stmt, Kind>>> mayFlowToCatch = isInTry ? new HashMap<>() : null;
        if (inUse == null || block.getFrame() != null) {
            Kind phantomType = block == builder.getEntryBlock() ? Kind.PARAM : Kind.PHANTOM;
            Map<Var, Pair<Stmt, Kind>> phantomUse = new HashMap<>();
            for (Var var : getDefsAtStartOfBlock(block)) { // initialization.
                Copy phantom = new Copy(var, var);
                Pair<Stmt, Kind> e = new Pair<>(phantom, phantomType);
                webs.get(var).addElement(e);
                phantomUse.put(var, e);
                if (isInTry) {
                    // collect phantom to mayFlowToCatch
                    List<Pair<Stmt, Kind>> varDefs = new ArrayList<>();
                    varDefs.add(e);
                    mayFlowToCatch.put(var, varDefs);
                }
            }
            inUse = phantomUse;
        } else {
            if (mayFlowToCatch != null) {
                inUse.forEach((k, v) -> {
                    List<Pair<Stmt, Kind>> temp = new ArrayList<>();
                    temp.add(v);
                    mayFlowToCatch.put(k, temp);
                });
            }
        }

        this.inUse.put(block, inUse);
        Map<Var, Pair<Stmt, Kind>> currentDefs = new HashMap<>(inUse);

        for (var stmt : getStmts(block)) {
            // uses first
            stmt.getUses()
                    .stream()
                    .distinct()
                    .filter(r -> r instanceof Var)
                    .map(r -> (Var) r)
                    .filter(varManager::isLocal)
                    .forEach(use ->  {
                var e = new Pair<>(stmt, Kind.USE);
                var unionFind = webs.get(use);
                unionFind.addElement(e);
                unionFind.union(e, currentDefs.get(use));
            });

            Var def = stmt.getDef()
                    .flatMap(l -> l instanceof Var ? Optional.of((Var) l) : Optional.empty())
                    .filter(varManager::isLocal)
                    .orElse(null);
            if (def != null) {
                Pair<Stmt, Kind> e = new Pair<>(stmt, Kind.DEF);
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

        if (block.fallThrough() != null) {
            constructWebInsideBlock(block.fallThrough(), currentDefs);
        }
    }

    private void constructWebBetweenTryAndHandler(BytecodeBlock tryBlock, BytecodeBlock handler) {
        var blockAllDefs = mayFlowToCatchOfBlocks.get(tryBlock);
        var handlerUse = inUse.get(handler);
        for (Var var : getDefsAtStartOfBlock(handler)) {
            var web = webs.get(var);
            for (var p : blockAllDefs.get(var)) {
                web.union(p, handlerUse.get(var));
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
                                sources.add(new ReplaceSource(p.first().getIndex(), p.second(), var));
                            }
                        });
                        if (sources.size() == 0) {
                            return;
                        }
                        count[0]++;
                        currentVar[0] = varManager.splitLocal(var, count[0]);
                        res.put(currentVar[0], sources);
                        Pair<Stmt, Kind> rep = s.iterator().next();
                        splitted.put(new SplitIndex(var, web.findRoot(rep)), currentVar[0]);
                    });
        });
        return res;
    }

    private void update() {
        Map<Var, List<ReplaceSource>> m = spiltVariable();
        Map<Integer, Stmt> modifyLists = Maps.newMap();
        Map<Var, Var> defMap = Maps.newMap();
        Map<Var, Var> useMap = Maps.newMap();
        Lenses lenses = new Lenses(builder.method, useMap, defMap);
        m.forEach((target, sources) -> {
            // assert ! target.getName().startsWith("this");
            for (ReplaceSource source : sources) {
                useMap.clear();
                defMap.clear();
                if (source.kind == Kind.DEF) {
                    defMap.put(source.old(), target);
                } else if (source.kind == Kind.USE) {
                    useMap.put(source.old(), target);
                } else if (source.kind == Kind.PARAM) {
                    varManager.replaceParam(source.old(), target);
                    // don't set stmt
                    continue;
                } else {
                    throw new UnsupportedOperationException();
                }

                Stmt oldStmt = modifyLists.computeIfAbsent(source.index, builder.stmts::get);
                Stmt newStmt = lenses.subSt(oldStmt);
                modifyLists.put(source.index(), newStmt);
            }
        });

        // TODO: correct handle other side-effects of instr set;
        modifyLists.forEach((idx, instr) -> {
            Stmt oldInstr = builder.stmts.get(idx);
            if (instr instanceof Return r) {
                varManager.getRetVars().remove(((Return) oldInstr).getValue());
                varManager.getRetVars().add(r.getValue());
            }
            builder.setStmts(idx, instr);
        });

        for (BytecodeBlock block : builder.blockSortedList) {
            if (block.getFrame() == null) {
                continue;
            }
            Map<Var, Pair<Stmt, Kind>> inUse = this.inUse.get(block);
            assert inUse != null;
            Map<Integer, Var> frameLocalVar = Maps.newMap();
            for (Pair<Integer, Var> p : varManager.getDefsBeforeStartOfABlock(block)) {
                int idx = p.first();
                Var v = p.second();
                Pair<Stmt, Kind> real = inUse.get(v);
                Var realVar;
                realVar = splitted.get(new SplitIndex(v, webs.get(v).findRoot(real)));
                frameLocalVar.put(idx, realVar != null ? realVar : v);
            }
            block.setFrameLocalVar(frameLocalVar);
        }
    }

    private List<Var> getLocals(BytecodeBlock block) {
        // int size = block.getFrame() == null ? 0 : block.getFrame().local.size();
        if (block.getFrame() == null) {
            assert block.inEdges().size() == 1 && block.inEdges().get(0).fallThrough() == block ||
                    block.inEdges().stream().allMatch(b -> b.getFrame() == null) && ! block.isCatch();
            return varManager.getParams();
        }
        return varManager.getBlockVar(block);
    }

    private List<Var> getDefsAtStartOfBlock(BytecodeBlock block) {
        if (block.getFrame() == null) {
            assert block.inEdges().size() == 1 && block.inEdges().get(0).fallThrough() == block ||
                    block.inEdges().stream().allMatch(b -> b.getFrame() == null) && ! block.isCatch();
            return varManager.getParams();
        }

        return varManager.getDefsBeforeStartOfABlock(block).stream().map(Pair::second).toList();
    }

    private List<Stmt> getStmts(BytecodeBlock block) {
        return builder.getStmts(block);
    }

    private enum Kind {
        DEF,
        USE,
        PHANTOM, // PHANTOM for the fake Copy in each block: x = x, used as a connector
        PARAM
    }

    private record ReplaceSource(int index, Kind kind, Var old) {}

    private record SplitIndex(Var v, Pair<Stmt, Kind> rep) {}
}
