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
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.LazyArray;

class StackPhiResolver {
    // --- Dependencies ---
    private final JMethod method;
    private final boolean isSSA;
    private final OperandStack operandStack;
    private final SlotManager slotManager;
    private final BytecodeCFG cfg;
    private final StmtManager stmtManager;
    private final VarManager varManager;

    // --- Internal State ---
    private LazyArray<List<Stmt>> stackMergeStmts;

    public StackPhiResolver(JMethod method, boolean isSSA,
                            OperandStack operandStack, SlotManager slotManager,
                            BytecodeCFG cfg, StmtManager stmtManager, VarManager varManager) {
        this.method = method;
        this.isSSA = isSSA;
        this.operandStack = operandStack;
        this.slotManager = slotManager;
        this.cfg = cfg;
        this.stmtManager = stmtManager;
        this.varManager = varManager;
    }

    void solveAllPhi() {
        List<StackPhi> stackPhiList = operandStack.getStackPhiList();
        for (BytecodeBlock block : cfg) {
            fillInLoopHeaderStackPhis(block);
            if (isSSA) {
                slotManager.addInDefsForSlotPhis(block);
            }
        }
        propagatePhiUsed(stackPhiList);
        resolveStackPhi(stackPhiList);
        for (BytecodeBlock block : cfg) {
            // unreachable
            if (block.getOutStack() == null) {
                continue;
            }
            if (!isSSA) {
                if (stackMergeStmts.contains(block.getIndex())) {
                    List<Stmt> stmts = stackMergeStmts.get(block.getIndex());
                    stmtManager.appendStackMergeStmts(block, stmts);
                }
            }
        }
    }

    private void propagatePhiUsed(List<StackPhi> stackPhiList) {
        for (StackPhi phi : stackPhiList) {
            if (phi.used) {
                setStackPhiUsed(phi);
            }
        }
    }

    private void setStackPhiUsed(StackPhi phi) {
        for (StackItem item : phi.getInExps()) {
            Exp e = item.exp();
            if (e instanceof StackPhi phi1) {
                if (!phi1.used) {
                    phi1.used = true;
                    setStackPhiUsed(phi1);
                }
            }
        }
    }

    private void resolveStackPhi(List<StackPhi> stackPhiList) {
        if (!isSSA) {
            stackMergeStmts = new LazyArray<>(cfg.nodeCount()) {
                @Override
                protected List<Stmt> createElement() {
                    return new ArrayList<>();
                }
            };
            for (StackPhi phi : stackPhiList) {
                if (phi.getWriteOutVar() != null) {
                    continue;
                }
                BytecodeBlock block = phi.createPos;
                boolean useWorseSolution = block.isLoopHeader() && phi.used;
                Var writeOut = useWorseSolution ? varManager.getTempVar() : phi.getVar();
                varManager.setNonSSA(writeOut);
                if (useWorseSolution) {
                    // add `v = writeOut` before any definition (first instruction) in create pos
                    BytecodeBlock createPos = phi.createPos;
                    LValue lValue = phi.getVar();
                    addToBlockHead(createPos, Utils.newAssignStmt(method, lValue, writeOut));
                }
                phi.setWriteOutVar(writeOut);
            }
            for (StackPhi phi : stackPhiList) {
                if (phi.getWriteOutVar() == null || phi.resolved) {
                    continue;
                }
                resolveStackPhi(phi);
            }
        } else {
            // emit phi stmts for stack variable
            for (StackPhi phi : stackPhiList) {
                BytecodeBlock block = phi.createPos;
                // insert phi node in the first instruction
                FrontendPhiExp phiExp = new FrontendPhiExp();
                int unreachableOffset = 0;
                for (int i = 0; i < cfg.getNormalInDegreeOf(block); ++i) {
                    if (cfg.getNormalPredOf(block, i).getOutStack() == null) {
                        unreachableOffset++;
                        continue;
                    }
                    StackItem item = phi.getInExps().get(i - unreachableOffset);
                    operandStack.liftToVar(item);
                    phiExp.addUseAndCorrespondingBlocks(item.var(), cfg.getNormalPredOf(block, i));
                }
                FrontendPhiStmt frontendPhiStmt = new FrontendPhiStmt(phi.getVar(), phi.getVar(), phiExp);
                phi.setWriteOutVar(phi.getVar());
                addToBlockHead(block, frontendPhiStmt);
                phi.resolved = true;
            }
        }
    }

    private void addToBlockHead(BytecodeBlock block, Stmt stmt) {
        AbstractInsnNode firstInsn = block.getInsns().get(0);
        stmtManager.associateStmt(firstInsn, stmt);
    }

    private void resolveStackPhi(StackPhi phi) {
        assert !phi.resolved;
        int unreachableOffset = 0;
        Var writeOut = phi.getWriteOutVar();
        for (int i = 0; i < phi.getInExps().size(); ++i) {
            StackItem item = phi.getInExps().get(i);
            BytecodeBlock inBlock = cfg.getNormalPredOf(phi.createPos, i + unreachableOffset);
            if (inBlock.getOutStack() == null) {
                unreachableOffset++;
                continue;
            }
            List<Stmt> stmts = stackMergeStmts.get(inBlock.getIndex());
            Exp e = item.exp();
            if (e instanceof StackPhi inPhi) {
                e = inPhi.getVar();
            }
            if (e == writeOut) {
                continue;
            }
            if (Utils.mayHaveSideEffect(e) || phi.used) {
                stmts.add(Utils.newAssignStmt(method, writeOut, e));
            }
        }
        phi.resolved = true;
    }

    private void fillInLoopHeaderStackPhis(BytecodeBlock current) {
        if (current.isLoopHeader()) {
            Stack<StackItem> inStack = current.getInStack();
            for (int i = 0; i < cfg.getNormalInDegreeOf(current); ++i) {
                BytecodeBlock outBlock = cfg.getNormalPredOf(current, i);
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
}
