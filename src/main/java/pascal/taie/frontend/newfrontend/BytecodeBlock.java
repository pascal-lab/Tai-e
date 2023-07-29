package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.VarInsnNode;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

public final class BytecodeBlock {
    private final LabelNode label;

    private final List<AbstractInsnNode> instr;
    private final List<BytecodeBlock> inEdges;
    private final List<BytecodeBlock> outEdges;

    private Stack<Exp> inStack;

    private Stack<Exp> outStack;

    private List<Stmt> stmts;

    @Nullable
    private BytecodeBlock fallThrough;

    @Nullable
    private FrameNode frame;

    private boolean complete;

    @Nullable
    private final Type exceptionHandlerType;

    private List<Object> frameLocalType;

    private List<Integer> stmt2Asm;

    private boolean isInTry = false;

    public BytecodeBlock(LabelNode label, @Nullable BytecodeBlock fallThrough) {
        this(label, fallThrough, null);
    }

    public BytecodeBlock(LabelNode label, @Nullable BytecodeBlock fallThrough, @Nullable Type exceptionHandlerType) {
        this.label = label;
        this.instr = new ArrayList<>();
        this.inEdges = new ArrayList<>();
        this.outEdges = new ArrayList<>();
        this.stmts = new ArrayList<>();
        this.fallThrough = fallThrough;
        this.complete = false;
        this.exceptionHandlerType = exceptionHandlerType;
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
        return getExceptionHandlerType() != null;
    }

    public void setIsInTry() {
        isInTry = true;
    }

    public boolean isInTry() {
        return isInTry;
    }

    public void setComplete() {
        complete = true;
    }

    public Stack<Exp> getInStack() {
        return inStack;
    }

    public Stack<Exp> getOutStack() {
        return outStack;
    }

    public void setInStack(Stack<Exp> inStack) {
        assert this.inStack == null : "InStack should not be assigned multiple times.";
        assert frame == null ||
                inStack.stream().filter(i -> i instanceof Var).count() == frame.stack.size();
        this.inStack = inStack;
        for (var pred : inEdges) {
            if (pred.outStack == null) {
                pred.setOutStack(inStack);
            }
        }
    }

    public void setOutStack(Stack<Exp> outStack) {
        assert this.outStack == null : "OutStack should not be assigned multiple times.";
        this.outStack = outStack;
        for (var succ : outEdges) {
            if (succ.inStack == null) {
                succ.setInStack(outStack);
            }
        }
    }

    public AbstractInsnNode getLastBytecode() {
        return instr.get(instr.size() - 1);
    }

    public Stmt getLastStmt() {
        assert !stmts.isEmpty();
        return stmts.get(stmts.size() - 1);
    }

    public List<Stmt> getStmts() {
        return stmts;
    }

    public void setStmts(List<Stmt> stmts) {
        this.stmts = stmts;
    }

    @Nullable
    public FrameNode getFrame() {
        return frame;
    }

    public Map<Var, Type> getInitTyping() {
        assert frame != null;

        Map<Var, Type> typing = Maps.newMap();

        if (inStack != null) {
            int n = 0;
            for (int i = 0; i < frame.stack.size(); ++i) {
                Exp e = inStack.get(n);
                Var v;
                if (e instanceof Top) {
                    n++;
                    v = (Var) inStack.get(n);
                } else {
                    v = (Var) e;
                }
                typing.put(v, Utils.fromAsmFrameType(frame.stack.get(i)));
                n++;
            }
        } else {
            assert inEdges.isEmpty();
        }
        return typing;
    }

    private void buildFrameLocalType() {
        assert frame != null;
        frameLocalType = new ArrayList<>(frame.local.size() + 1);
        int n = 0;
        for (Object o : frame.local) {
            frameLocalType.add(o);
            // is long or double
            if (o instanceof Integer i && (i == 3 || i == 4)) {
                frameLocalType.add(0); // place top
                n += 2;
            } else {
                n += 1;
            }
        }
        tryCorrectFrame(n);
    }

    private void ensureLocalType() {
        if (frameLocalType == null) {
            buildFrameLocalType();
        }
    }

    public boolean isLocalExistInFrame(int slot) {
        ensureLocalType();
        if (slot >= frameLocalType.size()) {
            return false;
        }
        Object o = frameLocalType.get(slot);
        return ! (o instanceof Integer i && i == 0);
    }

    public List<Object> getFrameLocalType() {
        ensureLocalType();
        return frameLocalType;
    }

    public void setFrame(FrameNode frame) {
        assert frame != null;
        this.frame = frame;
    }

    void setStmt2Asm(List<Integer> stmt2Asm) {
        this.stmt2Asm = stmt2Asm;
    }

    AbstractInsnNode getOrig(int index) {
        return instr.get(stmt2Asm.get(index));
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

    @Nullable
    public Type getExceptionHandlerType() {
        return exceptionHandlerType;
    }


    private void tryCorrectFrame(int size) {
        if (instr.isEmpty()) {
            return;
        }
        AbstractInsnNode last = instr.get(instr.size() - 1);
        if (!Utils.isReturn(last) || isInTry) {
            return;
        }
        // the last node is return

        boolean[] hits = new boolean[size];
        Arrays.fill(hits, false);
        boolean[] redefines = new boolean[size];
        Arrays.fill(redefines, false);

        for (AbstractInsnNode insnNode: instr) {
            if (insnNode instanceof VarInsnNode varInsnNode) {
                int var = varInsnNode.var;
                if (var >= size) {
                    continue;
                }
                if (Utils.isVarStore(varInsnNode)) {
                    redefines[var] = true;
                } else {
                    if (!redefines[var]) {
                        hits[var] = true;
                    }
                }
            } else if (insnNode instanceof IincInsnNode iincInsnNode) {
                int var = iincInsnNode.var;
                if (var >= size) {
                    continue;
                }
                redefines[var] = true;
                hits[var] = true;
            }
        }

        for (int i = 0; i < frameLocalType.size(); ++i) {
            if (!hits[i]) {
                frameLocalType.set(i, 0);
            }
        }
    }
}
