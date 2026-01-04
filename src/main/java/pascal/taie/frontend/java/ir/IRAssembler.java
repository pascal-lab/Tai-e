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
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.frontend.java.ir.ssa.FrontendPhiStmt;
import pascal.taie.ir.DefaultIR;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.PhiExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Goto;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.PhiStmt;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.SwitchStmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;

/**
 * Assembles the final Tai-e IR.
 * It resolves jump targets, converts {@link FrontendPhiStmt} to the {@link PhiStmt}, and builds the exception table.
 */
class IRAssembler {
    private static final Logger logger = LogManager.getLogger(IRAssembler.class);

    // --- Dependencies ---
    private final JMethod method;
    private final JSRInlinerAdapter source;
    private final FrontendTypeSystem typeSystem;
    private final VarManager varManager;
    private final BytecodeCFG cfg;

    public IRAssembler(JMethod method, JSRInlinerAdapter source, FrontendTypeSystem typeSystem,
                       VarManager varManager, BytecodeCFG cfg) {
        this.method = method;
        this.source = source;
        this.typeSystem = typeSystem;
        this.varManager = varManager;
        this.cfg = cfg;
    }

    /**
     * Make statements, build exception tables, and construct the IR object.
     */
    IR assembleIR() {
        List<Stmt> stmts = makeStmts();
        List<ExceptionEntry> exceptionEntries = buildExceptionTable(stmts);
        Var thisVar = varManager.getThisVar();
        List<Var> params = varManager.getParams();
        List<Var> vars = varManager.getVars();
        Set<Var> retVars = varManager.getRetVars();
        return new DefaultIR(method, thisVar, params, retVars, vars, stmts, exceptionEntries);
    }

    /**
     * Collect all statements from blocks and var manager, resolve jumps along the way, and resolve phi statements.
     */
    private List<Stmt> makeStmts() {
        List<Stmt> stmts = new ArrayList<>(source.instructions.size());
        List<FrontendPhiStmt> frontendPhiStmts = new ArrayList<>();
        int now = 0;
        for (Var v : varManager.intConstVarCache) {
            if (v != null) {
                Stmt curr = Utils.newAssignStmt(method, v, v.getConstValue());
                curr.setIndex(now++);
                stmts.add(curr);
            }
        }
        for (BytecodeBlock block : cfg) {
            List<Stmt> blockStmts = block.getStmts();
            if (!blockStmts.isEmpty()) {
                for (Stmt t : blockStmts) {
                    if (true && t instanceof FrontendPhiStmt p) {
                        frontendPhiStmts.add(p);
                    }
                    t.setIndex(now++);
                    stmts.add(t);
                }
                setJumpTargets(block.getLastInsn(), block.getLastStmt());
            }
        }

        FrontendPhiResolver resolver = new FrontendPhiResolver(cfg);
        // Make PhiStmts using stmt.index as the value source.
        for (FrontendPhiStmt p : frontendPhiStmts) {
            int index = p.getIndex();
            Type type = p.getLValue().getType();
            PhiExp exp = new PhiExp(resolver.resolvePhi(p.getRValue()), type);
            Stmt phiStmt = new PhiStmt(p.getLValue(), exp);
            phiStmt.setIndex(index);
            phiStmt.setLineNumber(p.getLineNumber());
            stmts.set(index, phiStmt);
        }

        return stmts;
    }

    /**
     * Converts bytecode exception handlers (TryCatchBlockNode) into Tai-e's ExceptionEntry format.
     */
    private List<ExceptionEntry> buildExceptionTable(List<Stmt> stmts) {
        List<ExceptionEntry> exceptionEntries = new ArrayList<>();
        for (TryCatchBlockNode node : source.tryCatchBlocks) {
            Stmt start = getFirstStmt(node.start);
            Stmt end;
            if (node.end.getNext() == null) {
                // final bytecode
                end = stmts.get(stmts.size() - 1);
            } else {
                end = getFirstStmt(node.end);
            }
            assert start.getIndex() != -1;
            Stmt handler = getFirstStmt(node.handler);
            if (!(handler instanceof Catch)) {
                // unreachable
                continue;
            } else if (start == end) {
                // same position, maybe const store
                // ----------- start
                // aconst_null
                // astore x       <----   x is ssa
                // ----------- end
                // then it should be automatically removed (or it will not be valid bytecode)
                continue;
            }
            ClassType expType = fromExceptionType(node.type);
            exceptionEntries.add(new ExceptionEntry(start, end, (Catch) handler, expType));
        }
        return exceptionEntries;
    }

    private int getInsnIndex(AbstractInsnNode insn) {
        assert insn != null;
        return source.instructions.indexOf(insn);
    }

    private Stmt getFirstStmt(LabelNode label) {
        BytecodeBlock block = cfg.searchForValidBlock(getInsnIndex(label));
        while (block.getStmts().isEmpty()) {
            BytecodeBlock next1 = cfg.getNormalSuccsOf(block).get(0);
            BytecodeBlock next2 = cfg.getObject(block.getIndex() + 1);
            if (next1 != next2) {
                // should not happen, which means refer to unreachable code
                // but may happen in real world code (this is valid bytecode)
                logger.atTrace()
                        .log("[IR] Unreachable code reference detected in method: "
                                + method.toString());
            }
            block = next2;
        }
        return block.getStmts().get(0);
    }

    private void setSwitchTargets(List<LabelNode> labels, LabelNode dflt, Stmt stmt) {
        assert stmt instanceof SwitchStmt;
        SwitchStmt switchStmt = (SwitchStmt) stmt;
        List<Stmt> cases = labels.stream().map(this::getFirstStmt).toList();
        Stmt defaultStmt = getFirstStmt(dflt);
        switchStmt.setTargets(cases);
        switchStmt.setDefaultTarget(defaultStmt);
    }

    private void setJumpTargets(AbstractInsnNode insn, Stmt jumpStmt) {
        assert jumpStmt != null;
        if (insn instanceof JumpInsnNode jump) {
            Stmt first = getFirstStmt(jump.label);
            if (jumpStmt instanceof Goto gotoStmt) {
                assert first != null;
                gotoStmt.setTarget(first);
            } else if (jumpStmt instanceof If ifStmt) {
                assert first != null;
                ifStmt.setTarget(first);
            } else if (jumpStmt instanceof Return) {
                return;
            } else {
                throw new IllegalArgumentException();
            }
        } else if (insn instanceof LookupSwitchInsnNode lookup) {
            setSwitchTargets(lookup.labels, lookup.dflt, jumpStmt);
        } else if (insn instanceof TableSwitchInsnNode table) {
            setSwitchTargets(table.labels, table.dflt, jumpStmt);
        }
        // insn is not jump, do nothing
    }

    private ClassType fromExceptionType(String internalName) {
        if (internalName == null) {
            return typeSystem.throwableType();
        } else {
            ReferenceType r = typeSystem.fromAsmInternalName(internalName);
            if (r instanceof ClassType c) {
                return c;
            } else {
                throw new UnsupportedOperationException("Unsupported exception type: " + r);
            }
        }
    }
}
