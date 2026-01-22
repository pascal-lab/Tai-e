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

import org.objectweb.asm.tree.AbstractInsnNode;

import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Nop;
import pascal.taie.ir.stmt.Stmt;


import static pascal.taie.frontend.java.ir.Utils.isCFEdge;

/**
 * Manages the mapping from bytecode instructions to their generated IR statements.
 * It also tracks source line numbers and assembles the final ordered list of statements for each basic block.
 */
final class StmtManager {

    /**
     * A mapping from bytecode instruction index (use {@link IRBuilderContext#getInsnIndex} to obtain) to generated Tai-e IR stmt
     */
    private final Stmt[] insn2Stmt;

    /**
     * Similar to {@link StmtManager#insn2Stmt}, when a bytecode instruction generate more than
     * one Tai-e IR stmt, use this mapping to store the rest
     */
    private final List<List<Stmt>> additionalStmts;

    /**
     * A <i>mutable</i> field that record current line number of visited bytecode
     */
    private int currentLineNumber;

    /**
     * The shared context holding all resources and state for the IR building process.
     */
    private final IRBuilderContext context;

    StmtManager(IRBuilderContext context) {
        this.context = context;

        int insnCount = context.source.instructions.size();
        this.insn2Stmt = new Stmt[insnCount];
        this.additionalStmts = new ArrayList<>(insnCount);
        for (int i = 0; i < insnCount; ++i) {
            additionalStmts.add(null);
        }
    }

    /**
     * Set line number.
     */
    void setLineNumber(int lineNumber) {
        currentLineNumber = lineNumber;
    }

    /**
     * Get line number.
     */
    int getLineNumber() {
        return currentLineNumber;
    }

    /**
     * Associates a generated Stmt with its source bytecode instruction.
     */
    void associateStmt(AbstractInsnNode insn, Stmt stmt) {
        // TODO: remove this checking
        if (stmt.getLineNumber() == -1) {
            stmt.setLineNumber(currentLineNumber);
        }
        int idx = context.getInsnIndex(insn);
        if (insn2Stmt[idx] == null) {
            insn2Stmt[idx] = stmt;
        } else {
            associateAdditionalStmt(insn, stmt);
        }
    }

    /**
     * Associates a generated Stmt as an additional statement for a source bytecode instruction.
     */
    private void associateAdditionalStmt(AbstractInsnNode insn, Stmt stmt) {
        int idx = context.getInsnIndex(insn);
        List<Stmt> additional = additionalStmts.get(idx);
        if (additional == null) {
            additional = new ArrayList<>();
            additionalStmts.set(idx, additional);
        }
        additional.add(stmt);
    }

    /**
     * Appends a list of statements to the end of the specified bytecode block.
     * If the block ends with a control-flow instruction, the new statements
     * are inserted before it.
     */
    void appendStmts(BytecodeBlock block, List<Stmt> stmts) {
        if (!stmts.isEmpty()) {
            AbstractInsnNode lastInsn = block.getLastInsn();
            if (isCFEdge(lastInsn)) {
                // last stmt may attach goto, if, switch ...
                List<Stmt> originalStmts = clearStmt(lastInsn);
                for (int i = 0; i < originalStmts.size() - 1; ++i) {
                    associateStmt(lastInsn, originalStmts.get(i));
                }
                stmts.forEach(stmt -> associateStmt(lastInsn, stmt));
                associateStmt(lastInsn, originalStmts.get(originalStmts.size() - 1));
            } else {
                stmts.forEach(stmt -> associateStmt(lastInsn, stmt));
            }
        }
    }

    void ensureBlockNotEmpty(BytecodeBlock block) {
        boolean blockEmpty = true;
        InsnListSlice insns = block.getInsns();
        int start = insns.getStart();
        for (int i = 0; i < insns.size(); ++i) {
            int current = start + i;
            Stmt stmt = insn2Stmt[current];
            if (stmt != null) {
                blockEmpty = false;
                break;
            }
        }
        if (blockEmpty) {
            insn2Stmt[start + insns.size() - 1] = new Nop();
        }
    }

    void buildBlockStmts(BytecodeBlock block) {
        List<Stmt> blockStmt = block.getStmts();
        InsnListSlice insns = block.getInsns();
        int start = insns.getStart();
        for (int i = 0; i < insns.size(); ++i) {
            int current = start + i;
            Stmt stmt = insn2Stmt[current];
            if (stmt != null) {
                blockStmt.add(stmt);
            }

            List<Stmt> stmts = additionalStmts.get(current);
            if (stmts != null) {
                blockStmt.addAll(stmts);
            }
        }
        if (block.isCatch() && context.isSSA) {
            // adjust order for phis, put catch in the front
            List<Stmt> stmts = new ArrayList<>();
            Catch catchStmt = null;
            for (Stmt stmt : blockStmt) {
                if (stmt instanceof Catch) {
                    assert catchStmt == null;
                    catchStmt = (Catch) stmt;
                } else {
                    stmts.add(stmt);
                }
            }
            assert catchStmt != null;
            stmts.add(0, catchStmt);
            blockStmt.clear();
            blockStmt.addAll(stmts);
        }
    }

    private List<Stmt> clearStmt(AbstractInsnNode insn) {
        List<Stmt> res = new ArrayList<>();
        int idx = context.getInsnIndex(insn);
        if (insn2Stmt[idx] != null) {
            res.add(insn2Stmt[idx]);
            insn2Stmt[idx] = null;
        }
        if (additionalStmts.get(idx) != null) {
            res.addAll(additionalStmts.get(idx));
            additionalStmts.set(idx, null);
        }
        return res;
    }
}
