package pascal.taie.frontend.newfrontend;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import pascal.taie.frontend.newfrontend.data.SparseArray;
import pascal.taie.frontend.newfrontend.dbg.BytecodeVisualizer;
import pascal.taie.frontend.newfrontend.report.StageTimer;
import pascal.taie.frontend.newfrontend.ssa.Dominator;
import pascal.taie.frontend.newfrontend.ssa.FastVarSplitting;
import pascal.taie.frontend.newfrontend.ssa.IndexedGraph;
import pascal.taie.frontend.newfrontend.ssa.PhiExp;
import pascal.taie.frontend.newfrontend.ssa.PhiResolver;
import pascal.taie.frontend.newfrontend.ssa.PhiStmt;
import pascal.taie.frontend.newfrontend.ssa.SSATransform;
import pascal.taie.frontend.newfrontend.typing.VarSSAInfo;
import pascal.taie.ir.DefaultIR;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.ArrayLengthExp;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.BitwiseExp;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.ComparisonExp;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.DoubleLiteral;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.ExpModifier;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.FloatLiteral;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InstanceOfExp;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInterface;
import pascal.taie.ir.exp.InvokeSpecial;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.InvokeVirtual;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.LongLiteral;
import pascal.taie.ir.exp.MethodHandle;
import pascal.taie.ir.exp.MethodType;
import pascal.taie.ir.exp.NegExp;
import pascal.taie.ir.exp.NewArray;
import pascal.taie.ir.exp.NewInstance;
import pascal.taie.ir.exp.NewMultiArray;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.ShiftExp;
import pascal.taie.ir.exp.StaticFieldAccess;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Goto;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LookupSwitch;
import pascal.taie.ir.stmt.Monitor;
import pascal.taie.ir.stmt.Nop;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.SwitchStmt;
import pascal.taie.ir.stmt.TableSwitch;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.VoidType;
import pascal.taie.util.Indexer;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;

import static pascal.taie.frontend.newfrontend.Utils.*;
import static pascal.taie.language.type.BooleanType.BOOLEAN;
import static pascal.taie.language.type.ByteType.BYTE;
import static pascal.taie.language.type.CharType.CHAR;
import static pascal.taie.language.type.DoubleType.DOUBLE;
import static pascal.taie.language.type.FloatType.FLOAT;
import static pascal.taie.language.type.IntType.INT;
import static pascal.taie.language.type.LongType.LONG;
import static pascal.taie.language.type.ShortType.SHORT;

public class AsmIRBuilder {

    private IR ir;

    final JMethod method;

    private final int classFileVersion;

    private final JSRInlinerAdapter source;

    List<BytecodeBlock> blockSortedList; // blocks that are sorted in bytecode order.

    private BytecodeBlock entry;

    final VarManager manager;

    final Stmt[] asm2Stmt;

    final List<List<Stmt>> auxiliaryStmts;

    private final boolean isEmpty;

    private boolean isFrameUsable;

    private List<Stmt> stmts;

    private List<ExceptionEntry> exceptionEntries;

    private final List<StackPhi> phiList;

    private final DUInfo duInfo;

    private List<Pair<List<BytecodeBlock>, BytecodeBlock>> tryAndHandlerBlocks;

    private static final Logger logger = LogManager.getLogger();

    private static final StackItem TOP = new StackItem(Top.Top, null);

    private int currentLineNumber;

    private static boolean EXPERIMENTAL = true;

    private final boolean USE_SSA;

    private final boolean USE_TYPING_ALGO2;

    final VarSSAInfo varSSAInfo;

    private Dominator<BytecodeBlock> dom;

    public AsmIRBuilder(JMethod method, AsmMethodSource methodSource) {
        this.method = method;
        this.source = methodSource.adapter();
        assert method.getName().equals(source.name);
        this.classFileVersion = methodSource.classFileVersion();
        int instrSize = source.instructions.size();
        this.isEmpty = instrSize == 0;
        this.varSSAInfo = new VarSSAInfo();
        this.USE_SSA = FrontendOptions.get().isSSA();
        this.USE_TYPING_ALGO2 = FrontendOptions.get().isUseTypingAlgo2();
        if (!isEmpty) {
            this.manager = new VarManager(method,
                    source.localVariables, source.instructions, source.maxLocals, varSSAInfo);
            this.asm2Stmt = new Stmt[instrSize];
            this.auxiliaryStmts = new ArrayList<>(instrSize);
            for (int i = 0; i < instrSize; ++i) {
                auxiliaryStmts.add(null);
            }
            this.stmts = new ArrayList<>();
            this.phiList = new ArrayList<>();
            this.duInfo = new DUInfo(source.maxLocals);
        } else {
            this.manager = null;
            this.asm2Stmt = null;
            this.auxiliaryStmts = null;
            this.stmts = null;
            this.phiList = null;
            this.duInfo = null;
        }
    }

    public void build() {
        // a.analyze()
        if (!isEmpty) {
            StageTimer stageTimer = StageTimer.getInstance();
            stageTimer.startTypelessIR();
            buildCFG();
            traverseBlocks();
            stageTimer.endTypelessIR();
            this.isFrameUsable = classFileVersion >= Opcodes.V1_6;
            if (isFrameUsable() && !USE_TYPING_ALGO2) {
                inferTypeWithFrame();
            } else {
                inferTypeWithoutFrame();
            }
            if (USE_SSA && !EXPERIMENTAL) {
                makeStmts(false);
                makeExceptionTable();
                stageTimer.startSplitting();
                ssa();
                stageTimer.endSplitting();
            }
            makeStmts(true);
            makeExceptionTable();
            verify();
            this.ir = getIR();
        }
    }

    void inferTypeWithFrame() {
        StageTimer stageTimer = StageTimer.getInstance();
        if (!EXPERIMENTAL) {
            stageTimer.startSplitting();
            VarWebSplitter splitter = new VarWebSplitter(this);
            splitter.build();
            stageTimer.endSplitting();
        }
        stageTimer.startTyping();
        TypeInference0 inference = new TypeInference0(this);
        inference.build();
        stageTimer.endTyping();
    }

    void inferTypeWithoutFrame() {
        StageTimer stageTimer = StageTimer.getInstance();
        if (!EXPERIMENTAL) {
            stageTimer.startSplitting();
            VarWebSplitter splitter = new VarWebSplitter(this);
            splitter.build();
            stageTimer.endSplitting();
        }
        stageTimer.startTyping();
        TypeInference inference = new TypeInference(this);
        inference.build();
        stageTimer.endTyping();
    }

    void ssa() {
        SSATransform<BytecodeBlock> ssa = new SSATransform<>(method, g, manager, duInfo);
        ssa.build();
    }

    public boolean isFrameUsable() {
        return isFrameUsable;
    }

    // TODO: optimize
//    public boolean checkFrameValid() {
//        for (BytecodeBlock block : blockSortedList) {
//            if (block.inEdges().size() >= 2 && block.getFrame() == null) {
//                return false;
//            }
//
//            if (block.outEdges().size() >= 2 &&
//                    block.outEdges().stream().allMatch(o -> o.getFrame() == null)) {
//                return false;
//            }
//
//            if (block.isCatch() && block.getFrame() == null) {
//                return false;
//            }
//        }
//        return true;
//    }

    public void dump() {
        BytecodeVisualizer.printDotFile(g,
                new Indexer<>() {
                    @Override
                    public int getIndex(AbstractInsnNode o) {
                        return AsmIRBuilder.this.getIndex(o);
                    }

                    @Override
                    public AbstractInsnNode getObject(int index) {
                        return source.instructions.get(index);
                    }
                },
                method.toString());
    }

    BytecodeBlock getEntryBlock() {
        return entry;
    }

    public List<Stmt> getAllStmts() {
        return blockSortedList.stream()
                .flatMap(block -> block.getStmts().stream())
                .toList();
    }

    public List<BytecodeBlock> getAllBlocks() {
        return blockSortedList;
    }

    public List<ExceptionEntry> getExceptionEntries() {
        return exceptionEntries;
    }

    private void verify() {
        for (Var v : manager.getVars()) {
            assert verifyAllInStmts(v.getInvokes());
            assert verifyAllInStmts(v.getLoadArrays());
            assert verifyAllInStmts(v.getStoreArrays());
            assert verifyAllInStmts(v.getLoadFields());
            assert verifyAllInStmts(v.getStoreFields());
        }

        for (int i = 0; i < manager.getVars().size(); ++i) {
            Var v = manager.getVars().get(i);
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

    private int getIndex(AbstractInsnNode node) {
        assert node != null;
        return source.instructions.indexOf(node);
    }

    private Stmt getAssignStmt(LValue lValue, Exp e) {
        if (lValue instanceof Var v && !EXPERIMENTAL) {
            duInfo.addDefBlock(v, currentBlock);
        }
        tryInferType(lValue, e);
        return Utils.getAssignStmt(method, lValue, e);
    }

    private void tryInferType(LValue l, Exp e) {
        if (l instanceof Var left && left.getType() == null) {
            Type right;

            if (e instanceof Var r) {
                right = r.getType() == null ? null : r.getType();
            } else if (e instanceof ArrayAccess ac) {
                right = ac.getBase().getType() == null ? null :
                        ac.getBase().getType() == NullType.NULL ? NullType.NULL :
                                ac.getType();
            } else if (e instanceof BinaryExp binary) {
                right = binary.getOperand1().getType() == null ? null : binary.getType();
            } else if (e instanceof NegExp neg) {
                right = neg.getOperand().getType() == null ? null : neg.getType();
            } else {
                right = e.getType();
            }

            if (right instanceof PrimitiveType) {
                ExpModifier.setType(left, right);
            } else if (right != null && varSSAInfo.isSSAVar(left)) {
                ExpModifier.setType(left, right);
            }
        }
    }

    private boolean isDword(AbstractInsnNode node, Exp e) {
        if (e instanceof InvokeExp invokeExp) {
            Type returnType = invokeExp.getType();
            return returnType == DOUBLE || returnType == LONG;
        } else if (e instanceof LongLiteral || e instanceof DoubleLiteral) {
            return true;
        } else if (e instanceof FieldAccess access) {
            Type fieldType = access.getType();
            return fieldType == DOUBLE || fieldType == LONG;
        } else if (e instanceof Var v && v.isConst()) {
            Literal literal = v.getConstValue();
            return literal instanceof DoubleLiteral || literal instanceof LongLiteral;
        }

        int opcode = node.getOpcode();
        return switch (opcode) {
            case Opcodes.LCONST_0, Opcodes.LCONST_1, Opcodes.DCONST_0, Opcodes.DCONST_1,
                    Opcodes.LLOAD, Opcodes.DLOAD, Opcodes.DALOAD, Opcodes.LALOAD,
                    Opcodes.LADD, Opcodes.DADD, Opcodes.LSUB, Opcodes.DSUB,
                    Opcodes.LMUL, Opcodes.DMUL, Opcodes.LDIV, Opcodes.DDIV,
                    Opcodes.LREM, Opcodes.DREM, Opcodes.DNEG, Opcodes.LNEG,
                    Opcodes.LSHL, Opcodes.LSHR, Opcodes.LUSHR, Opcodes.LAND,
                    Opcodes.LOR, Opcodes.LXOR, Opcodes.I2L, Opcodes.I2D,
                    Opcodes.L2D, Opcodes.F2L, Opcodes.F2D, Opcodes.D2L -> true;
            default -> false;
        };
    }

    private StackItem popExp(Stack<StackItem> stack) {
        StackItem e = popStack(stack);
        if (e.e() instanceof Top) {
            StackItem e1 = popStack(stack);
            assert ! (e1.e() instanceof Top);
            return e1;
        } else {
            return e;
        }
    }

    private List<Stmt> clearStmt(AbstractInsnNode node) {
        List<Stmt> res = new ArrayList<>();
        int idx = getIndex(node);
        if (asm2Stmt[idx] != null) {
            res.add(asm2Stmt[idx]);
            asm2Stmt[idx] = null;
        }
        if (auxiliaryStmts.get(idx) != null) {
            res.addAll(auxiliaryStmts.get(idx));
            auxiliaryStmts.set(idx, null);
        }
        return res;
    }

    private void assocStmt(AbstractInsnNode node, Stmt stmt) {
        // TODO: remove this checking
        if (stmt.getLineNumber() == -1) {
            stmt.setLineNumber(currentLineNumber);
        }
        int idx = getIndex(node);
        if (asm2Stmt[idx] == null) {
            asm2Stmt[idx] = stmt;
        } else {
            assocListStmt(node, stmt);
        }
    }

    private void assocListStmt(AbstractInsnNode node, Stmt stmt) {
        List<Stmt> aux = auxiliaryStmts.get(getIndex(node));
        if (aux == null) {
            aux = new ArrayList<>();
            auxiliaryStmts.set(getIndex(node), aux);
        }
        aux.add(stmt);
    }

    private void assocStmt(StackItem item, Stmt stmt) {
        assocStmt(item.origin(), stmt);
    }

    /**
     * Ensure the item is a Var, if not, lift it to a Var.
     * Do nothing for Top, set the `item.var` for Var.
     * @param item the item to be lifted
     */
    private void liftToVar(StackItem item) {
        if (item.e() instanceof Top) {
            return;
        } else if (item.e() instanceof Var v) {
            item.lift(v);
        } else {
            Var v = toVar(item.e(), item.origin());
            item.lift(v);
        }
    }

    /**
     * Ensure the item is a Var, if not, lift it to a Var.
     * Emit a new ($-v = e) even when `e` is a Var.
     * @param item the item to be lifted
     */
    private void forceLiftToVar(StackItem item) {
        Var v = toVar(item.e(), item.origin());
        item.lift(v);
    }

    private Var toVar(Exp e, AbstractInsnNode orig) {
        assert ! (e instanceof Var v && manager.isTempVar(v));
        if (e instanceof StackPhi phi) {
            phi.setUsed();
            assert phi.getVar() != null;
            return phi.getVar();
        }

        Var v;
        if (e instanceof NullLiteral) {
            // $null could be used without assign
            return manager.getNullLiteral();
        }
        if (e instanceof Literal l) {
            if (manager.peekConstVar(l)) {
                return manager.getConstVar(l);
            } else {
                v = manager.getConstVar(l);
            }
        } else {
            v = manager.getTempVar();
        }
        // if reach here
        // this method should only be called once (normally)
        if (false) {
//            logger.atInfo().log("[IR] Multiple expression belonging to one bytecode" + "\n" +
//                                "     It may be an error, you should check IR." + "\n" +
//                                "     In method: " + method.toString());
        }
        Stmt auxStmt = getAssignStmt(v, e);
        assocStmt(orig, auxStmt);
        return v;
    }

    private StackItem popStack(Stack<StackItem> stack) {
        return stack.pop();
    }

    private Var popVar(Stack<StackItem> stack) {
        StackItem e = popExp(stack);
        liftToVar(e);
        return e.var();
    }

    private Stmt popToVar(Stack<StackItem> stack, Var v, BytecodeBlock block) {
        StackItem top = popExp(stack);
        // Note: Var . getUses() will return empty set
        if (top.e() instanceof StackPhi) {
            liftToVar(top);
        } else {
            ensureStackSafety(stack, e -> e == v || e.getUses().contains(v));
        }
        return getAssignStmt(v, top.e());
    }

    private void popToEffect(Stack<StackItem> stack) {
        // normally, this should only be used to pop a InvokeExp
        StackItem item = popStack(stack);
        Exp e = item.e();
        if (e instanceof Top) {
            return;
        } else {
            expToEffect(item);
        }
    }

    private void expToEffect(StackItem item) {
        Exp e = item.e();
        if (e instanceof InvokeExp invokeExp) {
            assocStmt(item, new Invoke(method, invokeExp));
        } else if (maySideEffect(e)) {
            assocStmt(item, getAssignStmt(manager.getTempVar(), e));
        }
    }

    private void automaticPopToEffect(Stack<StackItem> stack) {
        StackItem item = popExp(stack);
        expToEffect(item);
    }

    private void dup(Stack<StackItem> stack, int takes, int seps) {
        List<StackItem> takesList = new ArrayList<>(takes);
        for (int i = 0; i < takes; ++i) {
            StackItem e = popStack(stack);
            liftToVar(e);
            takesList.add(e);
        }
        Collections.reverse(takesList);
        List<StackItem> sepsList = new ArrayList<>(seps);
        for (int i = 0; i < seps; ++i) {
            sepsList.add(popStack(stack));
        }
        Collections.reverse(sepsList);
        stack.addAll(takesList);
        stack.addAll(sepsList);
        stack.addAll(takesList);
    }

    private void ensureStackSafety(Stack<StackItem> stack, Function<Exp, Boolean> predicate) {
        for (StackItem item : stack) {
            Exp e = item.e();
            if (e instanceof Top || e instanceof StackPhi) {
                continue;
            }
            if (predicate.apply(e)) {
                forceLiftToVar(item);
            }
        }
    }

    private boolean maySideEffect(Exp e) {
        return !(e instanceof Var || e instanceof StackPhi || e instanceof Literal);
    }

    private void pushExp(AbstractInsnNode node, Stack<StackItem> stack, Exp e) {
        assert ! (e instanceof Top);
        if (maySideEffect(e)) {
            ensureStackSafety(stack, this::maySideEffect);
        }
        stack.push(new StackItem(e, node));
        if (isDword(node, e)) {
            stack.push(TOP);
        }
    }

    private void pushConst(AbstractInsnNode node, Stack<StackItem> stack, Literal literal) {
        if (manager.peekConstVar(literal)) {
            // note: if this code is reached, and `node` is `Ldc`,
            // pushExp cannot automatically push a `Top`
            pushExp(node, stack, manager.getConstVar(literal));
        } else {
            pushExp(node, stack, literal);
        }
    }


    private IR getIR() {
        Var thisVar = manager.getThisVar();
        List<Var> params = manager.getParams();
        List<Var> vars = manager.getVars();
        Set<Var> retVars = manager.getRetVars();
        return new DefaultIR(method, thisVar, params, retVars , vars, stmts, exceptionEntries);
    }

    private Stmt getFirstStmt(LabelNode label) {
        BytecodeBlock block = searchForValidBlock(label);
        while (block.getStmts().isEmpty()) {
            BytecodeBlock next1 = getOutEdge(block, 0);
            BytecodeBlock next2 = blockSortedList.get(block.getIndex() + 1);
            if (next1 != next2) {
                // should not happen, which means refer to unreachable code
                // but may happen in real world code (this is valid bytecode)
                logger.atTrace().log("[IR] Unreachable code reference detected in method: "
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

    private void setJumpTargets(AbstractInsnNode node, Stmt stmt) {
        if (node instanceof JumpInsnNode jump) {
            Stmt first = getFirstStmt(jump.label);
            if (stmt instanceof Goto gotoStmt) {
                assert first != null;
                gotoStmt.setTarget(first);
            } else if (stmt instanceof If ifStmt) {
                assert first != null;
                ifStmt.setTarget(first);
            } else if (stmt instanceof Return) {
                return;
            } else {
                throw new IllegalArgumentException();
            }
        } else if (node instanceof LookupSwitchInsnNode lookup) {
            setSwitchTargets(lookup.labels, lookup.dflt, stmt);
        } else if (node instanceof TableSwitchInsnNode table) {
            setSwitchTargets(table.labels, table.dflt, stmt);
        }
        // node is not jump, do nothing
    }

    private void makeStmts(boolean isLastTime) {
        this.stmts = new ArrayList<>(source.instructions.size());
        // Add trigger whether we process phiStmts.
        List<PhiStmt> phiStmts = isLastTime ? new ArrayList<>() : null;
        int now = 0;
        for (Var v : manager.intConstVarCache) {
            if (v != null) {
                Stmt curr = getAssignStmt(v, v.getConstValue());
                curr.setIndex(now++);
                stmts.add(curr);
            }
        }
        for (BytecodeBlock block : blockSortedList) {
            List<Stmt> blockStmts = block.getStmts();
            if (!blockStmts.isEmpty()) {
                for (Stmt t : blockStmts) {
                    if (isLastTime && t instanceof PhiStmt p) {
                        phiStmts.add(p);
                    }
                    t.setIndex(now++);
                    stmts.add(t);
                }
                setJumpTargets(block.getLastBytecode(), block.getLastStmt());
            }
        }

        if (isLastTime) {
            PhiResolver<? extends IBasicBlock> resolver = new PhiResolver<>(g);
            // Make PhiStmts using stmt.index as the value source.
            for (PhiStmt p : phiStmts) {
                p.getRValue().indexValueAndSource(resolver);
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
            }
            ClassType expType = fromExceptionType(node.type);
            res.add(new ExceptionEntry(start, end, (Catch) handler, expType));
        }
        exceptionEntries = res;
    }

    private boolean inRange(int opcode, int min, int max) {
        return opcode >= min && opcode <= max;
    }

    private boolean isConstInsn(int opcode) {
        return opcode == Opcodes.ACONST_NULL ||
                inRange(opcode, Opcodes.ICONST_M1, Opcodes.ICONST_5) ||
                inRange(opcode, Opcodes.FCONST_0, Opcodes.FCONST_2) ||
                inRange(opcode, Opcodes.LCONST_0, Opcodes.LCONST_1) ||
                inRange(opcode, Opcodes.DCONST_0, Opcodes.DCONST_1);
    }

    private boolean isAddInsn(int opcode) {
        return inRange(opcode, Opcodes.IADD, Opcodes.DADD);
    }
    private boolean isSubInsn(int opcode) {
        return inRange(opcode, Opcodes.ISUB, Opcodes.DSUB);
    }
    private boolean isMulInsn(int opcode) {
        return inRange(opcode, Opcodes.IMUL, Opcodes.DMUL);
    }
    private boolean isDivInsn(int opcode) {
        return inRange(opcode, Opcodes.IDIV, Opcodes.DDIV);
    }
    private boolean isRemInsn(int opcode) {
        return inRange(opcode, Opcodes.IREM, Opcodes.DREM);
    }
    private boolean isNegInsn(int opcode) {
        return inRange(opcode, Opcodes.INEG, Opcodes.DNEG);
    }

    private boolean isPrimCastInsn(int opcode) {
        return inRange(opcode, Opcodes.I2L, Opcodes.I2S);
    }

    private boolean isBinaryInsn(int opcode) {
        return (inRange(opcode, Opcodes.IADD, Opcodes.LXOR) && ! isNegInsn(opcode)) ||
                isComparisonInsn(opcode);
    }

    private boolean isArithmeticInsn(int opcode) {
        return inRange(opcode, Opcodes.IADD, Opcodes.DREM);
    }

    private boolean isBitwiseInsn(int opcode) {
        return inRange(opcode, Opcodes.IAND, Opcodes.LXOR);
    }

    private boolean isShiftInsn(int opcode) {
        return inRange(opcode, Opcodes.ISHL, Opcodes.LUSHR);
    }

    private boolean isComparisonInsn(int opcode) {
        return inRange(opcode, Opcodes.LCMP, Opcodes.DCMPG);
    }

    private boolean isReturnInsn(int opcode) {
        return inRange(opcode, Opcodes.IRETURN, Opcodes.RETURN);
    }

    private boolean isStackInsn(int opcode) {
        return inRange(opcode, Opcodes.POP, Opcodes.SWAP);
    }

    private boolean isArrayLoadInsn(int opcode) {
        return inRange(opcode, Opcodes.IALOAD, Opcodes.SALOAD);
    }

    private boolean isArrayStoreInsn(int opcode) {
        return inRange(opcode, Opcodes.IASTORE, Opcodes.SASTORE);
    }

    private Literal getConstValue(InsnNode node) {
        int opcode = node.getOpcode();
        if (opcode == Opcodes.ACONST_NULL) {
            return NullLiteral.get();
        } else if (inRange(opcode, Opcodes.ICONST_M1, Opcodes.ICONST_5)) {
            return IntLiteral.get(opcode - Opcodes.ICONST_M1 - 1);
        } else if (inRange(opcode, Opcodes.FCONST_0, Opcodes.FCONST_2)) {
            return FloatLiteral.get(opcode - Opcodes.FCONST_0 + 0.0f);
        } else if (inRange(opcode, Opcodes.DCONST_0, Opcodes.DCONST_1)) {
            return DoubleLiteral.get(opcode - Opcodes.DCONST_0 + 0.0);
        } else if (inRange(opcode, Opcodes.LCONST_0, Opcodes.LCONST_1)) {
            return LongLiteral.get(opcode - Opcodes.LCONST_0);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private int unifyIfOp(int opcode) {
        if (inRange(opcode, Opcodes.IFEQ, Opcodes.IFLE)) {
            return opcode - Opcodes.IFEQ + Opcodes.IF_ICMPEQ;
        } else if (inRange(opcode, Opcodes.FCMPL, Opcodes.FCMPG)) {
            return opcode - Opcodes.FCMPL + Opcodes.DCMPL;
        } else {
            return opcode;
        }
    }

    private ConditionExp.Op toTIRCondOp(int opcode) {
        opcode = unifyIfOp(opcode);
        return switch (opcode) {
            case Opcodes.IF_ICMPEQ, Opcodes.IF_ACMPEQ, Opcodes.IFNULL -> ConditionExp.Op.EQ;
            case Opcodes.IF_ICMPNE, Opcodes.IF_ACMPNE, Opcodes.IFNONNULL -> ConditionExp.Op.NE;
            case Opcodes.IF_ICMPLT -> ConditionExp.Op.LT;
            case Opcodes.IF_ICMPGE -> ConditionExp.Op.GE;
            case Opcodes.IF_ICMPGT -> ConditionExp.Op.GT;
            case Opcodes.IF_ICMPLE -> ConditionExp.Op.LE;
            default -> throw new IllegalArgumentException();
        };
    }

    private ConditionExp getIfExp(Stack<StackItem> stack, int opcode) {
        Var v1;
        Var v2;
        if (inRange(opcode, Opcodes.IFEQ, Opcodes.IFLE)) {
            v1 = popVar(stack);
            v2 = manager.getConstVar(IntLiteral.get(0));
        } else if (opcode == Opcodes.IFNULL || opcode == Opcodes.IFNONNULL) {
            v1 = popVar(stack);
            v2 = manager.getNullLiteral();
        } else {
            v2 = popVar(stack);
            v1 = popVar(stack);
        }
        return new ConditionExp(toTIRCondOp(opcode), v1, v2);
    }

    private ArithmeticExp.Op toTIRArithmeticOp(int opcode) {
        if (isAddInsn(opcode)) {
            return ArithmeticExp.Op.ADD;
        } else if (isSubInsn(opcode)) {
            return ArithmeticExp.Op.SUB;
        } else if (isMulInsn(opcode)) {
            return ArithmeticExp.Op.MUL;
        } else if (isDivInsn(opcode)) {
             return ArithmeticExp.Op.DIV;
        } else if (isRemInsn(opcode)) {
            return ArithmeticExp.Op.REM;
        }  else {
            throw new IllegalArgumentException();
        }
    }

    private BitwiseExp.Op toTIRBitwiseOp(int opcode) {
        return switch (opcode) {
            case Opcodes.IAND, Opcodes.LAND -> BitwiseExp.Op.AND;
            case Opcodes.IOR, Opcodes.LOR -> BitwiseExp.Op.OR;
            case Opcodes.IXOR, Opcodes.LXOR -> BitwiseExp.Op.XOR;
            default -> throw new IllegalArgumentException();
        };
    }

    private ComparisonExp.Op toTIRCmpOp(int opcode) {
        return switch (opcode) {
            case Opcodes.LCMP -> ComparisonExp.Op.CMP;
            case Opcodes.DCMPG, Opcodes.FCMPG -> ComparisonExp.Op.CMPG;
            case Opcodes.DCMPL, Opcodes.FCMPL -> ComparisonExp.Op.CMPL;
            default -> throw new IllegalArgumentException();
        };
    }

    private ShiftExp.Op toTIRShiftOp(int opcode) {
        return switch (opcode) {
            case Opcodes.ISHL, Opcodes.LSHL -> ShiftExp.Op.SHL;
            case Opcodes.ISHR, Opcodes.LSHR -> ShiftExp.Op.SHR;
            case Opcodes.IUSHR, Opcodes.LUSHR -> ShiftExp.Op.USHR;
            default -> throw new IllegalArgumentException();
        };
    }

    private Type getCastType(int opcode) {
        return switch (opcode) {
            case Opcodes.L2I, Opcodes.F2I, Opcodes.D2I -> INT;
            case Opcodes.I2L, Opcodes.F2L, Opcodes.D2L -> LONG;
            case Opcodes.I2F, Opcodes.L2F, Opcodes.D2F -> FLOAT;
            case Opcodes.I2D, Opcodes.L2D,  Opcodes.F2D -> DOUBLE;
            case Opcodes.I2B -> BYTE;
            case Opcodes.I2S -> SHORT;
            case Opcodes.I2C -> CHAR;
            default -> throw new IllegalArgumentException();
        };
    }

    private BinaryExp getBinaryExp(Stack<StackItem> stack, int opcode) {
        Var v2 = popVar(stack);
        Var v1 = popVar(stack);
        if (isArithmeticInsn(opcode)) {
            return new ArithmeticExp(toTIRArithmeticOp(opcode), v1, v2);
        } else if (isBitwiseInsn(opcode)) {
            return new BitwiseExp(toTIRBitwiseOp(opcode), v1, v2);
        } else if (isComparisonInsn(opcode)) {
            return new ComparisonExp(toTIRCmpOp(opcode), v1, v2);
        } else if (isShiftInsn(opcode)) {
            return new ShiftExp(toTIRShiftOp(opcode), v1, v2);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private CastExp getCastExp(Stack<StackItem> stack, int opcode) {
        return getCastExp(stack, getCastType(opcode));
    }

    private CastExp getCastExp(Stack<StackItem> stack, Type t) {
        Var v1 = popVar(stack);
        return new CastExp(v1, t);
    }

    private InvokeExp getInvokeExp(MethodInsnNode methodInsnNode, Stack<StackItem> stack) {
        int opcode = methodInsnNode.getOpcode();
        JClass owner = BuildContext.get().toJClass(methodInsnNode.owner);
        assert owner != null;
        Pair<List<Type>, Type> desc = BuildContext.get().fromAsmMethodType(methodInsnNode.desc);
        String name = methodInsnNode.name;
        boolean isStatic = opcode == Opcodes.INVOKESTATIC;
        MethodRef ref = MethodRef.get(owner, name, desc.first(), desc.second(), isStatic, methodInsnNode.itf);

        List<Var> args = new ArrayList<>();
        for (int i = 0; i < desc.first().size(); ++i) {
            args.add(popVar(stack));
        }
        Collections.reverse(args);
        Var base = isStatic ? null : popVar(stack);

        assert ref.getParameterTypes().size() == args.size();
        return switch (opcode) {
            case Opcodes.INVOKESTATIC -> new InvokeStatic(ref, args);
            case Opcodes.INVOKEVIRTUAL -> new InvokeVirtual(ref, base, args);
            case Opcodes.INVOKEINTERFACE -> new InvokeInterface(ref, base, args);
            case Opcodes.INVOKESPECIAL -> new InvokeSpecial(ref, base, args);
            default -> throw new UnsupportedOperationException();
        };
    }

    private ArrayAccess getArrayAccess(Stack<StackItem> nowStack) {
        Var idx = popVar(nowStack);
        Var ref = popVar(nowStack);
        return new ArrayAccess(ref, idx);
    }

    private void tryFixVarName(Var v, int slot, AbstractInsnNode node) {
        if (manager.existsLocalVariableTable && VarManager.mayRename(v)) {
            Optional<String> name = manager.getName(slot, node);
            name.ifPresent((n) -> {
                String realName = manager.tryUseName(n);
                ExpModifier.setName(v, realName);
            });
        }
    }

    private Var getRWVar(int rwIndex, int slot, AbstractInsnNode node) {
        Var v;
        int defIndex = splitting.getReachDef(rwIndex);
        assert defIndex != -1; // wtf? undefined variable?
        if (isFastProcessVar(defIndex)) {
            v = reachVars[defIndex];
        } else {
            int realVar = splitting.getRealLocalSlot(defIndex);
            assert realVar != -1; // must be phi-connected node, a local is assigned before
            v = manager.getLocal(realVar);
        }
        assert v != null;
        tryFixVarName(v, slot, node);
        return v;
    }

    private void storeRWVar(int rwIndex, int slot, AbstractInsnNode varNode,
                            BytecodeBlock block, Stack<StackItem> stack) {
        Var v;
        if (!splitting.isDefUsed(rwIndex)) {
            // this var is not used, we don't need to generate store stmt
            // still, we need to handle the side effect (e.g. invoke)
            // note: stack may contains `Top`, so don't use `popToEffect`
            automaticPopToEffect(stack);
            return;
        }
        if (isFastProcessVar(rwIndex)) {
            // load insn will use rwTables to get this var
            v = popVar(stack);
            // if this var is a local, we need create another copy
            // in case this local var is modified later
            if (manager.isLocal(v) && !varSSAInfo.isSSAVar(v) && !USE_SSA) {
                Var origin = v;
                v = manager.getTempVar();
                assocStmt(varNode, getAssignStmt(v, origin));
            }
            reachVars[rwIndex] = v;
        } else {
            // still use a local var
            int realVar = splitting.getRealLocalSlot(rwIndex);
            v = manager.getLocal(realVar);
            // use this to generate store stmt
            assert v != null;
            storeExp(varNode, v, stack, block);
        }
        tryFixVarName(v, slot, varNode);
    }

    private void storeExp(VarInsnNode varNode, Stack<StackItem> stack, BytecodeBlock block) {
        if (!EXPERIMENTAL) {
            int idx = varNode.var;
            Var v = manager.getLocal(idx);
            storeExp(varNode, v, stack, block);
        } else {
            int rwIndex = visitRW(block.getIndex(), getIndex(varNode));
            storeRWVar(rwIndex, varNode.var, varNode, block, stack);
        }
    }

    private void storeExp(AbstractInsnNode node, Var v, Stack<StackItem> stack, BytecodeBlock block) {
        Stmt stmt = popToVar(stack, v, block);
        assocStmt(node, stmt);
    }

    private void storeExp(AbstractInsnNode node, LValue left, RValue right) {
        Stmt stmt = getAssignStmt(left, right);
        assocStmt(node, stmt);
    }

    private void returnExp(Stack<StackItem> stack, InsnNode node) {
        int opcode = node.getOpcode();
        if (opcode == Opcodes.RETURN) {
            assocStmt(node, new Return());
        } else {
            Var v = popVar(stack);
            manager.addReturnVar(v);
            assocStmt(node, new Return(v));
        }
    }

    private void throwException(InsnNode node, Stack<StackItem> stack) {
        Var v = popVar(stack);
        assocStmt(node, new Throw(v));
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

    private void appendStackMergeStmts(BytecodeBlock bb, List<Stmt> auxiliary) {
        if (!auxiliary.isEmpty()) {
            AbstractInsnNode lastBytecode = bb.getLastBytecode();
            if (isCFEdge(lastBytecode)) {
                // last stmt may attach goto, if, switch ...
                List<Stmt> stmts = clearStmt(lastBytecode);
                for (int i = 0; i < stmts.size() - 1; ++i) {
                    assocStmt(lastBytecode, stmts.get(i));
                }
                auxiliary.forEach(stmt -> assocStmt(lastBytecode, stmt));
                assocStmt(lastBytecode, stmts.get(stmts.size() - 1));
            } else {
                auxiliary.forEach(stmt -> assocStmt(lastBytecode, stmt));
            }
        }
    }

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

    private void performStackOp(Stack<StackItem> stack, int opcode) {
        switch (opcode) {
            case Opcodes.POP -> popToEffect(stack);
            case Opcodes.POP2 -> {
                popToEffect(stack);
                popToEffect(stack);
            }
            case Opcodes.DUP -> {
                StackItem item = popStack(stack);
                Exp e = item.e();
                assert ! (e instanceof Top);
                liftToVar(item);
                stack.push(item);
                stack.push(item);
            }
            case Opcodes.DUP2 -> dup(stack, 2, 0);
            case Opcodes.DUP_X1 -> dup(stack, 1, 1);
            case Opcodes.DUP_X2 -> dup(stack, 1, 2);
            case Opcodes.DUP2_X1 -> dup(stack, 2, 1);
            case Opcodes.DUP2_X2 -> dup(stack, 2, 2);
            case Opcodes.SWAP -> {
                // swap can only be used when v1 and v2 are both category 1 c.t.
                int top = stack.size() - 1;
                StackItem e1 = popStack(stack);
                StackItem e2 = popStack(stack);
                assert ! (e1.e() instanceof Top) && ! (e2.e() instanceof Top);
                stack.push(e1);
                stack.push(e2);
            }
            default -> throw new UnsupportedOperationException();
        }
    }

    private void emitSSAPhisForLocal(BytecodeBlock block) {
        assert USE_SSA;
        // should have at least one instruction
        AbstractInsnNode first = block.instr().get(0);
        splitting.visitLivePhis(block, (phi) -> {
            Var phiVar = manager.getTempVar();
            Var origin = manager.getLocal(phi.getVar());
            PhiExp phiExp = new PhiExp();
            reachVars[phi.getDUIndex()] = phiVar;
            PhiStmt phiStmt = new PhiStmt(origin, phiVar, phiExp);
            varSSAInfo.setNonSSA(phiVar);
            assocStmt(first, phiStmt);
            phi.setRealPhi(phiStmt);
            manager.aliasLocal(phiVar, manager.getSlot(origin));
        });
    }

    private BytecodeBlock currentBlock;
    private void buildBlockStmt(BytecodeBlock block) {
        currentBlock = block;
        enter(block.getIndex());
        Stack<StackItem> inStack;
        if (block.getInStack() == null) {
            inStack = getInStack(block);
            block.setInStack(inStack);
        } else {
            assert block == entry;
            inStack = block.getInStack();
        }
        assert inStack != null || block.isCatch();
        assert block.getOutStack() == null;
        Stack<StackItem> nowStack = new Stack<>();
        Iterator<AbstractInsnNode> instr = block.instr().iterator();

        if (USE_SSA && EXPERIMENTAL) {
           emitSSAPhisForLocal(block);
        }
        // skips all non-bytecode insn
        AbstractInsnNode insnNode = instr.next();
        while (insnNode.getOpcode() == -1 && instr.hasNext()) {
            insnNode = instr.next();
            if (insnNode instanceof FrameNode f) {
                block.setFrame(f);
            } else if (insnNode instanceof LineNumberNode l) {
                currentLineNumber = l.line;
            }
        }
        // now, insnNode must be:
        // 1. the first "real" bytecode insn, or
        // 2. the last "fake" bytecode insn
        if (block.isCatch()) {
            if (insnNode.getOpcode() != -1) {
                Var catchVar;
                // insnNode is the first bytecode insn for this block
                // for most cases, this should be a store insn
                // this insn stores the exception object to a local var
                if (insnNode.getOpcode() == Opcodes.ASTORE) {
                    VarInsnNode node = (VarInsnNode) insnNode;
                    if (EXPERIMENTAL) {
                        int rwIndex = visitRW(block.getIndex(), getIndex(node));
                        // a little duplicate, any better way?
                        // see also: storeRWVar
                        catchVar = isFastProcessVar(rwIndex)
                                ? manager.getTempVar()
                                : manager.getLocal(splitting.getRealLocalSlot(rwIndex));
                        reachVars[rwIndex] = catchVar;
                        assocStmt(node, new Catch(catchVar));
                    } else {
                        catchVar = manager.getLocal(node.var);
                        duInfo.addDefBlock(catchVar, currentBlock);
                        assocStmt(node, new Catch(catchVar));
                    }
                } else {
                    // else
                    // * for java source, insn should be POP *
                    // 1. make a catch stmt with temp var
                    // 2. push this temp var onto stack
                    catchVar = manager.getTempVar();
                    if (!EXPERIMENTAL) {
                        duInfo.addDefBlock(catchVar, currentBlock);
                    }
                    assocStmt(insnNode, new Catch(catchVar));
                    pushExp(insnNode, nowStack, catchVar);
                    processInstr(nowStack, insnNode, block);
                }
                List<ClassType> handlerTypes = Objects.requireNonNull(block.getExceptionHandlerTypes());
                if (handlerTypes.size() == 1) {
                    ExpModifier.setType(catchVar, handlerTypes.get(0));
                } else {
                    // let type inference decide the type
                    varSSAInfo.setNonSSA(catchVar);
                }
            }
            // `insnNode.getOpcode() == -1` which means the last bytecode is also synthetic
            // this block is totally empty. Do nothing.
        } else {
            assert inStack != null;
            nowStack.addAll(inStack);
            // process the first bytecode insn
            if (insnNode.getOpcode() != -1) {
                processInstr(nowStack, insnNode, block);
            }
        }

        while (instr.hasNext()) {
            AbstractInsnNode node = instr.next();
            processInstr(nowStack, node, block);
        }

        // Temp fix. Add a nop to represent a block. Used in ssa.
        if (USE_SSA) {
            ensureBlockNotEmpty(block);
        }

        block.setOutStack(nowStack);
        exit(block.getIndex());
    }

    private void ensureBlockNotEmpty(BytecodeBlock block) {
        boolean blockEmpty = true;
        AsmListSlice instr = block.instr();
        int start = instr.getStart();
        for (int i = 0; i < instr.size(); ++i) {
            int current = start + i;
            Stmt stmt = asm2Stmt[current];
            if (stmt != null) {
                blockEmpty = false;
                break;
            }
        }
        if (blockEmpty) {
            asm2Stmt[start + instr.size() - 1] = new Nop();
        }
    }

    private Stack<StackItem> getInStack(BytecodeBlock block) {
        Stack<StackItem> inStack;
        int inEdgeCount = getInEdgeCount(block);
        if (isInEdgeEmpty(block)) {
            inStack = null;
        } else if (inEdgeCount == 1) {
            BytecodeBlock inEdge = getInEdge(block, 0);
            inStack = new Stack<>();
            inStack.addAll(inEdge.getOutStack());
        } else {
            inStack = mergeStack(block, inEdgeCount);
        }
        return inStack;
    }

    private Stack<StackItem> mergeStack(BytecodeBlock block, int inEdgeCount) {
        boolean isLoopHeader = false;
        Stack<StackItem> inStack = null;
        List<List<StackItem>> inExps = new ArrayList<>();
        boolean[] needPhi = null;
        for (int i = 0; i < inEdgeCount; ++i) {
            BytecodeBlock inEdge = getInEdge(block, i);
            if (inEdge.getOutStack() == null) {
                isLoopHeader = true;
                if (inStack != null) {
                    break;
                }
            } else {
                Stack<StackItem> outStack = inEdge.getOutStack();
                if (inStack == null) {
                    // clone the first non-null stack
                    inStack = new Stack<>();
                    inStack.addAll(inEdge.getOutStack());
                    if (isLoopHeader || inStack.isEmpty()) {
                        break;
                    }
                    for (StackItem stackItem : outStack) {
                        List<StackItem> inExp = new ArrayList<>();
                        inExp.add(stackItem);
                        inExps.add(inExp);
                    }
                    needPhi = new boolean[inStack.size()];
                } else {
                    // merge this stack with inStack
                    mergeStackWithInEdge(inEdge, inStack, inExps, needPhi);
                }
            }
        }
        assert inStack != null;
        if (isLoopHeader) {
            for (int i = 0; i < inStack.size(); ++i) {
                StackItem item = inStack.get(i);
                Exp e = item.e();
                if (e instanceof Top) {
                    continue;
                }
                // ignore, add inExps during phi resolving
                inStack.set(i, createNewStackPhiItem(block, i, new ArrayList<>()));
            }
            block.setLoopHeader(true);
        } else {
            for (int i = 0; i < inStack.size(); ++i) {
                if (needPhi[i]) {
                    inStack.set(i, createNewStackPhiItem(block, i, inExps.get(i)));
                }
            }
        }
        return inStack;
    }

    private void mergeStackWithInEdge(BytecodeBlock inEdge, Stack<StackItem> initStack,
                                      List<List<StackItem>> inExps, boolean[] needPhi) {
        Stack<StackItem> currentStack = inEdge.getOutStack();
        assert initStack.size() == currentStack.size();
        for (int j = 0; j < initStack.size(); ++j) {
            StackItem item = initStack.get(j);
            Exp e = item.e();
            StackItem item1 = currentStack.get(j);
            Exp e1 = item1.e();
            if (e instanceof Top) {
                assert e1 instanceof Top;
                continue;
            }
            List<StackItem> inExp = inExps.get(j);
            inExp.add(item1);
            assert !(e1 instanceof Top);
            needPhi[j] = needPhi[j] || e != e1;
        }
    }

    private StackItem createNewStackPhiItem(BytecodeBlock block, int index, List<StackItem> inExp) {
        StackPhi phi = new StackPhi(index, inExp, block);
        phi.setVar(manager.getTempVar());
        varSSAInfo.setNonSSA(phi.getVar());
        phiList.add(phi);
        return new StackItem(phi, null);
    }

    private void addLocalPhiInDefs(BytecodeBlock bb) {
        splitting.visitLivePhis(bb, (phi) ->  {
            PhiStmt realPhi = (PhiStmt) phi.getRealPhi();
            assert realPhi != null;
            PhiExp phiExp = realPhi.getRValue();
            for (int i = 0; i < phi.getInDefs().size(); ++i) {
                int defIndex = phi.getInDefs().get(i);
                Var v = reachVars[defIndex];
                phiExp.addUseAndCorrespondingBlocks(v, phi.getInBlocks().get(i));
            }
        });
    }

    private void solveAllPhiAndOutput() {
        for (BytecodeBlock bb : blockSortedList) {
            fillInLoopHeaderStackPhis(bb);
            if (USE_SSA && EXPERIMENTAL) {
                addLocalPhiInDefs(bb);
            }
        }
        propagatePhiUsed();
        resolveStackPhi();
        for (BytecodeBlock bb : blockSortedList) {
            // unreachable
            if (bb.getOutStack() == null) {
                continue;
            }
            if (!USE_SSA) {
                if (stackMergeStmts.has(bb.getIndex())) {
                    List<Stmt> stmts = stackMergeStmts.get(bb.getIndex());
                    appendStackMergeStmts(bb, stmts);
                }
            }
            outputIR(bb);
        }
    }

    private void propagatePhiUsed() {
        for (StackPhi phi : phiList) {
            if (phi.used) {
                setStackPhiUsed(phi);
            }
        }
    }

    private void setStackPhiUsed(StackPhi phi) {
        for (StackItem item : phi.getNodes()) {
            Exp e = item.e();
            if (e instanceof StackPhi phi1) {
                if (!phi1.used) {
                    phi1.used = true;
                    setStackPhiUsed(phi1);
                }
            }
        }
    }

    private void resolveStackPhi() {
        if (!USE_SSA) {
            stackMergeStmts = new SparseArray<>(blockSortedList.size()) {
                @Override
                protected List<Stmt> createInstance() {
                    return new ArrayList<>();
                }
            };
            for (StackPhi phi : phiList) {
                if (phi.getWriteOutVar() != null) continue;
                boolean hasCriticalInEdge = false;
                BytecodeBlock block = phi.createPos;
                for (int i = 0; i < getInEdgeCount(block); ++i) {
                    int pred = g.getInEdge(block.getIndex(), i);
                    if (g.getOutEdgesCount(pred) > 1) {
                        hasCriticalInEdge = true;
                        break;
                    }
                }
                for (StackItem item : block.getInStack()) {
                    Exp e = item.originalExp();
                    if (e instanceof StackPhi phi1) {
                        if (phi1.getWriteOutVar() != null) continue;
                        boolean useWorseSolution = hasCriticalInEdge && getInEdgeCount(block) != 0 && phi1.used;
                        Var writeOut = useWorseSolution ? manager.getTempVar() : phi1.getVar();
                        varSSAInfo.setNonSSA(writeOut);
                        if (useWorseSolution) {
                            // add `v = writeOut` before any definition (first instruction) in create pos
                            BytecodeBlock createPos = phi1.createPos;
                            addToBlockHead(createPos, getAssignStmt(phi1.getVar(), writeOut));
                        }
                        phi1.setWriteOutVar(writeOut);
                    }
                }
            }
            for (StackPhi phi : phiList) {
                if (phi.getWriteOutVar() == null || phi.resolved) continue;
                resolveStackPhi(phi);
            }
        } else {
            // emit phi stmts for stack variable
            for (StackPhi phi : phiList) {
                BytecodeBlock block = phi.createPos;
                // insert phi node in the first instruction
                PhiExp phiExp = new PhiExp();
                int unreachableOffset = 0;
                for (int i = 0; i < getInEdgeCount(block); ++i) {
                    if (getInEdge(block, i).getOutStack() == null) {
                        unreachableOffset++;
                        continue;
                    }
                    StackItem item = phi.getNodes().get(i - unreachableOffset);
                    liftToVar(item);
                    phiExp.addUseAndCorrespondingBlocks(item.var(), getInEdge(block, i));
                }
                PhiStmt phiStmt = new PhiStmt(phi.getVar(), phi.getVar(), phiExp);
                phi.setWriteOutVar(phi.getVar());
                addToBlockHead(block, phiStmt);
                phi.resolved = true;
            }
        }
    }

    private void addToBlockHead(BytecodeBlock block, Stmt stmt) {
        AbstractInsnNode first = block.instr().get(0);
        assocStmt(first, stmt);
    }

    private SparseArray<List<Stmt>> stackMergeStmts;

    private void resolveStackPhi(StackPhi phi) {
        assert !phi.resolved;
        int unreachableOffset = 0;
        Var writeOut = phi.getWriteOutVar();
        for (int i = 0; i < phi.getNodes().size(); ++i) {
            StackItem item = phi.getNodes().get(i);
            BytecodeBlock inEdge = getInEdge(phi.createPos, i + unreachableOffset);
            if (inEdge.getOutStack() == null) {
                unreachableOffset++;
                continue;
            }
            List<Stmt> stmts = stackMergeStmts.get(inEdge.getIndex());
            Exp e = item.e();
            if (e instanceof StackPhi phi1) {
                e = phi1.getVar();
            }
            if (e == writeOut) {
                continue;
            }
            if (maySideEffect(e) || phi.used) {
                stmts.add(getAssignStmt(writeOut, e));
            }
        }
        phi.resolved = true;
    }

    private void fillInLoopHeaderStackPhis(BytecodeBlock current) {
        if (current.isLoopHeader()) {
            Stack<StackItem> inStack = current.getInStack();
            for (int i = 0; i < getInEdgeCount(current); ++i) {
                BytecodeBlock outEdge = getInEdge(current, i);
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

    private void outputIR(BytecodeBlock block) {
        List<Stmt> blockStmt = block.getStmts();
        AsmListSlice instr = block.instr();
        int counter = 0;
        int start = instr.getStart();
        for (int i = 0; i < instr.size(); ++i) {
            int current = start + i;
            Stmt stmt = asm2Stmt[current];
            if (stmt != null) {
                blockStmt.add(stmt);
            }

            List<Stmt> stmts = auxiliaryStmts.get(current);
            if (stmts != null) {
                blockStmt.addAll(stmts);
            }
        }
        if (block.isCatch() && USE_SSA && EXPERIMENTAL) {
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

    private void processInstr(Stack<StackItem> nowStack, AbstractInsnNode node, BytecodeBlock block) {
        if (node instanceof VarInsnNode varNode) {
            switch (varNode.getOpcode()) {
                case Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD -> {
                    if (!EXPERIMENTAL) {
                        pushExp(node, nowStack, manager.getLocal(varNode.var));
                    } else {
                        int rwIndex = visitRW(block.getIndex(), getIndex(varNode));
                        Var v = getRWVar(rwIndex, varNode.var, node);
                        pushExp(node, nowStack, v);
                    }
                }
                case Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE ->
                        storeExp(varNode, nowStack, block);
                default -> // we can never reach here, JSRInlineAdapter should eliminate all rets
                        throw new UnsupportedOperationException();
            }
        } else if (node instanceof InsnNode insnNode) {
            int opcode = insnNode.getOpcode();
            if (opcode == Opcodes.NOP) {
                return;
            } else if (opcode == Opcodes.ARRAYLENGTH) {
                pushExp(node, nowStack, new ArrayLengthExp(popVar(nowStack)));
            } else if (opcode == Opcodes.ATHROW) {
                throwException(insnNode, nowStack);
            } else if (opcode == Opcodes.MONITORENTER) {
                Var obj = popVar(nowStack);
                assocStmt(node, new Monitor(Monitor.Op.ENTER, obj));
            } else if (opcode == Opcodes.MONITOREXIT) {
                Var obj = popVar(nowStack);
                assocStmt(node, new Monitor(Monitor.Op.EXIT, obj));
            } else if (isBinaryInsn(opcode)) {
                pushExp(node, nowStack, getBinaryExp(nowStack, opcode));
            } else if (isReturnInsn(opcode)) {
                returnExp(nowStack, insnNode);
            } else if (isConstInsn(opcode)) {
                pushConst(node, nowStack, getConstValue(insnNode));
            } else if (isPrimCastInsn(opcode)) {
                pushExp(node, nowStack, getCastExp(nowStack, opcode));
            } else if (isNegInsn(opcode)) {
                Var v1 = popVar(nowStack);
                pushExp(node, nowStack, new NegExp(v1));
            } else if (isStackInsn(opcode)) {
                performStackOp(nowStack, opcode);
            } else if (isArrayLoadInsn(opcode)) {
                ArrayAccess access = getArrayAccess(nowStack);
                pushExp(node, nowStack, access);
            } else if (isArrayStoreInsn(opcode)) {
                Var value = popVar(nowStack);
                ArrayAccess access = getArrayAccess(nowStack);
                storeExp(node, access, value);
            } else {
                throw new UnsupportedOperationException();
            }
        } else if (node instanceof JumpInsnNode jump) {
            if (jump.getOpcode() == Opcodes.GOTO) {
                assocStmt(jump, new Goto());
            } else {
                ConditionExp cond = getIfExp(nowStack, jump.getOpcode());
                assocStmt(jump, new If(cond));
            }
        } else if (node instanceof LdcInsnNode ldc) {
            pushConst(node, nowStack, fromObject(ldc.cst));
        } else if (node instanceof TypeInsnNode typeNode) {
            int opcode = typeNode.getOpcode();
            ReferenceType type = BuildContext.get().fromAsmInternalName(typeNode.desc);
            if (opcode == Opcodes.CHECKCAST) {
                pushExp(node, nowStack, getCastExp(nowStack, type));
            } else if (opcode == Opcodes.NEW) {
                pushExp(node, nowStack, new NewInstance((ClassType) type));
            } else if (opcode == Opcodes.ANEWARRAY) {
                Var length = popVar(nowStack);
                int dims = 1;
                Type base;
                if (type instanceof ArrayType arrayType) {
                    dims += arrayType.dimensions();
                    base = arrayType.baseType();
                } else {
                    base = type;
                }
                ArrayType arrayType = BuildContext.get().getTypeSystem().getArrayType(base, dims);
                pushExp(node, nowStack, new NewArray(arrayType, length));
            } else if (opcode == Opcodes.INSTANCEOF) {
                Var obj = popVar(nowStack);
                pushExp(node, nowStack, new InstanceOfExp(obj, type));
            } else {
                throw new UnsupportedOperationException();
            }
        } else if (node instanceof IntInsnNode intNode) {
            int opcode = intNode.getOpcode();
            if (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) {
                pushConst(node, nowStack, IntLiteral.get(intNode.operand));
            } else if (opcode == Opcodes.NEWARRAY) {
                PrimitiveType base = switch (intNode.operand) {
                    case 4 -> BOOLEAN;
                    case 5 -> CHAR;
                    case 6 -> FLOAT;
                    case 7 -> DOUBLE;
                    case 8 -> BYTE;
                    case 9 -> SHORT;
                    case 10 -> INT;
                    case 11 -> LONG;
                    default -> throw new IllegalArgumentException();
                };
                ArrayType arrayType = BuildContext.get().getTypeSystem().getArrayType(base, 1);
                Var length = popVar(nowStack);
                pushExp(node, nowStack, new NewArray(arrayType, length));
            } else {
                assert false;
            }
        } else if (node instanceof FieldInsnNode fieldInsnNode) {
            int opcode = fieldInsnNode.getOpcode();
            ClassType owner = (ClassType)
                    BuildContext.get().fromAsmInternalName(fieldInsnNode.owner);
            Type type = BuildContext.get().fromAsmType(fieldInsnNode.desc);
            String name = fieldInsnNode.name;
            FieldRef ref = FieldRef.get(owner.getJClass(), name, type,
                    opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC);
            switch (opcode) {
                case Opcodes.GETSTATIC -> pushExp(node, nowStack, new StaticFieldAccess(ref));
                case Opcodes.GETFIELD -> {
                    Var v1 = popVar(nowStack);
                    pushExp(node, nowStack, new InstanceFieldAccess(ref, v1));
                }
                case Opcodes.PUTSTATIC -> {
                    FieldAccess access = new StaticFieldAccess(ref);
                    Var v1 = popVar(nowStack);
                    storeExp(node, access, v1);
                }
                case Opcodes.PUTFIELD -> {
                    Var value = popVar(nowStack);
                    Var base = popVar(nowStack);
                    FieldAccess access = new InstanceFieldAccess(ref, base);
                    storeExp(node, access, value);
                }
                default -> throw new UnsupportedOperationException();
            }
        } else if (node instanceof MethodInsnNode methodInsnNode) {
            InvokeExp exp = getInvokeExp(methodInsnNode, nowStack);
            pushExp(node, nowStack, exp);
            if (exp.getType() == VoidType.VOID) {
                popToEffect(nowStack);
            }
        } else if (node instanceof MultiANewArrayInsnNode newArrayInsnNode) {
            Type type = BuildContext.get().fromAsmType(newArrayInsnNode.desc);
            assert type instanceof ArrayType;

            List<Var> lengths = new ArrayList<>();
            // ..., count1, [count2, ...] ->
            for (int i = 0; i < newArrayInsnNode.dims; ++i) {
                lengths.add(popVar(nowStack));
            }
            Collections.reverse(lengths);

            pushExp(node, nowStack, new NewMultiArray((ArrayType) type, lengths));
        } else if (node instanceof IincInsnNode inc) {
            if (EXPERIMENTAL) {
                int use = visitRW(block.getIndex(), getIndex(inc));
                int def = visitRW(block.getIndex(), getIndex(inc));
                pushConst(node, nowStack, IntLiteral.get(inc.incr));
                Var cst = popVar(nowStack);
                Var v = getRWVar(use, inc.var, node);
                pushExp(inc, nowStack, new ArithmeticExp(ArithmeticExp.Op.ADD, v, cst));
                storeRWVar(def, inc.var, inc, block, nowStack);
            } else {
                pushConst(node, nowStack, IntLiteral.get(inc.incr));
                Var cst = popVar(nowStack);
                Var v = manager.getLocal(inc.var);
                pushExp(inc, nowStack, new ArithmeticExp(ArithmeticExp.Op.ADD, v, cst));
                storeExp(inc, v, nowStack, block);
            }
        } else if (node instanceof InvokeDynamicInsnNode invokeDynamicInsnNode) {
            MethodHandle handle = fromAsmHandle(invokeDynamicInsnNode.bsm);
            List<Literal> bootArgs = Arrays.stream(invokeDynamicInsnNode.bsmArgs)
                    .map(Utils::fromObject).toList();
            assert handle.isMethodRef();
            Pair<List<Type>, Type> paramRets =
                    BuildContext.get().fromAsmMethodType(invokeDynamicInsnNode.desc);
            List<Var> args = new ArrayList<>();
            for (int i = 0; i < paramRets.first().size(); ++i) {
                args.add(popVar(nowStack));
            }
            Collections.reverse(args);
            pushExp(node, nowStack, new InvokeDynamic(
                    handle,
                    handle.getMethodRef(),
                    invokeDynamicInsnNode.name,
                    MethodType.get(paramRets.first(), paramRets.second()),
                    bootArgs,
                    args));

        } else if (node instanceof TableSwitchInsnNode switchInsnNode) {
            Var v = popVar(nowStack);
            assocStmt(node, new TableSwitch(v, switchInsnNode.min, switchInsnNode.max));
        } else if (node instanceof LookupSwitchInsnNode lookupSwitchInsnNode) {
            Var v = popVar(nowStack);
            assocStmt(node, new LookupSwitch(v, lookupSwitchInsnNode.keys));
        } else if (node instanceof LabelNode || node instanceof FrameNode) {
            // do nothing
            return;
        } else if (node instanceof LineNumberNode lineNumberNode) {
            this.currentLineNumber = lineNumberNode.line;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private BytecodeBlock[] idx2Block;

    private BytecodeBlock getBlockFromLabel(LabelNode node) {
        return idx2Block[getIndex(node)];
    }

    private void processEdge(BytecodeBlock now, LabelNode target) {
        int index = getIndex(target);
        BytecodeBlock b = idx2Block[index];
        assert b != null;
        if (b.getIndex() == -1) {
            // very unlikely, but possible
            b = searchForValidBlock(target);
        }
        g.addEdge(now.getIndex(), b.getIndex());
    }

    private void processEdges(BytecodeBlock now, List<LabelNode> targets) {
        for (LabelNode target : targets) {
            processEdge(now, target);
        }
    }

    private int rwCount;
    private int[] rwTable;
    private FastVarSplitting<BytecodeBlock> splitting;
    private Var[] reachVars;
    private void writeRwTable(int[] table, int index, int var, boolean read) {
        rwCount++;
        assert var < (1 << 29);
        int rwFlag = read ? 1 << 29 : 1 << 30;
        table[index] = table[index] | var | rwFlag;
    }


    private int getParamWriteSize() {
        int curr = method.isStatic() ? 0 : 1;
        for (int i = 0; i < method.getParamTypes().size(); ++i) {
            Type type = method.getParamTypes().get(i);
            if (Utils.isTwoWord(type)) {
                curr += 2;
            } else {
                curr += 1;
            }
        }
        return curr;
    }

    int[] start;
    int[] end;
    int[] rwToIndex;
    int currRw;
    int paramWrite;
    private void enter(int block) {
        if (EXPERIMENTAL) {
            currRw = start[block];
        }
    }

    private int visitRW(int block, int index) {
        assert rwToIndex[currRw] == index;
        assert currRw < end[block];
        return currRw++;
    }

    private void exit(int block) {
        if (EXPERIMENTAL) {
            assert currRw == end[block];
        }
    }

    private void postProcess() {
        paramWrite = getParamWriteSize();
        rwToIndex = new int[rwCount];
        BytecodeBlock[] rwToBlock = new BytecodeBlock[rwCount];
        int counter = 0;
        IndexedGraph<BytecodeBlock> graph = g;
        BytecodeBlock entry = graph.getEntry();
        start = new int[graph.size()];
        end = new int[graph.size()];

        int maxLocal = source.maxLocals;
        SparseArray<List<BytecodeBlock>> defBlocks = new SparseArray<>(maxLocal) {
            @Override
            protected List<BytecodeBlock> createInstance() {
                return new ArrayList<>();
            }
        };

        for (int i = 0; i < paramWrite; ++i) {
            rwToIndex[counter] = -1;
            rwToBlock[counter] = entry;
            counter++;
        }

        for (int n = 0; n < graph.size(); ++n) {
            BytecodeBlock curr = graph.getNode(n);
            start[curr.getIndex()] = counter;
            int size = curr.instr().size();
            int start1 = curr.instr().getStart();
            for (int j = 0; j < size; ++j) {
                int i = j + start1;
                int rw = rwTable[i];
                if (rw != 0) {
                    int var = rw & ((1 << 29) - 1);
                    boolean read = (rw & (1 << 29)) != 0;
                    boolean write = (rw & (1 << 30)) != 0;
                    if (read) {
                        rwToBlock[counter] = curr;
                        rwToIndex[counter++] = i;
                    }
                    if (write) {
                        rwToBlock[counter] = curr;
                        rwToIndex[counter++] = i;
                        defBlocks.get(var).add(curr);
                    }
                }
            }
            end[curr.getIndex()] = counter;
        }


        int finalCounter = counter;
        GenericDUInfo<BytecodeBlock> genericDUInfo = new GenericDUInfo<>() {
            @Override
            public List<BytecodeBlock> getDefBlock(int v) {
                return defBlocks.get(v);
            }

            @Override
            public int getMaxDuIndex() {
                return finalCounter;
            }

            @Override
            public void visit(BytecodeBlock block, DUVisitor visitor) {
                int start1 = start[block.getIndex()];
                int end1 = end[block.getIndex()];
                for (int i = start1; i < end1;) {
                    int index = rwToIndex[i];
                    int rw = rwTable[index];
                    int var = rw & ((1 << 29) - 1);
                    boolean read = (rw & (1 << 29)) != 0;
                    boolean write = (rw & (1 << 30)) != 0;
                    // careful: the order of visit is important
                    // and iinc can both read and write
                    if (read) {
                        visitor.visit(i, OccurType.USE, var);
                        i++;
                    }
                    // don't use `else if`, iinc can both read and write
                    if (write) {
                        visitor.visit(i, OccurType.DEF, var);
                        i++;
                    }
                }
            }

            @Override
            public BytecodeBlock getBlock(int index) {
                return rwToBlock[index];
            }

            @Override
            public int getParamSize() {
                return paramWrite;
            }
        };

        splitting = new FastVarSplitting<>(graph, maxLocal, genericDUInfo, USE_SSA, dom);
        splitting.build();
        reachVars = new Var[splitting.getMaxDUCount()];
        if (!USE_SSA) {
            manager.enlargeLocal(splitting.getRealLocalCount(), splitting.getVarMappingTable());
        }
        // ensure all params is defined at beginning
        for (int i = 0; i < paramWrite; ++i) {
            if (isFastProcessVar(i)) {
                reachVars[i] = manager.getLocal(i);
                Var current = reachVars[i];
                if (splitting.canFastProcess(i)) {
                    varSSAInfo.setSSA(current);
                } else {
                    varSSAInfo.setNonSSA(current);
                }
            }
        }
    }

    /**
     * Build CFG from ASM instructions.
     * <p>
     *     The {@code blockSortedList} is constructed,
     *     For every block, the block index is set to its pos in this list.
     *     i.e., forall b, {@code b.getIndex() == blockSortedList.indexOf(b)}
     * </p>
     * <p>
     *     Current implementation takes a 3-step solution
     *     <ol>
     *     <li>build a block for each instruction if needed</li>
     *     <li>remove the empty block and add the fall-through edges</li>
     *     <li>add other edges</li>
     *     </ol>
     *     If asm library can generate clean insnlist ("clean" means there is no
     *     empty block), then step 2 and 3 can be merged.
     * </p>
     *
     * <p>TODO: optimize block construction && edge adding</p>
     */
    private BytecodeGraph g;
    private void buildCFG() {
        rwCount = getParamWriteSize();
        int size = source.instructions.size();
        idx2Block = new BytecodeBlock[size];
        boolean[] fallThroughTable = new boolean[size];
        Arrays.fill(fallThroughTable, true);
        rwTable = new int[size];
        AbstractInsnNode begin = source.instructions.getFirst();
        if (begin == null) {
            return;
        }

        assert getIndex(begin) == 0;
        idx2Block[0] = getBlock(begin);
        this.entry = idx2Block[0];

        for (TryCatchBlockNode now : source.tryCatchBlocks) {
            getBlock(now.start);
            getBlock(now.end);
            getBlock(now.handler);
        }

        FlattenExceptionTable fet = new FlattenExceptionTable(source);
        boolean inTry = false;
        Pair<int[], Integer> exceptionSwitchesPair = fet.buildExceptionSwitches();
        int[] trySwitch = exceptionSwitchesPair.first();
        int trySwitchSize = exceptionSwitchesPair.second();
        int trySwitchIndex = 0;
        for (int i = 0; i < size; ++i) {
            while (trySwitchIndex < trySwitchSize && trySwitch[trySwitchIndex] == i) {
                inTry = !inTry;
                trySwitchIndex++;
            }
            AbstractInsnNode now = source.instructions.get(i);
            boolean needNoBlock = true;
            boolean splitBefore = false;
            if (now instanceof JumpInsnNode jmp) {
                getBlock(jmp.label);
                needNoBlock = false;
                fallThroughTable[i] = jmp.getOpcode() != Opcodes.GOTO;
            } else if (now instanceof LookupSwitchInsnNode lookup) {
                getBlock(lookup.dflt);
                for (LabelNode label : lookup.labels) {
                    getBlock(label);
                }
                needNoBlock = false;
                fallThroughTable[i] = false;
            } else if (now instanceof TableSwitchInsnNode table) {
                getBlock(table.dflt);
                for (LabelNode label : table.labels) {
                    getBlock(label);
                }
                needNoBlock = false;
                fallThroughTable[i] = false;
            } else {
                // inlined from Util::isReturnOrThrow
                // this checking may not be stable,
                // if upgrade ASM version, should check here
                if (now instanceof InsnNode insnNode) {
                    int opcode = insnNode.getOpcode();
                    if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) ||
                            opcode == Opcodes.ATHROW) {
                        needNoBlock = false;
                        fallThroughTable[i] = false;
                    }
                } else if (now instanceof VarInsnNode varNode) {
                    if (inTry && isVarStore(varNode)) {
                        splitBefore = true;
                    }
                    if (EXPERIMENTAL) {
                        writeRwTable(rwTable, i, varNode.var, !isVarStore(varNode));
                    }
                } else if (now instanceof IincInsnNode iincInsnNode) {
                    if (inTry) {
                        splitBefore = true;
                    }
                    if (EXPERIMENTAL) {
                        writeRwTable(rwTable, i, iincInsnNode.var, true);
                        writeRwTable(rwTable, i, iincInsnNode.var, false);
                    }
                }
            }

            if (!needNoBlock) {
                AbstractInsnNode next = now.getNext();
                if (next != null) {
                    getBlock(next);
                }
            }
            if (splitBefore) {
                getBlock(now);
            }
        }

        this.blockSortedList = new ArrayList<>(size / 4);
        g = new BytecodeGraph(maxBlockCounter);

        AbstractInsnNode[] edgeInsn = new AbstractInsnNode[size];
        BytecodeBlock current = idx2Block[0];
        assert current != null;
        int start = 0;
        for (int i = 1; i < size; ++i) {
            if (idx2Block[i] != null) {
                int end = i;
                AbstractInsnNode edge = source.instructions.get(end - 1);
                // check for empty
                // TODO: are there any better way to do that?
                for (int curr = end - 1; curr >= start; --curr) {
                    edge = source.instructions.get(curr);
                    if (edge.getOpcode() != -1) {
                        break;
                    }
                }
                if (edge.getOpcode() == -1) {
                    // empty block
                    BytecodeBlock before = idx2Block[start];
                    idx2Block[start] = idx2Block[i];
                    current = idx2Block[i];
                } else {
                    // process current
                    int counter = blockSortedList.size();
                    current.setIndex(counter);
                    edgeInsn[counter] = edge;
                    blockSortedList.add(current);
                    current.setInstr(new AsmListSlice(source.instructions, start, end));
                    // update and post-processing
                    boolean fallThrough = fallThroughTable[end - 1];
                    BytecodeBlock prev = current;
                    current = idx2Block[i];
                    start = i;
                    if (fallThrough) {
                        // prev.getIndex() must be counter
                        g.addEdge(counter, counter + 1);
                    }
                }
            }
        }
        // the last block is not processed
        // but for some cases, it is an emtpy block
        // first check for that
        boolean emtpyLast = start == size - 1 && source.instructions.getLast().getOpcode() == -1;
        if (!emtpyLast) {
            int counter = blockSortedList.size();
            current.setIndex(counter);
            edgeInsn[counter] = source.instructions.getLast();
            blockSortedList.add(current);
            current.setInstr(new AsmListSlice(source.instructions, start, size));
        }
        for (int i = 0; i < blockSortedList.size(); ++i) {
            AbstractInsnNode insn = edgeInsn[i];
            if (insn instanceof JumpInsnNode jmp) {
                BytecodeBlock bb = blockSortedList.get(i);
                processEdge(bb, jmp.label);
            } else if (insn instanceof LookupSwitchInsnNode lookup) {
                BytecodeBlock bb = blockSortedList.get(i);
                processEdges(bb, lookup.labels);
                processEdge(bb, lookup.dflt);
            } else if (insn instanceof TableSwitchInsnNode table) {
                BytecodeBlock bb = blockSortedList.get(i);
                processEdges(bb, table.labels);
                processEdge(bb, table.dflt);
            }
        }

        addExceptionEdges();
        g.setEntry(entry);
        g.setBlockSortedList(blockSortedList);

        dom = new Dominator<>(g);
        if (EXPERIMENTAL) {
            StageTimer.getInstance().endTypelessIR();
            StageTimer.getInstance().startSplitting();
            postProcess();
            StageTimer.getInstance().endSplitting();
            StageTimer.getInstance().startTypelessIR();
        }
    }

    private void addExceptionEdges() {
        for (TryCatchBlockNode now : source.tryCatchBlocks) {
            BytecodeBlock handler = searchForValidBlock(now.handler);
            BytecodeBlock start = searchForValidBlock(now.start);
            int end = searchForValidBlockOrEnd(now.end);
            for (int i = start.getIndex(); i < end; ++i) {
                g.addExceptionEdge(i, handler.getIndex());
            }
            handler.addExceptionHandlerType(fromExceptionType(now.type));
        }
    }

    private ClassType fromExceptionType(String internalName) {
        if (internalName == null) {
            return Utils.getThrowable();
        } else {
            ReferenceType r = BuildContext.get().fromAsmInternalName(internalName);
            if (r instanceof ClassType c) {
                return c;
            } else {
                throw new UnsupportedOperationException("Unsupported exception type: " + r);
            }
        }
    }

    private BytecodeBlock getBlock(AbstractInsnNode label) {
        int idx = getIndex(label);
        if (idx2Block[idx] == null) {
            LabelNode labelNode = (label instanceof LabelNode)
                    ? (LabelNode) label
                    : createNewLabel(label);
            idx2Block[idx] = createNewBlock(labelNode);
        }
        return idx2Block[idx];
    }

    private BytecodeBlock searchForValidBlock(AbstractInsnNode label) {
        int idx = getIndex(label);
        while (idx2Block[idx] == null || idx2Block[idx].getIndex() == -1) {
            idx++;
        }
        return idx2Block[idx];
    }

    private int searchForValidBlockOrEnd(AbstractInsnNode label) {
        int idx = getIndex(label);
        int maxSize = idx2Block.length;
        while (idx < maxSize) {
            if (idx2Block[idx] != null && idx2Block[idx].getIndex() != -1) {
                return idx2Block[idx].getIndex();
            }
            idx++;
        }
        return blockSortedList.size();
    }

    private int maxBlockCounter = 0;
    private BytecodeBlock createNewBlock(LabelNode label) {
        maxBlockCounter++;
        return new BytecodeBlock(label);
    }

    // avoid creating outer reference, use a static method
    private static LabelNode createNewLabel(AbstractInsnNode next) {
        return new LabelNode() {
            @Override
            public AbstractInsnNode getNext() {
                return next;
            }
        };
    }

    private boolean canBeTraversedInBytecodeOrder() {
        boolean[] hasInStack = new boolean[blockSortedList.size()];
        hasInStack[0] = true;

        for (int i = 0; i < blockSortedList.size(); ++i) {
            BytecodeBlock bytecodeBlock = blockSortedList.get(i);
            if (!hasInStack[i] && !bytecodeBlock.isCatch()) {
                return false;
            }
            for (int j = 0; j < getOutEdgeCount(bytecodeBlock); ++j) {
                int succ = getOutEdgeIndex(bytecodeBlock, j);
                hasInStack[succ] = true;
            }
        }

        return true;
    }

    private void traverseBlocks() {
        entry.setInStack(new Stack<>());

        int[] postOrder = dom.getPostOrder();
        for (int i = postOrder.length - 1; i >= 0; --i) {
            int current = postOrder[i];
            BytecodeBlock bb = blockSortedList.get(current);
            buildBlockStmt(bb);
        }
        solveAllPhiAndOutput();
    }

    private void setLineNumber() {
        int currentLineNumber = -1;
        for (var insnNode : source.instructions) {
            if (!(insnNode instanceof LabelNode)) {
                if (insnNode instanceof LineNumberNode l) {
                    currentLineNumber = l.line;
                } else {
                    if (currentLineNumber == -1) {
                        logger.atDebug().log("[IR] no line number info, method: " + method);
                        return;
                    }
                    var stmt = asm2Stmt[getIndex(insnNode)];
                    if (stmt != null) {
                        stmt.setLineNumber(currentLineNumber);
                    }

                    var stmts = auxiliaryStmts.get(getIndex(insnNode));
                    if (stmts != null) {
                        for (var s : stmts) {
                            s.setLineNumber(currentLineNumber);
                        }
                    }
                }
            }
        }
    }

    public List<Pair<List<BytecodeBlock>, BytecodeBlock>> getTryAndHandlerBlocks() {
        if (this.tryAndHandlerBlocks == null) {
            List<Pair<List<BytecodeBlock>, BytecodeBlock>> result = new ArrayList<>();
            for (var node : source.tryCatchBlocks) {
                List<BytecodeBlock> tryBlocks = new ArrayList<>();
                for (int i = getIndex(node.start); i < getIndex(node.end); ++i) {
                    if (idx2Block[i] != null) {
                        idx2Block[i].setIsInTry();
                        tryBlocks.add(idx2Block[i]);
                    }
                }
                BytecodeBlock handler = getBlockFromLabel(node.handler);
                assert handler != null;
                result.add(new Pair<>(tryBlocks, handler));
            }
            this.tryAndHandlerBlocks = result;
        }

        return this.tryAndHandlerBlocks;
    }

//    /**
//     * Get the blocks that is in an arbitrary path from start to end.
//     * WARNING: before calling this method, caller should have know that {@param start} and {@param end}
//     * are dominators of the result set.
//     * @param start (not null) the forward dominator of the result set.
//     * @param end (not null) the backward dominator of the result set.
//     * @return a list of blocks, and each of the elements is in an arbitrary path from start(inclusive)
//     * to end(exclusive), in bfs order.
//     */
//    private List<BytecodeBlock> getStartToEndBlocks(BytecodeBlock start, BytecodeBlock end) {
//        assert end != null && start != null;
//        Queue<BytecodeBlock> workList = new LinkedList<>();
//        workList.offer(start);
//        List<BytecodeBlock> closure = new ArrayList<>();
//        while (workList.peek() != null) {
//            var bb = workList.poll();
//            if (!closure.contains(bb) && bb != end) {
//                closure.add(bb);
//                for (var succ : bb.outEdges()) {
//                    workList.offer(succ);
//                }
//            }
//        }
//
//        return closure;
//    }

    public IR getIr() {
        return ir;
    }

    int getOutEdgeCount(BytecodeBlock block) {
        return g.getOutEdgesCount(block.getIndex());
    }

    int getInEdgeCount(BytecodeBlock block) {
        return g.getInEdgesCount(block.getIndex());
    }

    int getOutEdgeIndex(BytecodeBlock block, int outIndex) {
        return g.getOutEdge(block.getIndex(), outIndex);
    }

    int getInEdgeIndex(BytecodeBlock block, int inIndex) {
        return g.getInEdge(block.getIndex(), inIndex);
    }

    BytecodeBlock getOutEdge(BytecodeBlock block, int index) {
        int idx = getOutEdgeIndex(block, index);
        return blockSortedList.get(idx);
    }

    BytecodeBlock getInEdge(BytecodeBlock block, int index) {
        int idx = getInEdgeIndex(block, index);
        return blockSortedList.get(idx);
    }

    BytecodeBlock getMergedOutEdge(BytecodeBlock block, int index) {
        int idx = g.getMergedOutEdge(block.getIndex(), index);
        return blockSortedList.get(idx);
    }

    int getMergedOutEdgesCount(BytecodeBlock block) {
        return g.getMergedOutEdgesCount(block.getIndex());
    }

    boolean isInEdgeEmpty(BytecodeBlock block) {
        return getInEdgeCount(block) == 0;
    }

    boolean isOutEdgeEmpty(BytecodeBlock block) {
        return getOutEdgeCount(block) == 0;
    }

    boolean isFastProcessVar(int v) {
        return USE_SSA || splitting.canFastProcess(v);
    }

    int[] getPostOrder() {
        return dom.getPostOrder();
    }

    boolean isUSE_SSA() {
        return USE_SSA;
    }

    Dominator<BytecodeBlock> getDom() {
        return dom;
    }

    BytecodeGraph getGraph() {
        return g;
    }
}
