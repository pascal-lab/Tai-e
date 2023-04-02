package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.tree.AbstractInsnNode;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.UnionFindSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VarWebSplitter {

    private final AsmIRBuilder builder;

    private final VarManager varManager;

    private final Map<AbstractInsnNode, Stmt> asm2Stmt;

    private final Map<AbstractInsnNode, List<Stmt>> auxiliaryStmts;

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
        initWebs();
    }

    private void initWebs() {
        for (Var var : varManager.getVars()) {
            webs.put(var, new UnionFindSet<>(new ArrayList<>()));
        }
    }

    /**
     *
     * @param var the variable you want to get the web information.
     * @return the webs of the variable. WARNING: in each set there may exist element whose Kind is PHANTOM.
     */
    public Collection<Set<Pair<? extends Stmt, Kind>>> getWebs(Var var) {
        return webs.get(var).getDisjointSets();
    }

    public void constructWeb() {
        var blocks = builder.label2Block.values();
        for (var block : blocks) {
            constructWebInsideBlock(block);
        }

        for (var block : blocks) {
            for (var succ : block.outEdges()) {
                constructWebBetweenBlock(block, succ);
            }
        }

        // TODO: implicit exception edge to be processed.
    }

    private void constructWebBetweenBlock(BytecodeBlock pred, BytecodeBlock succ) {
        var predDef = outDef.get(pred);
        var succUse = inUse.get(succ);
        for (Var var : varManager.getVars()) {
            var web = webs.get(var);
            web.union(predDef.get(var), succUse.get(var));
        }
    }

    private void constructWebInsideBlock(BytecodeBlock block) {
        Map<Var, Pair<Copy, Kind>> phantomUse = new HashMap<>();
        for (Var var : varManager.getVars()) {
            Copy phantom = new Copy(var, var);
            Pair<Copy, Kind> e = new Pair<>(phantom, Kind.PHANTOM);
            webs.get(var).addElement(e);
            phantomUse.put(var, e);
        }
        this.inUse.put(block, phantomUse);

        Map<Var, Pair<? extends DefinitionStmt<? extends LValue, ? extends RValue>, Kind>> currentDefs = new HashMap<>(phantomUse);

        for (var stmt : getStmts(block)) {
            // uses first
            List<Var> uses = stmt.getUses().stream().filter(r -> r instanceof Var).map(r -> (Var) r).toList();
            for (Var use : uses) {
                var e = new Pair<>(stmt, Kind.USE);
                var unionFind = webs.get(use);
                unionFind.addElement(e);
                unionFind.union(e, currentDefs.get(use));
            }

            Var def = (Var) stmt.getDef().filter(l -> l instanceof Var).orElse(null);
            if (def != null) { // which means stmt is a DefinitionStmt.
                var e = new Pair<>((DefinitionStmt<? extends LValue, ? extends RValue>) stmt, Kind.DEF);
                var unionFind = webs.get(def);
                unionFind.addElement(e);
                var previous = currentDefs.put(def, e);
                assert previous != null;
            }
        }

        outDef.put(block, currentDefs);
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

}
