package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LabelNode;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;

public final class BytecodeBlock {
    private final LabelNode label;

    private final List<AbstractInsnNode> instr;
    private final List<BytecodeBlock> inEdges;
    private final List<BytecodeBlock> outEdges;

    private Stack<Var> inStack;

    private Stack<Var> outStack;

    private Stmt firstStmt;

    /**
     * <code>null</code> if this block is the last bytecode block in a method
     * or this block is empty
     */
    @Nullable
    private Stmt lastStmt;

    @Nullable
    private BytecodeBlock fallThrough;

    @Nullable
    private FrameNode frame;

    private AbstractInsnNode firstBytecode;

    private boolean complete;

    private final boolean isCatch;

    public BytecodeBlock(LabelNode label, @Nullable BytecodeBlock fallThrough) {
        this(label, fallThrough, false);
    }

    public BytecodeBlock(LabelNode label, @Nullable BytecodeBlock fallThrough, boolean isCatch) {
        this.label = label;
        this.instr = new ArrayList<>();
        this.inEdges = new ArrayList<>();
        this.outEdges = new ArrayList<>();
        this.fallThrough = fallThrough;
        this.complete = false;
        this.isCatch = isCatch;
    }

    public LabelNode label() {
        return label;
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
        if (frame == null) {
            assert inStack.isEmpty() || inEdges.size() == 1;
        } else {
            assert inStack.size() == frame.stack.size();
        }
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

    public Optional<AbstractInsnNode> getFirstBytecode() {
        if (instr().size() != 0) {
            if (firstBytecode == null) {
                firstBytecode = instr.get(0);
            }
            return Optional.of(firstBytecode);
        } else {
            return Optional.empty();
        }
    }

    public AbstractInsnNode getLastBytecode() {
        return instr.get(instr.size() - 1);
    }

    public void setFirstStmt(Stmt firstStmt) {
        this.firstStmt = firstStmt;
    }

    public Stmt getLastStmt() {
        return lastStmt;
    }

    public void setLastStmt(Stmt lastStmt) {
        this.lastStmt = lastStmt;
    }

    @Nullable
    public FrameNode getFrame() {
        return frame;
    }

    public void setFrame(@Nullable FrameNode frame) {
        this.frame = frame;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BytecodeBlock) obj;
        return Objects.equals(this.label, that.label) &&
                Objects.equals(this.instr, that.instr) &&
                Objects.equals(this.inEdges, that.inEdges) &&
                Objects.equals(this.outEdges, that.outEdges) &&
                Objects.equals(this.fallThrough, that.fallThrough);
    }

}
