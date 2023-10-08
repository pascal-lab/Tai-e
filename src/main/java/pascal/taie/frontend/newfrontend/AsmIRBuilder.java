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
import pascal.taie.analysis.dataflow.analysis.LiveVariable;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.CFGBuilder;
import pascal.taie.analysis.graph.cfg.ExtraEdgeAppender;
import pascal.taie.config.AnalysisConfig;
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
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.SwitchStmt;
import pascal.taie.ir.stmt.TableSwitch;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.VoidType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Stream;

import static pascal.taie.frontend.newfrontend.Utils.*;

public class AsmIRBuilder {

    private IR ir;

    final JMethod method;

    private final int classFileVersion;

    private final JSRInlinerAdapter source;

    Map<LabelNode, BytecodeBlock> label2Block;

    List<BytecodeBlock> blockSortedList; // blocks that are sorted in bytecode order.

    private LabelNode entry;

    final VarManager manager;

    private final Map<Exp, AbstractInsnNode> exp2origin;

    final Map<AbstractInsnNode, Stmt> asm2Stmt;

    final Map<AbstractInsnNode, List<Stmt>> auxiliaryStmts;

    private final boolean isEmpty;

    private boolean isFrameUsable;

    private List<Stmt> stmts;

    private List<ExceptionEntry> exceptionEntries;

    private static final Logger logger = LogManager.getLogger();

    public AsmIRBuilder(JMethod method, AsmMethodSource methodSource) {
        this.method = method;
        this.source = methodSource.adapter();
        this.classFileVersion = methodSource.classFileVersion();
        this.isEmpty = source.instructions.size() == 0;
        this.manager = new VarManager(method,
                source.localVariables, source.instructions, source.maxLocals);
        this.asm2Stmt = Maps.newMap(source.instructions.size());
        this.exp2origin = Maps.newMap();
        this.auxiliaryStmts = Maps.newMap();
        this.stmts = new ArrayList<>();
    }

    public void build() {
        // a.analyze()
        if (! isEmpty) {
            StageTimer stageTimer = StageTimer.getInstance();
            stageTimer.startTypelessIR();
            buildCFG();
            this.isFrameUsable = classFileVersion >= Opcodes.V1_6 && checkFrameValid();
            traverseBlocks();
            setLineNumber();
            stageTimer.endTypelessIR();
            if (isFrameUsable()) {
                inferTypeWithFrame();
            } else {
                inferTypeWithoutFrame();
            }
            stageTimer.startTypelessIR();
            makeStmts();
            makeExceptionTable();
            stageTimer.endTypelessIR();
            verify();
            this.ir = getIR();
        }
        // TODO: check how to handle empty method
    }

    void inferTypeWithFrame() {
        StageTimer stageTimer = StageTimer.getInstance();
        stageTimer.startSplitting();
        VarWebSplitter splitter = new VarWebSplitter(this);
        splitter.build();
        stageTimer.endSplitting();
        stageTimer.startTyping();
        TypeInference0 inference = new TypeInference0(this);
        inference.build();
        stageTimer.endTyping();
    }

    void inferTypeWithoutFrame() {
        StageTimer stageTimer = StageTimer.getInstance();
        stageTimer.startSplitting();
        makeStmts();
        makeExceptionTable();
        IR untyped = getIR();
        AnalysisConfig config = AnalysisConfig.of(CFGBuilder.ID,
                "exception", null,
                "dump", false);
        CFGBuilder builder = new CFGBuilder(config);
        CFG<Stmt> cfg = builder.analyze(untyped);
        MultiMap<Stmt, Stmt> exceptionMap = Maps.newMultiMap();
        for (ExceptionEntry entry : exceptionEntries) {
            for (int i = entry.start().getIndex(); i < entry.end().getIndex(); ++i) {
                exceptionMap.put(stmts.get(i), entry.handler());
            }
        }
        untyped.storeResult(CFGBuilder.ID, cfg);
        AnalysisConfig config1 = AnalysisConfig.of(LiveVariable.ID,
                "strongly", false);
        ExtraEdgeAppender.append(cfg, exceptionMap);
        LiveVariable liveVar = new LiveVariable(config1);
        var result = liveVar.analyze(untyped);
        VarWebSplitter splitter = new VarWebSplitter(this, result);
        splitter.build();
        stageTimer.endSplitting();
        stageTimer.startTyping();
        makeExceptionTable();
        TypeInference inference = new TypeInference(this);
        inference.build();
        makeStmts();
        stageTimer.endTyping();
    }

    public BytecodeBlock getEntryBlock() {
        return label2Block.get(entry);
    }

    public boolean isFrameUsable() {
        return isFrameUsable;
    }

    // TODO: optimize
    public boolean checkFrameValid() {
        for (BytecodeBlock block : blockSortedList) {
            if (block.inEdges().size() >= 2 && block.getFrame() == null) {
                return false;
            }

            if (block.outEdges().size() >= 2 &&
                    block.outEdges().stream().allMatch(o -> o.getFrame() == null)) {
                return false;
            }

            if (block.isCatch() && block.getFrame() == null) {
                return false;
            }
        }
        return true;
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
        return this.stmts.get(stmt.getIndex()) == stmt;
    }

    private Stmt getAssignStmt(LValue lValue, Exp e) {
        return Utils.getAssignStmt(method, lValue, e);
    }

    private boolean isDword(AbstractInsnNode node, Exp e) {
        if (e instanceof InvokeExp invokeExp) {
            Type returnType = invokeExp.getType();
            return returnType == PrimitiveType.DOUBLE || returnType == PrimitiveType.LONG;
        } else if (e instanceof LongLiteral || e instanceof DoubleLiteral) {
            return true;
        } else if (e instanceof FieldAccess access) {
            Type fieldType = access.getType();
            return fieldType == PrimitiveType.DOUBLE || fieldType == PrimitiveType.LONG;
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

    private Exp popExp(Stack<Exp> stack) {
        Exp e = stack.pop();
        if (e instanceof Top) {
            Exp e1 = stack.pop();
            assert ! (e1 instanceof Top);
            return e1;
        } else {
            return e;
        }
    }

    private Exp peekExp(Stack<Exp> stack) {
        Exp e = stack.peek();
        if (e instanceof Top) {
            Exp e1 = stack.get(stack.size() - 2);
            assert ! (e1 instanceof Top);
            return e1;
        } else {
            return e;
        }
    }

    private AbstractInsnNode getOrig(Exp e) {
        return exp2origin.get(e);
    }

    private List<Stmt> clearStmt(AbstractInsnNode node) {
        List<Stmt> res = new ArrayList<>();
        if (asm2Stmt.containsKey(node)) {
            res.add(asm2Stmt.get(node));
            asm2Stmt.remove(node);
        }
        if (auxiliaryStmts.containsKey(node)) {
            res.addAll(auxiliaryStmts.get(node));
            auxiliaryStmts.remove(node);
        }
        return res;
    }

    private void assocStmt(AbstractInsnNode node, Stmt stmt) {
        if (! asm2Stmt.containsKey(node)) {
            asm2Stmt.put(node, stmt);
        } else {
            assocListStmt(node, stmt);
        }
    }

    private void assocListStmt(AbstractInsnNode node, Stmt stmt) {
        List<Stmt> aux = auxiliaryStmts.computeIfAbsent(node, (n) -> new ArrayList<>());
        aux.add(stmt);
    }

    private void assocStmt(Exp e, Stmt stmt) {
        assocStmt(exp2origin.get(e), stmt);
    }

    private Var toVar(Exp e) {
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
        AbstractInsnNode orig = getOrig(e);
        if (! asm2Stmt.containsKey(orig)) {
//            logger.atInfo().log("[IR] Multiple expression belonging to one bytecode" + "\n" +
//                                "     It may be an error, you should check IR." + "\n" +
//                                "     In method: " + method.toString());
        }
        Stmt auxStmt = getAssignStmt(v, e);
        assocStmt(orig, auxStmt);
        return v;
    }

    private Var popVar(Stack<Exp> stack) {
        Exp e = popExp(stack);
        if (e instanceof Var v) {
            return v;
        } else {
            return toVar(e);
        }
    }

    private Stmt popToVar(Stack<Exp> stack, Var v) {
        Exp top = popExp(stack);
        // Note: Var . getUses() will return empty set
        ensureStackSafety(stack, e -> e == v || e.getUses().contains(v));
        return getAssignStmt(v, top);
    }

    private void popToEffect(Stack<Exp> stack) {
        // normally, this should only be used to pop a InvokeExp
        Exp e = stack.pop();
        if (e instanceof Top) {
            return;
        } else if (e instanceof InvokeExp invokeExp) {
            assocStmt(e, new Invoke(method, invokeExp));
        } else if (maySideEffect(e)) {
            assocStmt(e, getAssignStmt(manager.getTempVar(), e));
        }
    }

    private void dup(Stack<Exp> stack, int takes, int seps) {
        List<Exp> takesList = new ArrayList<>(takes);
        for (int i = 0; i < takes; ++i) {
            Exp e = stack.pop();
            if (e instanceof Top || e instanceof Var) {
                takesList.add(e);
            } else {
                takesList.add(toVar(e));
            }
        }
        Collections.reverse(takesList);
        List<Exp> sepsList = new ArrayList<>(seps);
        for (int i = 0; i < seps; ++i) {
            sepsList.add(stack.pop());
        }
        Collections.reverse(sepsList);
        stack.addAll(takesList);
        stack.addAll(sepsList);
        stack.addAll(takesList);
    }

    private void ensureStackSafety(Stack<Exp> stack, Function<Exp, Boolean> predicate) {
        for (int i = 0; i < stack.size(); ++i) {
            Exp e = stack.get(i);
            if (e instanceof Top) {
                continue;
            }
            if (predicate.apply(e)) {
                stack.set(i, toVar(e));
            }
        }
    }

    private boolean maySideEffect(Exp e) {
        return !(e instanceof Var);
    }

    private void pushExp(AbstractInsnNode node, Stack<Exp> stack, Exp e) {
        assert ! (e instanceof Top);
        exp2origin.put(e, node);
        ensureStackSafety(stack, this::maySideEffect);
        stack.push(e);
        if (isDword(node, e)) {
            stack.push(Top.Top);
        }
    }

    private void pushConst(AbstractInsnNode node, Stack<Exp> stack, Literal literal) {
        if (manager.peekConstVar(literal)) {
            // note: if this code is reached, and `node` is `Ldc`,
            // pushExp cannot automatically push a `Top`
            pushExp(node, stack, manager.getConstVar(literal));
            // TODO: ugly hack here, try to fix that
            if (node instanceof LdcInsnNode && isDword(node, literal)) {
                stack.push(Top.Top);
            }
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
        BytecodeBlock block = label2Block.get(label);
        if (block == null || block.getStmts().isEmpty()) {
            if (block != null) {
                logger.atWarn().log(method + ", empty block / labels : " + label);
            }
            AbstractInsnNode next = label.getNext();
            if (next instanceof LabelNode labelNode) {
                return getFirstStmt(labelNode);
            } else {
                logger.atTrace().log("[IR] All possible method fail to get a valid stmt for a label" + "\n" +
                                    "     Please check IR of this method: " + method);
                while (! asm2Stmt.containsKey(next)) {
                    next = next.getNext();
                    assert next != null;
                }
                return asm2Stmt.get(next);
            }
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
                gotoStmt.setTarget(first);
            } else if (stmt instanceof If ifStmt) {
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

    private void addStmt(Stmt stmt) {
        stmt.setIndex(stmts.size());
        stmts.add(stmt);
    }

    private void makeStmts() {
        this.stmts = new ArrayList<>();
        for (BytecodeBlock block : blockSortedList) {
            List<Stmt> blockStmts = block.getStmts();
            if (! blockStmts.isEmpty()) {
                blockStmts.forEach(this::addStmt);
                setJumpTargets(block.getLastBytecode(), block.getLastStmt());
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
            Stmt handler = getFirstStmt(node.handler);
            Type expType = getExceptionType(node.type);
            res.add(new ExceptionEntry(start, end, (Catch) handler, (ClassType) expType));
        }
        exceptionEntries = res;
    }

    private Type getExceptionType(String s) {
        String name = s == null ? getThrowable() : s;
        return BuildContext.get().fromAsmInternalName(name);
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

    private boolean isAddInsn(int opcode) { return inRange(opcode, Opcodes.IADD, Opcodes.DADD); }
    private boolean isSubInsn(int opcode) { return inRange(opcode, Opcodes.ISUB, Opcodes.DSUB); }
    private boolean isMulInsn(int opcode) { return inRange(opcode, Opcodes.IMUL, Opcodes.DMUL); }
    private boolean isDivInsn(int opcode) { return inRange(opcode, Opcodes.IDIV, Opcodes.DDIV); }
    private boolean isRemInsn(int opcode) { return inRange(opcode, Opcodes.IREM, Opcodes.DREM); }
    private boolean isNegInsn(int opcode) { return inRange(opcode, Opcodes.INEG, Opcodes.DNEG); }

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

    private ConditionExp getIfExp(Stack<Exp> stack, int opcode) {
        Var v1;
        Var v2;
        if (inRange(opcode, Opcodes.IFEQ, Opcodes.IFLE)) {
            v1 = popVar(stack);
            v2 = manager.getZeroLiteral();
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
            case Opcodes.L2I, Opcodes.F2I, Opcodes.D2I -> PrimitiveType.INT;
            case Opcodes.I2L, Opcodes.F2L, Opcodes.D2L -> PrimitiveType.LONG;
            case Opcodes.I2F, Opcodes.L2F, Opcodes.D2F -> PrimitiveType.FLOAT;
            case Opcodes.I2D, Opcodes.L2D,  Opcodes.F2D -> PrimitiveType.DOUBLE;
            case Opcodes.I2B -> PrimitiveType.BYTE;
            case Opcodes.I2S -> PrimitiveType.SHORT;
            case Opcodes.I2C -> PrimitiveType.CHAR;
            default -> throw new IllegalArgumentException();
        };
    }

    private BinaryExp getBinaryExp(Stack<Exp> stack, int opcode) {
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

    private CastExp getCastExp(Stack<Exp> stack, int opcode) {
        return getCastExp(stack, getCastType(opcode));
    }

    private CastExp getCastExp(Stack<Exp> stack, Type t) {
        Var v1 = popVar(stack);
        return new CastExp(v1, t);
    }

    private InvokeExp getInvokeExp(MethodInsnNode methodInsnNode, Stack<Exp> stack) {
        int opcode = methodInsnNode.getOpcode();
        JClass owner = BuildContext.get().toJClass(methodInsnNode.owner);
        assert owner != null;
        Pair<List<Type>, Type> desc = BuildContext.get().fromAsmMethodType(methodInsnNode.desc);
        String name = methodInsnNode.name;
        boolean isStatic = opcode == Opcodes.INVOKESTATIC;
        MethodRef ref = MethodRef.get(owner, name, desc.first(), desc.second(), isStatic);

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

    private ArrayAccess getArrayAccess(Stack<Exp> nowStack) {
        Var idx = popVar(nowStack);
        Var ref = popVar(nowStack);
        return new ArrayAccess(ref, idx);
    }

    private void storeExp(VarInsnNode varNode, Stack<Exp> stack) {
        int idx = varNode.var;
        Var v = manager.getLocal(idx);
        storeExp(varNode, v, stack);
    }

    private void storeExp(AbstractInsnNode node, Var v, Stack<Exp> stack) {
        Stmt stmt = popToVar(stack, v);
        assocStmt(node, stmt);
    }

    private void storeExp(AbstractInsnNode node, LValue left, RValue right) {
        Stmt stmt = getAssignStmt(left, right);
        assocStmt(node, stmt);
    }

    private void returnExp(Stack<Exp> stack, InsnNode node) {
        int opcode = node.getOpcode();
        if (opcode == Opcodes.RETURN) {
            assocStmt(node, new Return());
        } else {
            Var v = popVar(stack);
            manager.addReturnVar(v);
            assocStmt(node, new Return(v));
        }
    }

    private void throwException(InsnNode node, Stack<Exp> stack) {
        Var v = popVar(stack);
        assocStmt(node, new Throw(v));
    }

    private void mergeStack1(List<Stmt> auxiliary, Stack<Exp> nowStack, Stack<Exp> targetStack) {
        Exp v = targetStack.pop();
        if (v instanceof Top) {
            return;
        }
        assert v instanceof Var: "merge target should be var of top";
        Exp e = peekExp(nowStack);
        if (e == v) {
            popExp(nowStack);
        } else {
            Stmt stmt = popToVar(nowStack, (Var) v);
            auxiliary.add(stmt);
        }
    }

    private void mergeStack(BytecodeBlock bb, Stack<Exp> nowStack, Stack<Exp> target) {
        List<Stmt> auxiliary = new ArrayList<>();
        Stack<Exp> nowStack1 = new Stack<>();
        Stack<Exp> target1 = new Stack<>();
        nowStack1.addAll(nowStack);
        target1.addAll(target);
        while (! nowStack1.isEmpty()) {
            mergeStack1(auxiliary, nowStack1, target1);
        }
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
        assert target1.empty();
    }

    private void performStackOp(Stack<Exp> stack, int opcode) {
        switch (opcode) {
            case Opcodes.POP -> popToEffect(stack);
            case Opcodes.POP2 -> {
                popToEffect(stack);
                popToEffect(stack);
            }
            case Opcodes.DUP -> dup(stack, 1, 0);
            case Opcodes.DUP2 -> dup(stack, 2, 0);
            case Opcodes.DUP_X1 -> dup(stack, 1, 1);
            case Opcodes.DUP_X2 -> dup(stack, 1, 2);
            case Opcodes.DUP2_X1 -> dup(stack, 2, 1);
            case Opcodes.DUP2_X2 -> dup(stack, 2, 2);
            case Opcodes.SWAP -> {
                // swap can only be used when v1 and v2 are both category 1 c.t.
                Exp e1 = stack.pop();
                Exp e2 = stack.pop();
                assert ! (e1 instanceof Top) && ! (e2 instanceof Top);
                stack.push(e1);
                stack.push(e2);
            }
            default -> throw new UnsupportedOperationException();
        }
    }

    private void buildBlockStmt(BytecodeBlock block) {
        manager.clearConstCache();
        Stack<Exp> inStack = block.getInStack();
        Stack<Exp> nowStack = new Stack<>();
        Iterator<AbstractInsnNode> instr = block.instr().iterator();

        if (block.isCatch()) {
            if (instr.hasNext()) {
                AbstractInsnNode insnNode = instr.next();
                if (insnNode.getOpcode() == Opcodes.ASTORE) {
                    VarInsnNode node = (VarInsnNode) insnNode;
                    assocStmt(node, new Catch(manager.getLocal(node.var)));
                } else {
                    // else
                    // * for java source, insn should be POP *
                    // 1. make a catch stmt with temp var
                    // 2. push this temp var onto stack
                    Var v = manager.getTempVar();
                    assocStmt(insnNode, new Catch(v));
                    pushExp(insnNode, nowStack, v);
                    processInstr(nowStack, insnNode);
                }
            }
        } else {
            nowStack.addAll(inStack);
        }

        while (instr.hasNext()) {
            AbstractInsnNode node = instr.next();
            processInstr(nowStack, node);
        }

        // if there is no out edges, it must be a return / throw block
        // do nothing
        // else, perform stack assign merge
        if (!block.outEdges().isEmpty()) {
            if (block.getOutStack() == null) {
                // Web has not been constructed. So all the succs do not have inStack.
                block.setOutStack(regularizeStack(block, nowStack));
            } else {
                Stack<Exp> target = block.getOutStack();
                mergeStack(block, nowStack, target);
            }
        }

        // collect all the stmts associated with this block.
        List<Integer> stmt2Asm = new ArrayList<>();
        for (int i = 0; i < block.instr().size(); ++i) {
            AbstractInsnNode insnNode = block.instr().get(i);
            Stmt stmt = asm2Stmt.get(insnNode);
            if (stmt != null) {
                block.getStmts().add(stmt);
                stmt2Asm.add(i);
            }

            List<Stmt> stmts = auxiliaryStmts.get(insnNode);
            if (stmts != null) {
                block.getStmts().addAll(stmts);
                for (int j = 0; j < stmts.size(); ++j) {
                    stmt2Asm.add(i);
                }
            }
        }
        block.setStmt2Asm(stmt2Asm);
    }

    private void processInstr(Stack<Exp> nowStack, AbstractInsnNode node) {
        if (node instanceof VarInsnNode varNode) {
            switch (varNode.getOpcode()) {
                case Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD ->
                    pushExp(node, nowStack, manager.getLocal(varNode.var));
                case Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE ->
                        storeExp(varNode, nowStack);
                default -> // we can never reach here, JSRInlineAdapter should eliminate all rets
                        throw new UnsupportedOperationException();
            }
        } else if (node instanceof InsnNode insnNode) {
            int opcode = insnNode.getOpcode();
            if (opcode == Opcodes.NOP) {
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
                    case 4 -> PrimitiveType.BOOLEAN;
                    case 5 -> PrimitiveType.CHAR;
                    case 6 -> PrimitiveType.FLOAT;
                    case 7 -> PrimitiveType.DOUBLE;
                    case 8 -> PrimitiveType.BYTE;
                    case 9 -> PrimitiveType.SHORT;
                    case 10 -> PrimitiveType.INT;
                    case 11 -> PrimitiveType.LONG;
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
            // TODO: check why our class hierarchy builder makes owner.getJClass() null
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
            pushConst(node, nowStack, IntLiteral.get(inc.incr));
            Var cst = popVar(nowStack);
            Var v = manager.getLocal(inc.var);
            nowStack.push(new ArithmeticExp(ArithmeticExp.Op.ADD, v, cst));
            storeExp(inc, v, nowStack);
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
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    private Stack<Exp> regularizeStack(BytecodeBlock bb, Stack<Exp> origin) {
        /*
            1. conversion from non-Var Exp to Var,
            2. no the same Vars in a stack.

            The conversion should have effect on the InsnNode that generated the exp.
         */
        Stack<Exp> target = new Stack<>();
        Set<Var> used = Sets.newHybridSet();
        for (Exp e : origin) {
            if (e instanceof Top) {
                target.push(e);
            }
            else if (e instanceof Var v && manager.isTempVar(v) && !used.contains(v)) {
                used.add(v);
                target.push(v);
            } else {
                target.add(manager.getTempVar());
            }
        }
        mergeStack(bb, origin, target);
        return target;
    }

    private void buildCFG() {
        label2Block = Maps.newMap();

        AbstractInsnNode begin = source.instructions.getFirst();
        Queue<LabelNode> queue = new LinkedList<>();
        if (begin == null) {
            return;
        }

        if (begin instanceof LabelNode l) {
            entry = l;
        } else {
            entry = createNewLabel(begin);
        }

        queue.add(entry);

        for (TryCatchBlockNode now : source.tryCatchBlocks) {
            queue.add(now.handler);
            label2Block.put(now.handler, new BytecodeBlock(now.handler, null, getExceptionType(now.type)));
        }

        while (!queue.isEmpty()) {
            LabelNode currentBegin = queue.poll();
            if (isVisited(currentBegin)) {
                continue;
            }

            BytecodeBlock bb = getBlock(currentBegin);
            List<AbstractInsnNode> instr = bb.instr();

            AbstractInsnNode now = currentBegin.getNext();
            while (now != null) {
                if (now instanceof FrameNode frameNode) {
                    bb.setFrame(frameNode);
                }
                else if (! (now instanceof LabelNode) && ! (now instanceof LineNumberNode)) {
                    instr.add(now);
                }

                if (isCFEdge(now)) {
                    break;
                }
                now = now.getNext();
            }
            collectJumpLabels(bb, now).forEach(label -> {
                BytecodeBlock target = getBlock(label);
                target.inEdges().add(bb);
                bb.outEdges().add(target);
                queue.add(label);
            });
            bb.setComplete();
        }

        avoidUnreachableExceptionTable();
        bridgeWronglySeparatedBlocks();
        blockSortedList =
                label2Block.keySet().stream()
                        .sorted((a, b) -> source.instructions.indexOf(a.getNext()) - source.instructions.indexOf(b.getNext()))
                        .map(label -> label2Block.get(label))
                        .toList();

        for (int i = 0; i < blockSortedList.size() - 1; i++) {
            assert !blockSortedList.get(i).equals(blockSortedList.get(i + 1));
        }
        for (var b : label2Block.values()) {
            assert !b.instr().isEmpty();
        }
    }

    private BytecodeBlock getBlock(LabelNode label) {
        return label2Block.computeIfAbsent(label, this::createNewBlock);
    }

    private BytecodeBlock createNewBlock(LabelNode label) {
        return new BytecodeBlock(label, null);
    }

    private boolean isVisited(LabelNode label) {
        return label2Block.containsKey(label) &&
                label2Block.get(label).isComplete();
    }

    private Stream<LabelNode> collectJumpLabels(BytecodeBlock currentBlock, AbstractInsnNode node) {
        if (node == null || isReturn(node) || isThrow(node)) {
            return Stream.empty();
        } else if (node instanceof JumpInsnNode jump) {
            if (node.getOpcode() == Opcodes.GOTO) {
                return Stream.of(jump.label);
            } else {
                return Stream.concat(Stream.of(jump.label),
                        collectFallThrough(currentBlock, node.getNext()));
            }
        } else if (node instanceof LookupSwitchInsnNode lookup) {
            return Stream.concat(Stream.of(lookup.dflt), lookup.labels.stream());
        } else if (node instanceof TableSwitchInsnNode table) {
            return Stream.concat(Stream.of(table.dflt), table.labels.stream());
        } else if (node instanceof LabelNode) {
            return collectFallThrough(currentBlock, node);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private Stream<LabelNode> collectFallThrough(BytecodeBlock currentBlock, AbstractInsnNode node) {
        if (node == null) {
            return Stream.empty();
        } else if (node instanceof LabelNode l) {
            currentBlock.setFallThrough(getBlock(l));
            return Stream.of(l);
        } else {
            LabelNode l = createNewLabel(node);
            BytecodeBlock bb = getBlock(l);
            currentBlock.setFallThrough(bb);
            return Stream.of(l);
        }
    }

    private LabelNode createNewLabel(AbstractInsnNode next) {
        return new LabelNode() {
            @Override
            public AbstractInsnNode getNext() {
                return next;
            }
        };
    }

    /**
     * This function try to correct exception table.
     * There is a very rare case that the start position of an exception entry is unreachable.
     * <br/>
     * <pre>
     * before:
     *           1. goto 3
     *   start-> 2. goto 3 (unreachable)
     *           3. iload
     *           4. istore
     * after:
     *           1. goto 3
     *           2. goto 3
     *   start-> 3. iload (reachable from 1.)
     *           4. istore
     * </pre>
     */
    private void avoidUnreachableExceptionTable() {
        for (TryCatchBlockNode node : source.tryCatchBlocks) {
            LabelNode start = node.start;
            LabelNode startChecker = getNextIfUnreachable(start);
            if (startChecker != start) {
                logger.atWarn().log("[IR] Unreachable exception entry start." + "\n" +
                                    "     In method: " + method);
                node.start = startChecker;
            }
        }
    }

    private LabelNode getNextIfUnreachable(LabelNode labelNode) {
        if (! label2Block.containsKey(labelNode)) {
            return getNextLabel(labelNode);
        } else {
            return labelNode;
        }
    }

    private LabelNode getNextLabel(LabelNode labelNode) {
        AbstractInsnNode next = labelNode.getNext();
        assert next != null;

        do {
            if (next instanceof LabelNode labelNode1 &&
                    label2Block.containsKey(labelNode1)) {
                return labelNode1;
            }
            next = next.getNext();
        } while (next != null);

        return null;
    }

    private final Set<LabelNode> ignoredLabels = new HashSet<>();
    /**
     * Bridge the blocks that a wrongly separated by regarding every LabelNode as an entry of a block.
     * We regard these pairs to be bridged: {(pred, succ) | pred.outEdges = {succ} && succ.inEdges = {pred}}
     * Processes:
     * 1. concat the 2 blocks;
     * 2. delete the entry for the successor in the label2Block map.
     */
    private void bridgeWronglySeparatedBlocks() {
        BytecodeBlock entry = label2Block.get(this.entry);
        Set<BytecodeBlock> visited = new HashSet<>();

        /*
        Temporary solution:
        Blocks associating the labels that is the start of a try block should be ignored in concatenating process.
        Collect those labels now for later query.
         */
        for (TryCatchBlockNode node : source.tryCatchBlocks) {
            ignoredLabels.add(node.start);
            ignoredLabels.add(node.end);
            ignoredLabels.add(node.handler);
        }

        dfsConcatenateBlocks(entry, visited);
        source.tryCatchBlocks.forEach(i -> dfsConcatenateBlocks(label2Block.get(i.handler), visited)); // mey trigger exception. be careful when used.
    }

    private void dfsConcatenateBlocks(BytecodeBlock bb, Set<BytecodeBlock> visitedSet) {
        boolean bridgeable = true;
        while (bridgeable) {
            bridgeable = concatenateSuccIfPossible(bb);
        }

        visitedSet.add(bb);

        // bb.setComplete();

        for (var succ : bb.outEdges()) {
            if (!visitedSet.contains(succ)) {
                dfsConcatenateBlocks(succ, visitedSet);
            }
        }
    }

    /**
     * Concatenate the successor.
     */
    private boolean concatenateSuccIfPossible(BytecodeBlock pred) {
        if (pred.outEdges().size() != 1) return false;

        var succ = pred.outEdges().get(0);
        assert !succ.isCatch(); // There should be no inEdges for exception blocks.
        if (succ.inEdges().size() != 1) return false;

        if (ignoredLabels.contains(succ.label())) return false;

        // Do not concatenate blocks that are explicitly declared to be separated,
        // because they could be separated by exception labelNodes.
        // i.e. GOTO, SWITCH.
        // But if succ is empty, the concatenation is ok.
        int opcode = pred.getLastBytecode().getOpcode();
        if ((opcode == Opcodes.GOTO || opcode == Opcodes.TABLESWITCH || opcode == Opcodes.LOOKUPSWITCH) && !succ.instr().isEmpty())
            return false;

        // Main concatenating process:
        pred.instr().addAll(succ.instr());
        pred.outEdges().clear();
        pred.outEdges().addAll(succ.outEdges());
        for (var succSucc : succ.outEdges()) {
            boolean b = succSucc.inEdges().remove(succ);
            assert b; // Maybe redundant.
            succSucc.inEdges().add(pred);
        }
        pred.setFallThrough(succ.fallThrough());

        // Remove the succ from label2Block.
        label2Block.remove(succ.label());

        return true;
    }

    private boolean canBeTraversedInBytecodeOrder() {
        BytecodeBlock entry = label2Block.get(this.entry);
        Set<BytecodeBlock> hasInStack = new HashSet<>();
        hasInStack.add(entry);
        hasInStack.addAll(
                label2Block.values().stream().filter(BytecodeBlock::isCatch).toList()
        );

        for (var bb : blockSortedList) {
            if (!hasInStack.contains(bb)) {
                return false;
            }
            hasInStack.addAll(bb.outEdges());
        }

        assert hasInStack.containsAll(blockSortedList); // This assertion should be satisfied at most times. Remove when released.
        assert new HashSet<>(blockSortedList).containsAll(hasInStack); // This assertion should be satisfied at most times. Remove when released.

        return true;
    }

    private void traverseBlocks() {
        BytecodeBlock entry = label2Block.get(this.entry);
        entry.setInStack(new Stack<>());

        boolean canBeTraversedInBytecodeOrder = canBeTraversedInBytecodeOrder();
        // assert canBeTraversedInBytecodeOrder; // This assertion should be satisfied at most times. Remove when released.
        if (canBeTraversedInBytecodeOrder) {
            for (var bb : blockSortedList) {
                buildBlockStmt(bb);
            }
        } else {
            Set<BytecodeBlock> visited = new HashSet<>();
            Queue<BytecodeBlock> workList = new LinkedList<>();
            workList.offer(entry);
            source.tryCatchBlocks.forEach(i -> workList.offer(label2Block.get(i.handler)));
            while (workList.peek() != null) {
                BytecodeBlock bb = workList.poll();
                if (visited.contains(bb)) {
                    continue;
                }
                visited.add(bb);

                buildBlockStmt(bb);

                for (BytecodeBlock succ : bb.outEdges()) {
                    if (!visited.contains(succ)) {
                        workList.offer(succ);
                    }
                }
            }
        }
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
                    var stmt = asm2Stmt.get(insnNode);
                    if (stmt != null) {
                        stmt.setLineNumber(currentLineNumber);
                    }

                    var stmts = auxiliaryStmts.get(insnNode);
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
        List<Pair<List<BytecodeBlock>, BytecodeBlock>> result = new ArrayList<>();
        for (var node : source.tryCatchBlocks) {
            var start = label2Block.get(node.start);
            var end = label2Block.get(node.end);
            var handler = label2Block.get(node.handler);
            assert start != null;
            assert handler != null;
            List<BytecodeBlock> tryBlocks;
            if (end != null) {
                tryBlocks = blockSortedList.subList(blockSortedList.indexOf(start), blockSortedList.indexOf(end));
            } else {
                if (node.end.getNext() == null) {
                    // node.end is the end asm InsnNode.
                    tryBlocks = blockSortedList.subList(blockSortedList.indexOf(start), blockSortedList.size());
                } else {
                    AbstractInsnNode insnNode = node.end;
                    while (insnNode != null && (
                            !(insnNode instanceof LabelNode) || !label2Block.containsKey(insnNode))) {
                        insnNode = insnNode.getNext();
                    }
//                    assert insnNode instanceof LabelNode; // make sure that the while loop above stops due to !label2Block.containsKey(insnNode).
                    if (insnNode == null) {
                        end = blockSortedList.get(blockSortedList.size() - 1);
                    } else {
                        end = label2Block.get((LabelNode) insnNode);
                    }

                    tryBlocks = blockSortedList.subList(blockSortedList.indexOf(start), blockSortedList.indexOf(end));
                }
            }
            tryBlocks.forEach(BytecodeBlock::setIsInTry);
            result.add(new Pair<>(tryBlocks, handler));
        }

        return result;
    }

    /**
     * Get the blocks that is in an arbitrary path from start to end.
     * WARNING: before calling this method, caller should have know that {@param start} and {@param end}
     * are dominators of the result set.
     * @param start (not null) the forward dominator of the result set.
     * @param end (not null) the backward dominator of the result set.
     * @return a list of blocks, and each of the elements is in an arbitrary path from start(inclusive)
     * to end(exclusive), in bfs order.
     */
    private List<BytecodeBlock> getStartToEndBlocks(BytecodeBlock start, BytecodeBlock end) {
        assert end != null && start != null;
        Queue<BytecodeBlock> workList = new LinkedList<>();
        workList.offer(start);
        List<BytecodeBlock> closure = new ArrayList<>();
        while (workList.peek() != null) {
            var bb = workList.poll();
            if (!closure.contains(bb) && bb != end) {
                closure.add(bb);
                for (var succ : bb.outEdges()) {
                    workList.offer(succ);
                }
            }
        }

        return closure;
    }

    public IR getIr() {
        return ir;
    }


}
