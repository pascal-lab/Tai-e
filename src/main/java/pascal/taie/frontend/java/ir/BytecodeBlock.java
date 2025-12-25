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
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.type.ClassType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * The basic block of bytecode control flow graph.
 */
public final class BytecodeBlock {

    private final LabelNode label;

    private int index = -1;

    private InsnListSlice instr;

    private Stack<StackItem> inStack;

    private Stack<StackItem> outStack;

    private List<Stmt> stmts;

    private FrameNode frame;

    @Nullable
    private List<ClassType> exceptionHandlerTypes;

    private boolean isLoopHeader = false;

    BytecodeBlock(LabelNode label) {
        this.label = label;
        this.stmts = new ArrayList<>();
    }

    InsnListSlice instr() {
        return instr;
    }

    void setInstr(InsnListSlice instr) {
        this.instr = instr;
    }

    public boolean isCatch() {
        return getExceptionHandlerTypes() != null;
    }

    Stack<StackItem> getInStack() {
        return inStack;
    }

    void setInStack(Stack<StackItem> inStack) {
        assert this.inStack == null : "InStack should not be assigned multiple times.";
        this.inStack = inStack;
    }

    Stack<StackItem> getOutStack() {
        return outStack;
    }

    void setOutStack(Stack<StackItem> outStack) {
        assert this.outStack == null : "OutStack should not be assigned multiple times.";
        this.outStack = outStack;
    }

    AbstractInsnNode getLastBytecode() {
        return instr.get(instr.size() - 1);
    }

    public List<Stmt> getStmts() {
        return stmts;
    }

    @Nullable
    public Stmt getLastStmt() {
        return stmts.isEmpty() ? null : stmts.get(stmts.size() - 1);
    }

    public void setStmts(List<Stmt> stmts) {
        this.stmts = stmts;
    }

    @Nullable
    FrameNode getFrame() {
        return frame;
    }

    void setFrame(FrameNode frame) {
        assert frame != null;
        this.frame = frame;
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

    /**
     * @return {@code true} if this block is a loop header, {@code false} otherwise
     */
    boolean isLoopHeader() {
        return isLoopHeader;
    }

    /**
     * Marks this block as a loop header.
     */
    void setLoopHeader() {
        isLoopHeader = true;
    }
}
