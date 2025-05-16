/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.frontend.newfrontend.bcir;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.VarInsnNode;
import pascal.taie.frontend.newfrontend.IBasicBlock;
import pascal.taie.frontend.newfrontend.TOP;
import pascal.taie.frontend.newfrontend.Utils;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.function.BiConsumer;

/**
 * The basic block of bytecode control flow graph.
 */
public final class BytecodeBlock implements IBasicBlock {
    private final LabelNode label;

    private int index = -1;

    private BytecodeListSlice instr;

    private Stack<StackItem> inStack;

    private Stack<StackItem> outStack;

    private List<Stmt> stmts;

    private FrameNode frame;

    @Nullable
    private List<ClassType> exceptionHandlerTypes;

    private List<Object> frameLocalType;

    private int[] stmt2Asm;

    private boolean isInTry = false;

    private boolean isLoopHeader = false;

    public BytecodeBlock(LabelNode label) {
        this.label = label;
        this.stmts = new ArrayList<>();
    }

    public BytecodeListSlice instr() {
        return instr;
    }

    public void setInstr(BytecodeListSlice instr) {
        this.instr = instr;
    }

    public boolean isCatch() {
        return getExceptionHandlerTypes() != null;
    }

    public void setIsInTry() {
        isInTry = true;
    }

    public boolean isInTry() {
        return isInTry;
    }

    public Stack<StackItem> getInStack() {
        return inStack;
    }

    public Stack<StackItem> getOutStack() {
        return outStack;
    }

    public void setInStack(Stack<StackItem> inStack) {
        assert this.inStack == null : "InStack should not be assigned multiple times.";
        this.inStack = inStack;
    }

    public void setOutStack(Stack<StackItem> outStack) {
        assert this.outStack == null : "OutStack should not be assigned multiple times.";
        this.outStack = outStack;
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

    @Override
    public void setStmt(Stmt stmt, int pos) {
        stmts.set(pos, stmt);
    }

    @Override
    public void insertStmts(List<Stmt> stmts) {
        // if this block is a catch block,
        // then we should insert the stmts after the first catch stmt
        List<Stmt> temp = new ArrayList<>(stmts.size() + this.stmts.size());
        int i = 0;
        if (isCatch()) {
            // if this block is a catch block, then the first stmt must be a catch stmt
            assert this.stmts.get(0) instanceof Catch;
            temp.add(this.stmts.get(0));
            temp.addAll(stmts);
            i++;
        } else {
            temp.addAll(stmts);
        }
        for (; i < this.stmts.size(); ++i) {
            temp.add(this.stmts.get(i));
        }
        this.stmts = temp;
    }

    @Override
    public void setStmts(List<Stmt> stmts) {
        this.stmts = stmts;
    }

    @Nullable
    public FrameNode getFrame() {
        return frame;
    }

    public void visitInitTyping(BiConsumer<Var, Type> consumer) {
        assert frame != null;

        if (inStack != null) {
            int n = 0;
            for (int i = 0; i < frame.stack.size(); ++i) {
                Exp e = inStack.get(n).e();
                Exp original = inStack.get(n).originalExp();
                Var v;
                if (e instanceof TOP) {
                    n++;
                    e = inStack.get(n).e();
                }

                if (original instanceof StackPhi phi) {
                    v = phi.getWriteOutVar();
                } else if (e instanceof Var v1) {
                    v = v1;
                } else {
                    v = null;
                }
                if (v != null) {
                    Type t = Utils.fromAsmFrameType(frame.stack.get(i));
                    consumer.accept(v, t);
                }
                n++;
            }
        }
    }

    private void buildFrameLocalType() {
        frameLocalType = frame.local;
        boolean copied = false;
        for (int i = 0; i < frame.local.size(); i++) {
            Object o = frame.local.get(i);
            // is long or double
            if (o instanceof Integer && ((Integer) o == 3 || (Integer) o == 4)) {
                if (!copied) {
                    frameLocalType = new ArrayList<>(frame.local.size() + 1);
                    for (int j = 0; j < i; j++) {
                        frameLocalType.add(frame.local.get(j));
                    }
                    copied = true;
                }
                frameLocalType.add(o);
                frameLocalType.add(0); // place top
            } else if (copied) {
                frameLocalType.add(o);
            }
        }
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

    void setStmt2Asm(int[] stmt2Asm) {
        this.stmt2Asm = stmt2Asm;
    }

    public AbstractInsnNode getOrig(int index) {
        return instr.get(stmt2Asm[index]);
    }

    public void setIndex(int i) {
        this.index = i;
    }

    public int getIndex() {
        return this.index;
    }

    @Nullable
    public List<ClassType> getExceptionHandlerTypes() {
        return exceptionHandlerTypes;
    }

    public void addExceptionHandlerType(ClassType type) {
        if (exceptionHandlerTypes == null) {
            exceptionHandlerTypes = new ArrayList<>();
        }
        exceptionHandlerTypes.add(type);
    }

    @Deprecated
    private void tryCorrectFrame(int size) {
        if (instr.isEmpty()) {
            return;
        }
        AbstractInsnNode last = instr.get(instr.size() - 1);
        if (!(Utils.isReturn(last) || Utils.isThrow(last)) || isInTry) {
            return;
        }
        // the last node is return

        boolean[] hits = new boolean[size];
        Arrays.fill(hits, false);
        boolean[] redefines = new boolean[size];
        Arrays.fill(redefines, false);

        for (AbstractInsnNode insnNode : instr) {
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

    public boolean isLoopHeader() {
        return isLoopHeader;
    }

    public void setLoopHeader(boolean loopHeader) {
        isLoopHeader = loopHeader;
    }
}
