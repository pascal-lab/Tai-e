package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

public final class BytecodeBlock {
    private final LabelNode start;
    private final List<AbstractInsnNode> instr;
    private final List<BytecodeBlock> inEdges;
    private final List<BytecodeBlock> outEdges;

    private Stack<Var> inStack;

    private Stack<Var> outStack;

    private @Nullable Stmt firstStmt;

    @Nullable
    private BytecodeBlock fallThrough;

    private boolean complete;

    private final boolean isCatch;

    public BytecodeBlock(LabelNode start,  @Nullable BytecodeBlock fallThrough) {
        this(start, fallThrough, false);
    }

    public BytecodeBlock(LabelNode start, @Nullable BytecodeBlock fallThrough, boolean isCatch) {
        this.start = start;
        this.instr = new ArrayList<>();
        this.inEdges = new ArrayList<>();
        this.outEdges = new ArrayList<>();
        this.fallThrough = fallThrough;
        this.complete = false;
        this.isCatch = isCatch;
    }

    public LabelNode start() {
        return start;
    }

    public List<AbstractInsnNode> instr() {
        return instr;
    }

    public List<BytecodeBlock> inEdges() {
        return inEdges;
    }

    public List<BytecodeBlock> outEdges() {
        return outEdges;
    }

    @Nullable
    public BytecodeBlock fallThrough() {
        return fallThrough;
    }

    public void setFallThrough(BytecodeBlock fallThrough) {
        this.fallThrough = fallThrough;
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean isCatch() {
        return isCatch;
    }

    public void setComplete() {
        complete = true;
    }

    public Stack<Var> getInStack() {
        return inStack;
    }

    public Stack<Var> getOutStack() {
        return outStack;
    }

    public void setInStack(Stack<Var> inStack) {
        assert this.inStack == null : "InStack should not be assigned multiple times.";
        this.inStack = inStack;
        for (var pred : inEdges) {
            if (pred.outStack == null) {
                pred.setOutStack(inStack);
            }
        }
    }

    public void setOutStack(Stack<Var> outStack) {
        assert this.outStack == null : "OutStack should not be assigned multiple times.";
        this.outStack = outStack;
        for (var succ : outEdges) {
            if (succ.inStack == null) {
                succ.setInStack(outStack);
            }
        }
    }

    public Stmt getFirstStmt() {
        return firstStmt;
    }

    public AbstractInsnNode getLastBytecode() {
        return instr.get(instr.size() - 1);
    }

    public void setFirstStmt(Stmt firstStmt) {
        this.firstStmt = firstStmt;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BytecodeBlock) obj;
        return Objects.equals(this.start, that.start) &&
                Objects.equals(this.instr, that.instr) &&
                Objects.equals(this.inEdges, that.inEdges) &&
                Objects.equals(this.outEdges, that.outEdges) &&
                Objects.equals(this.fallThrough, that.fallThrough);
    }

}
