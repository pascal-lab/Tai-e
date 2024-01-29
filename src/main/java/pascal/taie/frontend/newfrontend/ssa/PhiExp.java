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

    private List<Pair<Var, IBasicBlock>> usesAndInBlocks = new ArrayList<>();

    private List<Pair<Integer, Var>> sourceAndVar;

    public static final int METHOD_ENTRY = -1;

    public void addUseAndCorrespondingBlocks(Var v, IBasicBlock block) {
        usesAndInBlocks.add(new Pair<>(v, block));
    }

    List<Pair<Var, IBasicBlock>> getUsesAndInBlocks() {
        return usesAndInBlocks;
    }

    public void indexValueAndSource(PhiResolver<? extends IBasicBlock> resolver) {
        sourceAndVar = resolver.resolvePhi(this);
        usesAndInBlocks = null;
    }

    public List<Pair<Integer, Var>> getSourceAndVar() {
        return sourceAndVar;
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

    public Var findVar(IBasicBlock block) {
        for (Pair<Var, IBasicBlock> pair : usesAndInBlocks) {
            if (pair.second() == block) {
                return pair.first();
            }
        }
        throw new IllegalArgumentException("No such block");
    }
}
