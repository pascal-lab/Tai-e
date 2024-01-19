package pascal.taie.frontend.newfrontend.ssa;

import pascal.taie.frontend.newfrontend.IBasicBlock;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.ExpVisitor;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PhiExp implements Exp, RValue {

    private List<Pair<Var, IBasicBlock>> usesAndInBlocks = new ArrayList<>();

    private List<Pair<Integer, Var>> sourceAndVar;

    public static final int METHOD_ENTRY = -1;

    void addUseAndCorrespondingBlocks(Var v, IBasicBlock block) {
        usesAndInBlocks.add(new Pair<>(v, block));
    }

    List<Pair<Var, IBasicBlock>> getUsesAndInBlocks() {
        return usesAndInBlocks;
    }

    public void indexValueAndSource() {
        sourceAndVar = new ArrayList<>(usesAndInBlocks.size());
        for (Pair<Var, IBasicBlock> p : usesAndInBlocks) {
            Var v = p.first();
            IBasicBlock b = p.second();
            int index = getSourceIndex(b);
            sourceAndVar.add(new Pair<>(index, v));
        }
        usesAndInBlocks = null;
    }

    public List<Pair<Integer, Var>> getSourceAndVar() {
        return sourceAndVar;
    }

    private static int getSourceIndex(IBasicBlock block) {
        if (block == null) {
            return METHOD_ENTRY;
        }
        int index;
        List<Stmt> stmts = block.getStmts();
        if (!stmts.isEmpty()) {
            index = stmts.get(stmts.size() - 1).getIndex();
        } else {
            /*
            The block is within a try block, and the bytecode in it is translated as side
            effect. So we have to find the next non-empty block and get its first stmt.
            */
            // TODO
            index = -2;
        }
        return index;
    }

    @Override
    public Type getType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<RValue> getUses() {
        return sourceAndVar
                .stream()
                .map(Pair::second)
                .collect(Collectors.toSet());
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        String repr;
        if (usesAndInBlocks == null) {
            repr = sourceAndVar.stream()
                    .map(p -> p.first() + ":" + p.second().toString())
                    .collect(Collectors.joining(", "));
        } else {
            repr = usesAndInBlocks.stream()
                    .map(p -> p.first().toString())
                    .collect(Collectors.joining(", "));
        }
        return "Φ(" + repr + ")";
    }
}
