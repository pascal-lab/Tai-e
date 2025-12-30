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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import pascal.taie.World;
import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.frontend.java.ir.ssa.FrontendPhiExp;
import pascal.taie.frontend.java.ir.ssa.FrontendPhiStmt;
import pascal.taie.frontend.java.ir.ssa.VarSSAInfo;
import pascal.taie.frontend.java.ir.typing.TypeInference;
import pascal.taie.ir.DefaultIR;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.LValue;
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
import pascal.taie.util.Indexer;
import pascal.taie.util.collection.LazyArray;
import pascal.taie.util.graph.Dominators;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * The main class for converting bytecode to Tai-e IR.
 */
public class BytecodeIRBuilder {

    private static final Logger logger = LogManager.getLogger(BytecodeIRBuilder.class);

    private final FrontendTypeSystem typeSystem;

    /**
     * Tai-e IR output
     */
    private IR ir;

    /**
     * The method to be built
     */
    public final JMethod method;

    /**
     * Bytecode input, a bytecode method represented by {@link JSRInlinerAdapter}
     */
    private final JSRInlinerAdapter source;

    /**
     * Manager that manage the creation and naming of Tai-e IR variables
     */
    public final VarManager varManager;

    /**
     * Generated Tai-e IR stmts
     */
    private List<Stmt> stmts;

    /**
     * Generated Tai-e IR exception entry ({@link ExceptionEntry})
     */
    private List<ExceptionEntry> exceptionEntries;

    /**
     * Operand stack for stack manipulation operations
     */
    private OperandStack operandStack;

    /**
     * If we build SSA IR. Read from {@link pascal.taie.config.Options}
     */
    private final boolean isSSA;

    /**
     * Record whether a Var is SSA.
     */
    private final VarSSAInfo varSSAInfo;

    /**
     * Manages load/store operations on local variable slots and handles SSA-related transformations.
     */
    private final SlotManager slotManager;

    /**
     * Manages the mapping from bytecode instructions to their generated IR statements.
     */
    private final StmtManager stmtManager;

    /**
     * Process bytecode instructions into Tai-e IR statements
     */
    private BytecodeProcessor bytecodeProcessor;

    /**
     * Dominator and dominator frontier computed for bytecode block graph
     */
    private Dominators<BytecodeBlock> dom;


    BytecodeIRBuilder(FrontendTypeSystem typeSystem, JMethod method,
                      AsmMethodSource methodSource) {
        this.typeSystem = typeSystem;
        this.method = method;
        this.source = methodSource.adapter();
        assert method.getName().equals(source.name);
        this.varSSAInfo = new VarSSAInfo();
        this.isSSA = World.get().getOptions().isSSA();
        this.varManager = new VarManager(method,
                source.localVariables, source.instructions, source.maxLocals, varSSAInfo);
        this.stmtManager = new StmtManager(isSSA, source.instructions);
        this.slotManager = new SlotManager(method,
                varManager, isSSA, varSSAInfo, source, stmtManager);
        this.stmts = new ArrayList<>();
    }

    public void build() {
        if (source.instructions.size() != 0) {
            buildCFG();
            buildDom();
            slotManager.build(cfg, dom);
            operandStack = new OperandStack(method, varManager, cfg, varSSAInfo, stmtManager);
            bytecodeProcessor = new BytecodeProcessor(typeSystem, varManager,
                    method, isSSA, varSSAInfo, operandStack, slotManager, stmtManager);
            traverseBlocks();
            inferTypes();
            makeStmts(true);
            makeExceptionTable();
            this.ir = getIR();
        }
    }

    private void buildDom() {
        dom = new Dominators<>(cfg);
    }

    private void inferTypes() {
        new TypeInference(this, typeSystem).build();
    }

    public void dump() {
        BytecodeDumper.printDotFile(cfg,
                new Indexer<>() {
                    @Override
                    public int getIndex(AbstractInsnNode insn) {
                        return source.instructions.indexOf(insn);
                    }

                    @Override
                    public AbstractInsnNode getObject(int index) {
                        return source.instructions.get(index);
                    }
                },
                method.toString());
    }

    private void verify() {
        for (Var v : varManager.getVars()) {
            assert verifyAllInStmts(v.getInvokes());
            assert verifyAllInStmts(v.getLoadArrays());
            assert verifyAllInStmts(v.getStoreArrays());
            assert verifyAllInStmts(v.getLoadFields());
            assert verifyAllInStmts(v.getStoreFields());
        }

        for (int i = 0; i < varManager.getVars().size(); ++i) {
            Var v = varManager.getVars().get(i);
            assert v.getIndex() == i;
        }
    }

    private <T extends Stmt> boolean verifyAllInStmts(List<T> stmts) {
        return stmts.stream().allMatch(this::verifyInStmts);
    }

    private boolean verifyInStmts(Stmt stmt) {
        return stmt.getIndex() != -1 &&
                this.stmts.size() > stmt.getIndex() &&
                this.stmts.get(stmt.getIndex()) == stmt;
    }

    private int getInsnIndex(AbstractInsnNode insn) {
        assert insn != null;
        return source.instructions.indexOf(insn);
    }

    private IR getIR() {
        Var thisVar = varManager.getThisVar();
        List<Var> params = varManager.getParams();
        List<Var> vars = varManager.getVars();
        Set<Var> retVars = varManager.getRetVars();
        return new DefaultIR(method, thisVar, params, retVars, vars, stmts, exceptionEntries);
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

    private void makeStmts(boolean isLastTime) {
        this.stmts = new ArrayList<>(source.instructions.size());
        // Add trigger whether we process phiStmts.
        List<FrontendPhiStmt> frontendPhiStmts = isLastTime ? new ArrayList<>() : null;
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
                    if (isLastTime && t instanceof FrontendPhiStmt p) {
                        frontendPhiStmts.add(p);
                    }
                    t.setIndex(now++);
                    stmts.add(t);
                }
                setJumpTargets(block.getLastInsn(), block.getLastStmt());
            }
        }

        if (isLastTime) {
            PhiResolver resolver = new PhiResolver(cfg);
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
        }
    }

    private void makeExceptionTable() {
        List<ExceptionEntry> res = new ArrayList<>();
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
            res.add(new ExceptionEntry(start, end, (Catch) handler, expType));
        }
        exceptionEntries = res;
    }

//    private void mergeStack1(List<Stmt> auxiliary, Stack<StackItem> nowStack, Stack<StackItem> targetStack) {
//        Exp v = targetStack.pop();
//        if (v instanceof Top) {
//            return;
//        }
//        assert v instanceof Var: "merge target should be var of top";
//        Exp e = peekExp(nowStack);
//        if (e == v) {
//            popExp(nowStack);
//        } else {
//            Stmt stmt = popToVar(nowStack, (Var) v);
//            auxiliary.add(stmt);
//        }
//    }

//    private void mergeStack(BytecodeBlock bb, Stack<Exp> nowStack, Stack<Exp> target) {
//        List<Stmt> auxiliary = new ArrayList<>();
//        Stack<Exp> nowStack1 = new Stack<>();
//        Stack<Exp> target1 = new Stack<>();
//        nowStack1.addAll(nowStack);
//        target1.addAll(target);
//        while (! nowStack1.isEmpty()) {
//            mergeStack1(auxiliary, nowStack1, target1);
//        }
//        appendStackMergeStmts(bb, auxiliary);
//        assert target1.empty();
//    }

    private void solveAllPhiAndOutput() {
        List<StackPhi> stackPhiList = operandStack.getStackPhiList();
        for (BytecodeBlock bb : cfg) {
            fillInLoopHeaderStackPhis(bb);
            if (isSSA) {
                slotManager.addInDefsForSlotPhis(bb);
            }
        }
        propagatePhiUsed(stackPhiList);
        resolveStackPhi(stackPhiList);
        for (BytecodeBlock bb : cfg) {
            // unreachable
            if (bb.getOutStack() == null) {
                continue;
            }
            if (!isSSA) {
                if (stackMergeStmts.contains(bb.getIndex())) {
                    List<Stmt> stmts = stackMergeStmts.get(bb.getIndex());
                    stmtManager.appendStackMergeStmts(bb, stmts);
                }
            }
            stmtManager.buildBlockStmts(bb);
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
        for (StackItem item : phi.getNodes()) {
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
                boolean hasCriticalInEdge = block.isLoopHeader();
//                for (int i = 0; i < g.getInEdgesCount(block); ++i) {
//                    int pred = g.g.getInEdge(block.getIndex(), i);
//                    if (g.getOutEdgesCount(pred) > 1) {
//                        hasCriticalInEdge = true;
//                        break;
//                    }
//                }
                for (StackItem item : block.getInStack()) {
                    Exp e = item.originalExp();
                    if (e instanceof StackPhi phi1) {
                        if (phi1.getWriteOutVar() != null) {
                            continue;
                        }
                        boolean useWorseSolution = hasCriticalInEdge && phi1.used;
                        Var writeOut = useWorseSolution ? varManager.getTempVar() : phi1.getVar();
                        varSSAInfo.setNonSSA(writeOut);
                        if (useWorseSolution) {
                            // add `v = writeOut` before any definition (first instruction) in create pos
                            BytecodeBlock createPos = phi1.createPos;
                            LValue lValue = phi1.getVar();
                            addToBlockHead(createPos, Utils.newAssignStmt(method, lValue, writeOut));
                        }
                        phi1.setWriteOutVar(writeOut);
                    }
                }
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
                    StackItem item = phi.getNodes().get(i - unreachableOffset);
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

    private LazyArray<List<Stmt>> stackMergeStmts;

    private void resolveStackPhi(StackPhi phi) {
        assert !phi.resolved;
        int unreachableOffset = 0;
        Var writeOut = phi.getWriteOutVar();
        for (int i = 0; i < phi.getNodes().size(); ++i) {
            StackItem item = phi.getNodes().get(i);
            BytecodeBlock inEdge = cfg.getNormalPredOf(phi.createPos, i + unreachableOffset);
            if (inEdge.getOutStack() == null) {
                unreachableOffset++;
                continue;
            }
            List<Stmt> stmts = stackMergeStmts.get(inEdge.getIndex());
            Exp e = item.exp();
            if (e instanceof StackPhi phi1) {
                e = phi1.getVar();
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
                BytecodeBlock outEdge = cfg.getNormalPredOf(current, i);
                if (outEdge.getOutStack() == null) {
                    assert outEdge.getInStack() == null;
                    continue;
                }
                for (int j = 0; j < outEdge.getOutStack().size(); ++j) {
                    Exp currentExp = inStack.get(j).originalExp();
                    if (currentExp instanceof Top) {
                        continue;
                    }
                    StackPhi phi = (StackPhi) currentExp;
                    StackItem item = outEdge.getOutStack().get(j);
                    phi.getNodes().add(item);
                }
            }
        }
    }

    /**
     * Build CFG from ASM instructions using {@link BytecodeCFGBuilder}.
     */
    public BytecodeCFG cfg;

    private void buildCFG() {
        // Build CFG with rwTable visitor and exception type resolver
        cfg = new BytecodeCFGBuilder(source,
                slotManager::writeRwTable,
                this::fromExceptionType)
                .build();
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

    private void traverseBlocks() {
        cfg.getEntry().setInStack(new Stack<>());
        for (BytecodeBlock block : dom.getReversePostOrder()) {
            bytecodeProcessor.processBlock2Stmts(block);
        }
        solveAllPhiAndOutput();
    }

//    private void setLineNumber() {
//        int currentLineNumber = -1;
//        for (var insn : source.instructions) {
//            if (!(insn instanceof LabelNode)) {
//                if (insn instanceof LineNumberNode l) {
//                    currentLineNumber = l.line;
//                } else {
//                    if (currentLineNumber == -1) {
//                        logger.atDebug().log("[IR] no line number info, method: " + method);
//                        return;
//                    }
//                    var stmt = insn2Stmt[getInsnIndex(insn)];
//                    if (stmt != null) {
//                        stmt.setLineNumber(currentLineNumber);
//                    }
//
//                    var stmts = auxiliaryStmts.get(getInsnIndex(insn));
//                    if (stmts != null) {
//                        for (var s : stmts) {
//                            s.setLineNumber(currentLineNumber);
//                        }
//                    }
//                }
//            }
//        }
//    }

    public IR getIr() {
        return ir;
    }

    public boolean isSSA() {
        return isSSA;
    }
}
