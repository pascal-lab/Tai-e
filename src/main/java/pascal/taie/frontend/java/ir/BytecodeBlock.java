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

package pascal.taie.frontend.java.ir;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LabelNode;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.type.ClassType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

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

    private int[] stmt2Asm;

    private boolean isLoopHeader = false;

    BytecodeBlock(LabelNode label) {
        this.label = label;
        this.stmts = new ArrayList<>();
    }

    BytecodeListSlice instr() {
        return instr;
    }

    void setInstr(BytecodeListSlice instr) {
        this.instr = instr;
    }

    public boolean isCatch() {
        return getExceptionHandlerTypes() != null;
    }

    Stack<StackItem> getInStack() {
        return inStack;
    }

    Stack<StackItem> getOutStack() {
        return outStack;
    }

    void setInStack(Stack<StackItem> inStack) {
        assert this.inStack == null : "InStack should not be assigned multiple times.";
        this.inStack = inStack;
    }

    void setOutStack(Stack<StackItem> outStack) {
        assert this.outStack == null : "OutStack should not be assigned multiple times.";
        this.outStack = outStack;
    }

    AbstractInsnNode getLastBytecode() {
        return instr.get(instr.size() - 1);
    }

    Stmt getLastStmt() {
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

    void setFrame(FrameNode frame) {
        assert frame != null;
        this.frame = frame;
    }

    void setStmt2Asm(int[] stmt2Asm) {
        this.stmt2Asm = stmt2Asm;
    }

    public AbstractInsnNode getOrig(int index) {
        return instr.get(stmt2Asm[index]);
    }

    void setIndex(int i) {
        this.index = i;
    }

    public int getIndex() {
        return this.index;
    }

    @Nullable
    public List<ClassType> getExceptionHandlerTypes() {
        return exceptionHandlerTypes;
    }

    void addExceptionHandlerType(ClassType type) {
        if (exceptionHandlerTypes == null) {
            exceptionHandlerTypes = new ArrayList<>();
        }
        exceptionHandlerTypes.add(type);
    }

    boolean isLoopHeader() {
        return isLoopHeader;
    }

    void setLoopHeader(boolean loopHeader) {
        isLoopHeader = loopHeader;
    }
}
