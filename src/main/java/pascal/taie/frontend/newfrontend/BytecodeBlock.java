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
import java.util.Optional;
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

    private AbstractInsnNode firstBytecode;

    private boolean complete;

    @Nullable
    private final Type exceptionHandlerType;

    private Map<Integer, Type> frameLocalType;

    private Map<Integer, Var> frameLocalVar;

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
        if (frame != null) {
            buildFrameLocalType();
            tryCorrectFrame();
        }
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
        if (frame == null) {
            // assert inStack.isEmpty() || inEdges.size() == 1;
        } else {
            assert inStack.stream().filter(i -> i instanceof Var).count()
                    == frame.stack.size();
        }
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

    public Stmt getFirstStmt() {
        return stmts.get(0);
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

    public Map<Integer, Type> getFrameLocalType() {
        return this.frameLocalType;
    }

    public Type getFrameLocalType(int i) {
        return frameLocalType.get(i);
    }

    public void setFrameLocalVar(Map<Integer, Var> frameLocalVar) {
        if (frame == null) {
            return;
        }
        this.frameLocalVar = frameLocalVar;
    }

    public Map<Var, Type> getInitTyping() {
        assert frame != null;

        Map<Var, Type> typing = Maps.newMap();
        frameLocalVar.forEach((i, v) -> {
            Type t = frameLocalType.get(i);
            assert t != null;
            typing.put(v, t);
        });

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
            assert inEdges.size() == 0;
        }
        return typing;
    }

    private void buildFrameLocalType() {
        assert frame != null;
        frameLocalType = Maps.newMap();
        int n = 0;
        for (Object o : frame.local) {
            Type t = Utils.fromAsmFrameType(o);
            frameLocalType.put(n, t);
            if (t == PrimitiveType.DOUBLE || t == PrimitiveType.LONG) {
                n += 2;
            } else {
                n += 1;
            }
        }
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


    private void tryCorrectFrame() {
        if (instr.isEmpty()) {
            return;
        }
        AbstractInsnNode last = instr.get(instr.size() - 1);
        if (!Utils.isReturn(last)) {
            return;
        }
        // the last node is return
        int size = frameLocalType.size();
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
                redefines[var] = true;
                hits[var] = true;
            }
        }

        for (int i = 0; i < size; ++i) {
            if (!hits[i]) {
                frameLocalType.put(i, Top.Top);
            }
        }
    }
}
