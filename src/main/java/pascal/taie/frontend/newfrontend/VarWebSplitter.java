package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.tree.AbstractInsnNode;
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

    private final Map<AbstractInsnNode, Stmt> asm2Stmt;

    private final Map<AbstractInsnNode, List<Stmt>> auxiliaryStmts;

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

    public VarWebSplitter(AsmIRBuilder builder) {
        this.builder = builder;
        this.varManager = builder.manager;
        this.asm2Stmt = builder.asm2Stmt;
        this.auxiliaryStmts = builder.auxiliaryStmts;
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
        var blocks = builder.label2Block.values();
        for (var block : blocks) {
//            if (! block.isCatch()) { // TODO: remove when exception handling is completed.
                constructWebInsideBlock(block);
//            }
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
        for (Var var : locals) {
            var web = webs.get(var);
            web.union(predDef.get(var), succUse.get(var));
        }
    }

    private void constructWebInsideBlock(BytecodeBlock block) {
        boolean isInTry = this.isInTry.contains(block);
        Map<Var, Pair<Stmt, Kind>> phantomUse = new HashMap<>();
        Map<Var, List<Pair<Stmt, Kind>>> mayFlowToCatch = isInTry ? new HashMap<>() : null;
        for (Var var : locals) { // initialization.
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
        this.inUse.put(block, phantomUse);

        Map<Var, Pair<Stmt, Kind>> currentDefs = new HashMap<>(phantomUse);

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
                assert previous != null;

                if (isInTry) {
                    var varDefs = mayFlowToCatch.get(def);
                    varDefs.add(e);
                }
            }
        }

        outDef.put(block, currentDefs);
        if (isInTry) {
            mayFlowToCatchOfBlocks.put(block, mayFlowToCatch);
        }
    }

    private void constructWebBetweenTryAndHandler(BytecodeBlock tryBlock, BytecodeBlock handler) {
        var blockAllDefs = mayFlowToCatchOfBlocks.get(tryBlock);
        var handlerUse = inUse.get(handler);
        for (Var var : locals) {
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

        modifyLists.forEach(builder.stmts::set);
    }

    private List<Stmt> getStmts(BytecodeBlock block) {
        List<Stmt> stmts = new ArrayList<>();
        for (var node : block.instr()) {
            if (asm2Stmt.containsKey(node)) {
                stmts.add(asm2Stmt.get(node));

            }
            if (auxiliaryStmts.containsKey(node)) {
                stmts.addAll(auxiliaryStmts.get(node));
            }
        }
        return stmts;
    }

    private enum Kind {
        DEF,
        USE,
        PHANTOM, // PHANTOM for the fake Copy in each block: x = x, used as a connector
    }

    private record ReplaceSource(int index, Kind kind, Var old) {}
}
