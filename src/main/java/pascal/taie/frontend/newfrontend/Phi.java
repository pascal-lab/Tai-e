package pascal.taie.frontend.newfrontend;

import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.ExpVisitor;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.type.Type;

import java.util.List;
import java.util.Set;

class Phi implements Exp {

    private final List<StackItem> nodes;
    private Var var;
    private final int height;
    boolean used;
    int nodeSize;
    BytecodeBlock createPos;

    Phi(int i, List<StackItem> exps, BytecodeBlock block) {
        this.nodes = exps;
        this.height = i;
        this.createPos = block;
        used = false;
        nodeSize = nodes.size();
    }

    void addNodes(StackItem n) {
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

    List<StackItem> getNodes() {
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
