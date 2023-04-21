package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.Opcodes;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.UnionFindSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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

    private final Set<BytecodeBlock> isInTry;

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
        this.isInTry = new HashSet<>();
        this.locals = varManager.getVars()
                .stream()
                .filter(varManager::isLocal)
                .toList();
    }

    public void build() {
        if (builder.label2Block.values().size() == 1) {
            return;
        }
        initWebs();
        findTryBlocks();
        constructWeb();
        update();
    }

    private void initWebs() {
        for (Var var : locals) {
            webs.put(var, new UnionFindSet<>(new ArrayList<>()));
        }
    }

    private void findTryBlocks() {
        for (var tryCatchPair : tryAndHandlerBlocks) {
            var trys = tryCatchPair.first();
            isInTry.addAll(trys);
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
        for (Var var : getLocals(succ)) {
            var web = webs.get(var);
            web.union(predDef.get(var), succUse.get(var));
        }
    }

    private void constructWebInsideBlock(BytecodeBlock block, Map<Var, Pair<Stmt, Kind>> inUse) {
        boolean isInTry = this.isInTry.contains(block);
        Map<Var, List<Pair<Stmt, Kind>>> mayFlowToCatch = isInTry ? new HashMap<>() : null;
        if (inUse == null) {
            Map<Var, Pair<Stmt, Kind>> phantomUse = new HashMap<>();
            for (Var var : getLocals(block)) { // initialization.
                Copy phantom = new Copy(var, var);
                Pair<Stmt, Kind> e = new Pair<>(phantom, Kind.PHANTOM);
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
        for (Var var : getLocals(handler)) {
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
                        res.put(currentVar[0], sources);
                        count[0]++;
                        currentVar[0] = varManager.splitLocal(var, count[0]);
                    });
        });
        return res;
    }

    private void update() {
        Map<Var, List<ReplaceSource>> m = spiltVariable();
        Map<Integer, Stmt> modifyLists = Maps.newMap();
        Map<Var, Var> defMap = Maps.newMap();
        Map<Var, Var> useMap = Maps.newMap();
        Lenses lenses = new Lenses(builder.method, defMap, useMap);
        m.forEach((target, sources) -> {
            useMap.clear();
            defMap.clear();
            // assert ! target.getName().startsWith("this");
            for (ReplaceSource source : sources) {
                Stmt oldStmt = modifyLists.computeIfAbsent(source.index, builder.stmts::get);
                if (source.kind == Kind.DEF) {
                    defMap.put(source.old(), target);
                } else if (source.kind == Kind.USE) {
                    defMap.put(source.old(), target);
                } else {
                    throw new UnsupportedOperationException();
                }

                Stmt newStmt = lenses.subSt(oldStmt);
                modifyLists.put(source.index(), newStmt);
            }
        });

        modifyLists.forEach(builder::setStmts);
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

    private List<Stmt> getStmts(BytecodeBlock block) {
        return builder.getStmts(block);
    }

    private enum Kind {
        DEF,
        USE,
        PHANTOM, // PHANTOM for the fake Copy in each block: x = x, used as a connector
    }

    private record ReplaceSource(int index, Kind kind, Var old) {}
}
