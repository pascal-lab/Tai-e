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

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.objectweb.asm.tree.AbstractInsnNode;

import pascal.taie.frontend.java.ir.ssa.FrontendPhiExp;
import pascal.taie.frontend.java.ir.ssa.FrontendPhiStmt;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.LazyArray;

/**
 * Resolve {@link StackPhi}s into {@link FrontendPhiStmt}s (SSA mode) or explicit assignments (non-SSA mode).
 */
class StackPhiResolver {

    /**
     * The shared context holding all resources and state for the IR building process.
     */
    private final IRBuilderContext context;

    StackPhiResolver(IRBuilderContext context) {
        this.context = context;
    }

    /**
     * Orchestrates the resolution process: computes loop header inputs, propagates usage status, and emits IR statements.
     */
    void resolveStackPhis() {
        List<StackPhi> stackPhiList = context.operandStack.getStackPhiList();
        for (BytecodeBlock block : context.cfg) {
            addInExpsOfLoopHeaderStackPhis(block);
        }
        propagateStackPhiUsed(stackPhiList);
        resolveStackPhis2Stmts(stackPhiList);
    }

    /**
     * Propagates the usage status transitively.
     */
    private void propagateStackPhiUsed(List<StackPhi> stackPhiList) {
        for (StackPhi phi : stackPhiList) {
            if (phi.used) {
                propagate2InPhis(phi);
            }
        }
    }

    private void propagate2InPhis(StackPhi phi) {
        for (StackItem item : phi.getInExps()) {
            Exp e = item.exp();
            if (e instanceof StackPhi inPhi) {
                if (!inPhi.used) {
                    inPhi.used = true;
                    propagate2InPhis(inPhi);
                }
            }
        }
    }

    /**
     * Resolve {@link StackPhi} into concrete IR statements.
     */
    private void resolveStackPhis2Stmts(List<StackPhi> stackPhiList) {
        if (!context.isSSA) {
            LazyArray<List<Stmt>> preMergeAssigns = new LazyArray<>(context.cfg.nodeCount()) {
                @Override
                protected List<Stmt> createElement() {
                    return new ArrayList<>();
                }
            };
            for (StackPhi phi : stackPhiList) {
                computeWriteOutVar(phi);
            }
            for (StackPhi phi : stackPhiList) {
                resolve2Assigns(phi, preMergeAssigns);
            }
            for (BytecodeBlock block : context.cfg) {
                // unreachable block
                if (block.getOutStack() == null) {
                    continue;
                }
                if (preMergeAssigns.contains(block.getIndex())) {
                    List<Stmt> stmts = preMergeAssigns.get(block.getIndex());
                    context.stmtManager.appendStmts(block, stmts);
                }
            }
        } else {
            // emit phi stmts for stack variable
            for (StackPhi phi : stackPhiList) {
                resolve2FrontendPhi(phi);
            }
        }
    }

    private void resolve2FrontendPhi(StackPhi phi) {
        BytecodeBlock block = phi.createPos;
        // insert phi node in the first instruction
        FrontendPhiExp phiExp = new FrontendPhiExp();
        int unreachableOffset = 0;
        for (int i = 0; i < context.cfg.getNormalInDegreeOf(block); ++i) {
            if (context.cfg.getNormalPredOf(block, i).getOutStack() == null) {
                unreachableOffset++;
                continue;
            }
            StackItem item = phi.getInExps().get(i - unreachableOffset);
            context.operandStack.liftToVar(item);
            phiExp.addUseAndCorrespondingBlocks(item.var(), context.cfg.getNormalPredOf(block, i));
        }
        FrontendPhiStmt frontendPhiStmt = new FrontendPhiStmt(phi.getVar(), phi.getVar(), phiExp);
        phi.setWriteOutVar(phi.getVar());
        addToBlockHead(block, frontendPhiStmt);
        phi.resolved = true;
    }

    private void computeWriteOutVar(StackPhi phi) {
        if (phi.getWriteOutVar() != null) {
            return;
        }
        BytecodeBlock block = phi.createPos;
        boolean useWorseSolution = block.isLoopHeader() && phi.used;
        Var writeOut = useWorseSolution ? context.varManager.getTempVar() : phi.getVar();
        context.varManager.setNonSSA(writeOut);
        if (useWorseSolution) {
            // add `v = writeOut` before any definition (first instruction) in create pos
            BytecodeBlock createPos = phi.createPos;
            LValue lValue = phi.getVar();
            addToBlockHead(createPos, Utils.newAssignStmt(context.method, lValue, writeOut));
        }
        phi.setWriteOutVar(writeOut);
    }

    private void resolve2Assigns(StackPhi phi, LazyArray<List<Stmt>> preMergeAssigns) {
        if (phi.getWriteOutVar() == null || phi.resolved) {
            return;
        }
        int unreachableOffset = 0;
        Var writeOut = phi.getWriteOutVar();
        for (int i = 0; i < phi.getInExps().size(); ++i) {
            StackItem item = phi.getInExps().get(i);
            BytecodeBlock inBlock = context.cfg.getNormalPredOf(phi.createPos, i + unreachableOffset);
            if (inBlock.getOutStack() == null) {
                unreachableOffset++;
                continue;
            }
            List<Stmt> stmts = preMergeAssigns.get(inBlock.getIndex());
            Exp e = item.exp();
            if (e instanceof StackPhi inPhi) {
                e = inPhi.getVar();
            }
            if (e == writeOut) {
                continue;
            }
            if (Utils.mayHaveSideEffect(e) || phi.used) {
                stmts.add(Utils.newAssignStmt(context.method, writeOut, e));
            }
        }
        phi.resolved = true;
    }

    private void addInExpsOfLoopHeaderStackPhis(BytecodeBlock current) {
        if (current.isLoopHeader()) {
            Stack<StackItem> inStack = current.getInStack();
            for (int i = 0; i < context.cfg.getNormalInDegreeOf(current); ++i) {
                BytecodeBlock outBlock = context.cfg.getNormalPredOf(current, i);
                if (outBlock.getOutStack() == null) {
                    assert outBlock.getInStack() == null;
                    continue;
                }
                for (int j = 0; j < outBlock.getOutStack().size(); ++j) {
                    Exp currentExp = inStack.get(j).originalExp();
                    if (currentExp instanceof Top) {
                        continue;
                    }
                    StackPhi phi = (StackPhi) currentExp;
                    StackItem item = outBlock.getOutStack().get(j);
                    phi.getInExps().add(item);
                }
            }
        }
    }

    private void addToBlockHead(BytecodeBlock block, Stmt stmt) {
        AbstractInsnNode firstInsn = block.getInsns().get(0);
        context.stmtManager.associateStmt(firstInsn, stmt);
    }
}
