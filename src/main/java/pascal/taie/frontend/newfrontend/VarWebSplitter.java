package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.tree.AbstractInsnNode;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.DefinitionStmt;
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

    private final Map<AbstractInsnNode, Stmt> asm2Stmt;

    private final Map<AbstractInsnNode, List<Stmt>> auxiliaryStmts;

    private final List<Var> locals;

    private final Map
            <
                    Var,
                    UnionFindSet<Pair<? extends Stmt, Kind>>
            > webs;

    private final Map
            <
                    BytecodeBlock,
                    Map<
                            Var,
                            Pair<Copy, Kind>
                    >
            > inUse;

    private final Map
            <
                    BytecodeBlock,
                    Map<
                            Var,
                            Pair<? extends DefinitionStmt<? extends LValue, ? extends RValue>, Kind>
                    >
            > outDef;

    public VarWebSplitter(AsmIRBuilder builder) {
        this.builder = builder;
        this.varManager = builder.manager;
        this.asm2Stmt = builder.asm2Stmt;
        this.auxiliaryStmts = builder.auxiliaryStmts;
        this.webs = new HashMap<>();
        this.inUse = new HashMap<>();
        this.outDef = new HashMap<>();
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
    public Collection<Set<Pair<? extends Stmt, Kind>>> getWebs(Var var) {
        return webs.get(var).getDisjointSets();
    }

    public void constructWeb() {
        var blocks = builder.label2Block.values();
        for (var block : blocks) {
            if (! block.isCatch()) {
                constructWebInsideBlock(block);
            }
        }

        for (var block : blocks) {
            for (var succ : block.outEdges()) {
                constructWebBetweenBlock(block, succ);
            }
        }

        // TODO: implicit exception edge to be processed.
    }

    private void constructWebBetweenBlock(BytecodeBlock pred, BytecodeBlock succ) {
        if (pred.isCatch() || succ.isCatch()) {
            return;
        }
        var predDef = outDef.get(pred);
        var succUse = inUse.get(succ);
        for (Var var : locals) {
            var web = webs.get(var);
            web.union(predDef.get(var), succUse.get(var));
        }
    }

    private void constructWebInsideBlock(BytecodeBlock block) {
        Map<Var, Pair<Copy, Kind>> phantomUse = new HashMap<>();
        for (Var var : locals) {
            Copy phantom = new Copy(var, var);
            Pair<Copy, Kind> e = new Pair<>(phantom, Kind.PHANTOM);
            webs.get(var).addElement(e);
            phantomUse.put(var, e);
        }
        this.inUse.put(block, phantomUse);

        Map<Var, Pair<? extends DefinitionStmt<? extends LValue, ? extends RValue>, Kind>> currentDefs = new HashMap<>(phantomUse);

        for (var stmt : getStmts(block)) {
            // uses first
            stmt.getUses()
                    .stream()
                    .filter(r -> r instanceof Var)
                    .map(r -> (Var) r)
                    .filter(varManager::isLocal)
                    .distinct().forEach(use ->  {
                var e = new Pair<>(stmt, Kind.USE);
                var unionFind = webs.get(use);
                unionFind.addElement(e);
                unionFind.union(e, currentDefs.get(use));
            });

            Var def = stmt.getDef()
                    .flatMap(l -> l instanceof Var ? Optional.of((Var) l) : Optional.empty())
                    .filter(varManager::isLocal)
                    .orElse(null);
            if (def != null) { // which means stmt is a DefinitionStmt.
                Pair<DefinitionStmt<? extends LValue, ? extends RValue>, Kind> e =
                        new Pair<>((DefinitionStmt<? extends LValue, ? extends RValue>) stmt, Kind.DEF);
                var unionFind = webs.get(def);
                unionFind.addElement(e);
                var previous = currentDefs.put(def, e);
                assert previous != null;
            }
        }

        outDef.put(block, currentDefs);
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

    enum Kind {
        DEF,
        USE,
        PHANTOM, // PHANTOM for the fake Copy in each block: x = x, used as a connector
    }

    private record ReplaceSource(int index, Kind kind, Var old) { }
}
