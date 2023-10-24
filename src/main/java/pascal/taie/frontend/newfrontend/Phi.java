package pascal.taie.frontend.newfrontend;

import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.ExpVisitor;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.type.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class Phi implements Exp {

    private final List<Exp> nodes;
    private Var var;
    private final int height;
    boolean used;
    int nodeSize;
    BytecodeBlock createPos;

    Phi(int i, List<Exp> exps, BytecodeBlock block) {
        this.nodes = exps;
        this.height = i;
        this.createPos = block;
        used = false;
        nodeSize = nodes.size();
    }

    Phi(int i, Exp first, BytecodeBlock block) {
        height = i;
        nodes = new ArrayList<>();
        var = null;
        addNodes(first);
        used = false;
        createPos = block;
        nodeSize = 0;
    }

    void addNodes(Exp n) {
        if (!nodes.contains(n)) {
            this.nodes.add(n);
        }
        nodeSize++;
    }

    void setVar(Var var) {
        this.var = var;
    }

    void setUsed() {
        this.used = true;
    }

    Var getVar() {
        return this.var;
    }

    List<Exp> getNodes() {
        return nodes;
    }

    int getHeight() {
        return height;
    }

    @Override
    public Type getType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<RValue> getUses() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        throw new UnsupportedOperationException();
    }
}
