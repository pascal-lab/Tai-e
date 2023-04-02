package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.tree.AbstractInsnNode;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Binary;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.Goto;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.InstanceOf;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.LookupSwitch;
import pascal.taie.ir.stmt.Monitor;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.Nop;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.ir.stmt.TableSwitch;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.ir.stmt.Unary;
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

    private final Map<
            Var,
            UnionFindSet<Pair<? extends Stmt, Kind>>
            > webs;

    private final Map<BytecodeBlock, Map<Var, Pair<Copy, Kind>>> inUse;

    private final Map<BytecodeBlock, Map<Var, Pair<? extends DefinitionStmt<? extends LValue, ? extends RValue>, Kind>>> outDef;

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

    public Collection<Set<Pair<? extends Stmt, Kind>>> getDisjointSets(Var var) {
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

        DefVisitor defVisitor = new DefVisitor();
        UseVisitor useVisitor = new UseVisitor();
        for (var stmt : getStmts(block)) {
            // uses first
            List<Var> uses = stmt.accept(useVisitor);
            for (Var use : uses) {
                var e = new Pair<>(stmt, Kind.USE);
                var unionfind = webs.get(use);
                unionfind.addElement(e);
                unionfind.union(e, currentDefs.get(use));
            }

            Var def = stmt.accept(defVisitor);
            if (def != null) { // which means stmt is a DefinitionStmt.
                var e = new Pair<>((DefinitionStmt<? extends LValue, ? extends RValue>) stmt, Kind.DEF);
                var unionfind = webs.get(def);
                unionfind.addElement(e);
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

    private static class DefVisitor implements StmtVisitor<Var> {

        @Override
        public Var visit(New stmt) {
            return stmt.getLValue();
        }

        @Override
        public Var visit(AssignLiteral stmt) {
            return stmt.getLValue();
        }

        @Override
        public Var visit(Copy stmt) {
            return stmt.getLValue();
        }

        @Override
        public Var visit(LoadArray stmt) {
            return stmt.getLValue();
        }

        @Override
        public Var visit(StoreArray stmt) {
            return visitDefault(stmt);
        }

        @Override
        public Var visit(LoadField stmt) {
            return stmt.getLValue();
        }

        @Override
        public Var visit(StoreField stmt) {
            return visitDefault(stmt);
        }

        @Override
        public Var visit(Binary stmt) {
            return stmt.getLValue();
        }

        @Override
        public Var visit(Unary stmt) {
            return stmt.getLValue();
        }

        @Override
        public Var visit(InstanceOf stmt) {
            return stmt.getLValue();
        }

        @Override
        public Var visit(Cast stmt) {
            return stmt.getLValue();
        }

        @Override
        public Var visit(Goto stmt) {
            return visitDefault(stmt);
        }

        @Override
        public Var visit(If stmt) {
            return visitDefault(stmt);
        }

        @Override
        public Var visit(TableSwitch stmt) {
            return visitDefault(stmt);
        }

        @Override
        public Var visit(LookupSwitch stmt) {
            return visitDefault(stmt);
        }

        @Override
        public Var visit(Invoke stmt) {
            return stmt.getLValue();
        }

        @Override
        public Var visit(Return stmt) {
            return visitDefault(stmt);
        }

        @Override
        public Var visit(Throw stmt) {
            return visitDefault(stmt);
        }

        @Override
        public Var visit(Catch stmt) {
            return visitDefault(stmt);
        }

        @Override
        public Var visit(Monitor stmt) {
            return visitDefault(stmt);
        }

        @Override
        public Var visit(Nop stmt) {
            return visitDefault(stmt);
        }

        @Override
        public Var visitDefault(Stmt stmt) {
            return null;
        }
    }

    private static class UseVisitor implements StmtVisitor<List<Var>> {

        @Override
        public List<Var> visit(New stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visit(AssignLiteral stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visit(Copy stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visit(LoadArray stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visit(StoreArray stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visit(LoadField stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visit(StoreField stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visit(Binary stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visit(Unary stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visit(InstanceOf stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visit(Cast stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visit(Goto stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visit(If stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visit(TableSwitch stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visit(LookupSwitch stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visit(Invoke stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visit(Return stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visit(Throw stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visit(Catch stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visit(Monitor stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visit(Nop stmt) {
            return visitDefault(stmt);
        }

        @Override
        public List<Var> visitDefault(Stmt stmt) {
            return new ArrayList<>();
        }
    }
}
