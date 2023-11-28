package pascal.taie.frontend.newfrontend.ssa;

import pascal.taie.frontend.newfrontend.IBasicBlock;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.ExpVisitor;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PhiExp implements Exp, RValue {

    private final List<Pair<Var, IBasicBlock>> usesAndInBlocks = new ArrayList<>();

    public void addUseAndCorrespondingBlocks(Var v, IBasicBlock block) {
        usesAndInBlocks.add(new Pair<>(v, block));
    }

    @Override
    public Type getType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<RValue> getUses() {
        return usesAndInBlocks
                .stream()
                .map(Pair::first)
                .collect(Collectors.toSet());
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "Φ(" +
                usesAndInBlocks.stream()
                        .map(p -> p.first().toString())
                        .collect(Collectors.joining(","))
                + ")";
    }
}
