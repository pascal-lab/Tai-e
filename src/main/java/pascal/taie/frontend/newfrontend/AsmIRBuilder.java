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
import pascal.taie.frontend.newfrontend.dbg.BytecodeVisualizer;
import pascal.taie.frontend.newfrontend.report.StageTimer;
import pascal.taie.frontend.newfrontend.ssa.IndexedGraph;
import pascal.taie.frontend.newfrontend.ssa.SSATransform;
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
import pascal.taie.util.Indexer;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;

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

import static pascal.taie.frontend.newfrontend.Utils.*;

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

    private final List<Phi> phiList;

    private final DUInfo duInfo;

    private List<Pair<List<BytecodeBlock>, BytecodeBlock>> tryAndHandlerBlocks;

    private static final Logger logger = LogManager.getLogger();

    private static final StackItem TOP = new StackItem(Top.Top, null);

    private int currentLineNumber;

    public AsmIRBuilder(JMethod method, AsmMethodSource methodSource) {
        this.method = method;
        this.source = methodSource.adapter();
        assert method.getName().equals(source.name);
        this.classFileVersion = methodSource.classFileVersion();
        int instrSize = source.instructions.size();
        this.isEmpty = instrSize == 0;
        this.manager = new VarManager(method,
                source.localVariables, source.instructions, source.maxLocals);
        this.asm2Stmt = new Stmt[instrSize];
        this.auxiliaryStmts = new ArrayList<>(instrSize);
        for (int i = 0; i < instrSize; ++i) {
            auxiliaryStmts.add(null);
        }
        this.stmts = new ArrayList<>();
        this.phiList = new ArrayList<>();
        this.duInfo = new DUInfo(source.maxLocals);
    }

    public void build() {
        // a.analyze()
        if (! isEmpty) {
            StageTimer stageTimer = StageTimer.getInstance();
            stageTimer.startTypelessIR();
            buildCFG();
            traverseBlocks();
            stageTimer.endTypelessIR();
            this.isFrameUsable = classFileVersion >= Opcodes.V1_6;
            if (isFrameUsable()) {
                inferTypeWithFrame();
            } else {
                inferTypeWithoutFrame();
            }
            stageTimer.startTypelessIR();
            makeStmts();
            makeExceptionTable();
            stageTimer.endTypelessIR();
            // TODO: add options for ssa toggle
//            ssa();
            makeStmts();
            makeExceptionTable();
            verify();
            this.ir = getIR();
        }
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
        VarWebSplitter splitter = new VarWebSplitter(this);
        splitter.build();
        stageTimer.endSplitting();
        stageTimer.startTyping();
        // very important, need to build the exception table
        // before the type inference.
        // e.g. (catch %1), we store type info in exception table,
        //      TypeInference need to know what type %1 is
        makeStmts();
        makeExceptionTable();
        TypeInference inference = new TypeInference(this);
        inference.build();
        makeStmts();
        stageTimer.endTyping();
    }

    void ssa() {
        SSATransform<BytecodeBlock> ssa =
                new SSATransform<>(method, g, manager, duInfo, !exceptionEntries.isEmpty());
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
        if (lValue instanceof Var v) {
            duInfo.addDefBlock(v, currentBlock);
        }
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
        StackItem e = stack.pop();
        if (e.e() instanceof Top) {
            StackItem e1 = stack.pop();
            assert ! (e1.e() instanceof Top);
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

//    private AbstractInsnNode getOrig(Exp e) {
//        return exp2origin.get(e);
//    }

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

    private Var toVar(Exp e, AbstractInsnNode orig) {
        assert ! (e instanceof Var v && manager.isTempVar(v));
        if (e instanceof Phi phi) {
            if (phi.getVar() != null) {
                return phi.getVar();
            }
            phi.setUsed();
            Var v = manager.getTempVar();
            phi.setVar(v);
            return v;
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

    private Var popVar(Stack<StackItem> stack) {
        StackItem e = popExp(stack);
        if (e.e() instanceof Var v) {
            return v;
        } else {
            return toVar(e.e(), e.origin());
        }
    }

    private Stmt popToVar(Stack<StackItem> stack, Var v, BytecodeBlock block) {
        StackItem top = popExp(stack);
        // Note: Var . getUses() will return empty set
        if (top.e() instanceof Phi phi) {
            top = new StackItem(toVar(phi, null), null);
        } else {
            ensureStackSafety(stack, e -> e == v || e.getUses().contains(v));
        }
        return getAssignStmt(v, top.e());
    }

    private void popToEffect(Stack<StackItem> stack) {
        // normally, this should only be used to pop a InvokeExp
        StackItem item = stack.pop();
        Exp e = item.e();
        if (e instanceof Top) {
            return;
        } else if (e instanceof InvokeExp invokeExp) {
            assocStmt(item, new Invoke(method, invokeExp));
        } else if (maySideEffect(e)) {
            assocStmt(item, getAssignStmt(manager.getTempVar(), e));
        }
    }

    private void dup(Stack<StackItem> stack, int takes, int seps) {
        List<StackItem> takesList = new ArrayList<>(takes);
        for (int i = 0; i < takes; ++i) {
            StackItem e = stack.pop();
            if (e.e() instanceof Top || e.e() instanceof Var) {
                takesList.add(e);
            } else {
                takesList.add(new StackItem(toVar(e.e(), e.origin()), null));
            }
        }
        Collections.reverse(takesList);
        List<StackItem> sepsList = new ArrayList<>(seps);
        for (int i = 0; i < seps; ++i) {
            sepsList.add(stack.pop());
        }
        Collections.reverse(sepsList);
        stack.addAll(takesList);
        stack.addAll(sepsList);
        stack.addAll(takesList);
    }

    private void ensureStackSafety(Stack<StackItem> stack, Function<Exp, Boolean> predicate) {
        for (int i = 0; i < stack.size(); ++i) {
            StackItem item = stack.get(i);
            Exp e = item.e();
            if (e instanceof Top || e instanceof Phi) {
                continue;
            }
            if (predicate.apply(e)) {
                stack.set(i, new StackItem(toVar(e, item.origin()), null));
            }
        }
    }

    private boolean maySideEffect(Exp e) {
        return !(e instanceof Var || e instanceof Phi);
    }

    private void pushExp(AbstractInsnNode node, Stack<StackItem> stack, Exp e) {
        assert ! (e instanceof Top);
        ensureStackSafety(stack, this::maySideEffect);
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
        BytecodeBlock block = getBlockFromLabel(label);
        while (block.getStmts().isEmpty()) {
            BytecodeBlock next1 = getOutEdge(block, 0);
            BytecodeBlock next2 = blockSortedList.get(block.getIndex() + 1);
            if (next1 != next2) {
                // should not happen, which means refer to unreachable code
                // but may happen in real world code (this is valid bytecode)
                logger.atInfo().log("[IR] Unreachable code reference detected in method: "
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

    private void makeStmts() {
        this.stmts = new ArrayList<>(source.instructions.size());
        for (BytecodeBlock block : blockSortedList) {
            List<Stmt> blockStmts = block.getStmts();
            if (!blockStmts.isEmpty()) {
                for (Stmt t : blockStmts) {
                    t.setIndex(stmts.size());
                    stmts.add(t);
                }
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
            assert start.getIndex() != -1;
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

    private ArrayAccess getArrayAccess(Stack<StackItem> nowStack) {
        Var idx = popVar(nowStack);
        Var ref = popVar(nowStack);
        return new ArrayAccess(ref, idx);
    }

    private void storeExp(VarInsnNode varNode, Stack<StackItem> stack, BytecodeBlock block) {
        int idx = varNode.var;
        Var v = manager.getLocal(idx);
        storeExp(varNode, v, stack, block);
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
                StackItem item = stack.pop();
                Exp e = item.e();
                assert ! (e instanceof Top);
                StackItem op;
                if (e instanceof Var) {
                    op = item;
                } else {
                    op = new StackItem(toVar(e, item.origin()), null);
                }
                stack.push(op);
                stack.push(op);
            }
            case Opcodes.DUP2 -> dup(stack, 2, 0);
            case Opcodes.DUP_X1 -> dup(stack, 1, 1);
            case Opcodes.DUP_X2 -> dup(stack, 1, 2);
            case Opcodes.DUP2_X1 -> dup(stack, 2, 1);
            case Opcodes.DUP2_X2 -> dup(stack, 2, 2);
            case Opcodes.SWAP -> {
                // swap can only be used when v1 and v2 are both category 1 c.t.
                StackItem e1 = stack.pop();
                StackItem e2 = stack.pop();
                assert ! (e1.e() instanceof Top) && ! (e2.e() instanceof Top);
                stack.push(e1);
                stack.push(e2);
            }
            default -> throw new UnsupportedOperationException();
        }
    }

    private BytecodeBlock currentBlock;
    private void buildBlockStmt(BytecodeBlock block) {
        currentBlock = block;
        Stack<StackItem> inStack;
        if (block.getInStack() == null) {
            inStack = getInStack(block);
            block.setInStack(inStack);
        } else {
            inStack = block.getInStack();
        }
        assert inStack != null || block.isCatch();
        Stack<StackItem> nowStack = new Stack<>();
        Iterator<AbstractInsnNode> instr = block.instr().iterator();

        if (block.isCatch()) {
            if (instr.hasNext()) {
                AbstractInsnNode insnNode = instr.next();
                // skips all non-bytecode insn
                while (insnNode.getOpcode() == -1 && instr.hasNext()) {
                    insnNode = instr.next();
                    if (insnNode instanceof FrameNode f) {
                        block.setFrame(f);
                    }
                }

                if (insnNode.getOpcode() != -1) {
                    // insnNode is the first bytecode insn for this block
                    // for most cases, this should be a store insn
                    // this insn stores the exception object to a local var
                    if (insnNode.getOpcode() == Opcodes.ASTORE) {
                        VarInsnNode node = (VarInsnNode) insnNode;
                        Var catchVar = manager.getLocal(node.var);
                        duInfo.addDefBlock(catchVar, currentBlock);
                        assocStmt(node, new Catch(catchVar));
                    } else {
                        // else
                        // * for java source, insn should be POP *
                        // 1. make a catch stmt with temp var
                        // 2. push this temp var onto stack
                        Var v = manager.getTempVar();
                        duInfo.addDefBlock(v, currentBlock);
                        assocStmt(insnNode, new Catch(v));
                        pushExp(insnNode, nowStack, v);
                        processInstr(nowStack, insnNode, block);
                    }
                }
            }
        } else {
            assert inStack != null;
            nowStack.addAll(inStack);
        }

        while (instr.hasNext()) {
            AbstractInsnNode node = instr.next();
            processInstr(nowStack, node, block);
        }

        // if there is no out edges, it must be a return / throw block
        // do nothing
        // else, perform stack assign merge
//        if (!block.outEdges().isEmpty()) {
//            if (block.getOutStack() == null) {
//                // Web has not been constructed. So all the succs do not have inStack.
//                block.setOutStack(regularizeStack(block, nowStack));
//            } else {
//                Stack<Exp> target = block.getOutStack();
//                mergeStack(block, nowStack, target);
//            }
//        }

//        if (block.outEdges().size() > 1) {
//            for (int i = 0; i < nowStack.size(); ++i) {
//                Exp e = nowStack.get(i);
//                if (! (e instanceof Top || e instanceof Var || e instanceof Phi)) {
//                    nowStack.set(i, toVar(e));
//                }
//            }
//        }

        for (int j = 0; j < getOutEdgeCount(block); ++j) {
            BytecodeBlock outEdge = getOutEdge(block, j);
            if (outEdge.getInStack() != null) {
                assert outEdge.getInStack().size() == nowStack.size();
                for (int i = 0; i < nowStack.size(); ++i) {
                    StackItem item = outEdge.getInStack().get(i);
                    Exp exp = item.e();
                    if (nowStack.get(i).e() == Top.Top) {
                        assert exp == Top.Top;
                        continue;
                    }
                    assert exp instanceof Phi;
                    Phi phi = (Phi) exp;
                    phi.addNodes(nowStack.get(i));
                }
            }
        }
        block.setOutStack(nowStack);

        // collect all the stmts associated with this block.

    }

    private Stack<StackItem> getInStack(BytecodeBlock block) {
        Stack<StackItem> inStack;
        if (isInEdgeEmpty(block)) {
            inStack = null;
        } else {
            inStack = new Stack<>();
            List<Stack<StackItem>> stacks = new ArrayList<>();
            for (int i = 0; i < getInEdgeCount(block); ++i) {
                BytecodeBlock inEdge = getInEdge(block, i);
                if (inEdge.getOutStack() != null) {
                    stacks.add(inEdge.getOutStack());
                }
            }
            boolean canFastMerge = stacks.size() == getInEdgeCount(block);
            assert !stacks.isEmpty();
            for (int i = 0; i < stacks.get(0).size(); ++i) {
                List<StackItem> exps = new ArrayList<>();
                for (Stack<StackItem> stack : stacks) {
                    StackItem exp = stack.get(i);
                    if (!exps.contains(exp)) {
                        exps.add(exp);
                    }
                }
                boolean allSame = exps.size() == 1;
                StackItem item = exps.get(0);
                Exp e = item.e();
                if ((e instanceof Top) ||
                        (allSame && canFastMerge && e instanceof Var)) {
                    inStack.add(item);
                } else {
                    Phi phi = new Phi(i, exps, block);
                    inStack.add(new StackItem(phi, null));
                    phiList.add(phi);
                }
            }
        }
        return inStack;
    }

    private void solveAllPhiAndOutput() {
        solvePhis();
        for (BytecodeBlock bb : blockSortedList) {
            applyPhis(bb);
            outputIR(bb);
        }
    }

    MultiMap<Phi, Var> mergedVars = Maps.newMultiMap();

    private void solvePhis() {
        for (Phi phi : phiList) {
            if (phi.getVar() != null) {
                propagatePhiVar(phi, phi.getVar());
            }
        }

        for (Phi phi : phiList) {
            if (phi.getVar() == null) {
                Set<Var> merged = mergedVars.get(phi);
                if (merged.size() == 1) {
                    phi.setVar(merged.iterator().next());
                } else if (merged.size() > 1) {
                    phi.setVar(manager.getTempVar());
                }
            }
        }
    }

    private void propagatePhiVar(Phi current, Var var) {
        if (! mergedVars.get(current).contains(var)) {
            mergedVars.put(current, var);
            for (StackItem node : current.getNodes()) {
                if (node.e() instanceof Phi phi) {
                    propagatePhiVar(phi, var);
                }
            }
        }
    }

    private void applyPhis(BytecodeBlock block) {
        assert block.getOutStack() != null;
        currentBlock = block;
        if (block.getOutStack().isEmpty()) {
            return;
        }
        Map<Var, Integer> killed = Maps.newMap();
        List<Stmt> auxiliary = new ArrayList<>();
        for (int j = 0; j < getOutEdgeCount(block); ++j) {
            BytecodeBlock outEdge = getOutEdge(block, j);
            Stack<StackItem> inStack = outEdge.getInStack();
            Stack<StackItem> outStack = block.getOutStack();
            for (int i = 0; i < inStack.size(); ++i) {
                StackItem stackItem = inStack.get(i);
                Exp e1 = stackItem.e();
                if (e1 == Top.Top) {
                    assert outStack.get(i) == TOP;
                    continue;
                }
                Var var;
                if (e1 instanceof Phi phi) {
                    var = phi.getVar();
                } else {
                    continue;
                }
                StackItem outItem = outStack.get(i);
                Exp e = outStack.get(i).e();
                if (var != null) {
                    if (e instanceof Var v) {
                        if (var != v) {
                            auxiliary.add(getAssignStmt(var, v));
                        }
                    } else if (e instanceof Phi phi1) {
                        Var right = phi1.getVar();
                        assert right != null;
                        if (var != right) {
                            if (killed.containsKey(right)) {
                                Var temp = manager.getTempVar();
                                int pos = killed.get(right);
                                Stmt stmt = auxiliary.get(pos);
                                auxiliary.add(pos, getAssignStmt(temp, right));
                                auxiliary.set(pos + 1, new Lenses(this.method,
                                        Map.of(right, temp), Map.of())
                                        .subSt(stmt));
                                right = temp;
                            }
                            auxiliary.add(getAssignStmt(var, right));
                            killed.put(var, auxiliary.size() - 1);
                        }
                    } else {
                        // TODO: current impl is not safe
                        auxiliary.add(getAssignStmt(var, e));
                    }
                } else {
                    if (maySideEffect(e)) {
                        Var v = toVar(outItem.e(), outItem.origin());
                        outStack.set(i, new StackItem(v, null));
                    }
                }
            }
        }
        appendStackMergeStmts(block, auxiliary);
    }

    private void outputIR(BytecodeBlock block) {
        List<Stmt> blockStmt = block.getStmts();
        AsmListSlice instr = block.instr();
        int[] stmt2Asm = new int[instr.size()];
        int counter = 0;
        int start = instr.getStart();
        for (int i = 0; i < instr.size(); ++i) {
            int current = start + i;
            Stmt stmt = asm2Stmt[current];
            if (stmt != null) {
                blockStmt.add(stmt);
                if (counter >= stmt2Asm.length) {
                    stmt2Asm = Arrays.copyOf(stmt2Asm, stmt2Asm.length * 2);
                }
                stmt2Asm[counter++] = i;
            }

            List<Stmt> stmts = auxiliaryStmts.get(current);
            if (stmts != null) {
                blockStmt.addAll(stmts);
                for (int j = 0; j < stmts.size(); ++j) {
                    if (counter >= stmt2Asm.length) {
                        stmt2Asm = Arrays.copyOf(stmt2Asm, stmt2Asm.length * 2);
                    }
                    stmt2Asm[counter++] = i;
                }
            }
        }
        block.setStmt2Asm(stmt2Asm);
    }

    private void processInstr(Stack<StackItem> nowStack, AbstractInsnNode node, BytecodeBlock block) {
        if (node instanceof VarInsnNode varNode) {
            switch (varNode.getOpcode()) {
                case Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD ->
                        pushExp(node, nowStack, manager.getLocal(varNode.var));
                case Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE ->
                        storeExp(varNode, nowStack, block);
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
            nowStack.push(new StackItem(new ArithmeticExp(ArithmeticExp.Op.ADD, v, cst), inc));
            storeExp(inc, v, nowStack, block);
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
        } else if (node instanceof LabelNode) {
            // do nothing
        } else if (node instanceof LineNumberNode lineNumberNode) {
            this.currentLineNumber = lineNumberNode.line;
        } else if (node instanceof FrameNode frameNode) {
            if (block.getFrame() == null) {
                block.setFrame(frameNode);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

//    private Stack<Exp> regularizeStack(BytecodeBlock bb, Stack<Exp> origin) {
//        /*
//            1. conversion from non-Var Exp to Var,
//            2. no the same Vars in a stack.
//
//            The conversion should have effect on the InsnNode that generated the exp.
//         */
//        Stack<Exp> target = new Stack<>();
//        Set<Var> used = Sets.newHybridSet();
//        for (Exp e : origin) {
//            if (e instanceof Top) {
//                target.push(e);
//            }
//            else if (e instanceof Var v && manager.isTempVar(v) && !used.contains(v)) {
//                used.add(v);
//                target.push(v);
//            } else {
//                target.add(manager.getTempVar());
//            }
//        }
//        mergeStack(bb, origin, target);
//        return target;
//    }

    private BytecodeBlock[] idx2Block;

    private BytecodeBlock getBlockFromLabel(LabelNode node) {
        return idx2Block[getIndex(node)];
    }

    private void processEdge(BytecodeBlock now, LabelNode target) {
        int index = getIndex(target);
        BytecodeBlock b = idx2Block[index];
        assert b != null;
        g.addEdge(now.getIndex(), b.getIndex());
    }

    private void processEdges(BytecodeBlock now, List<LabelNode> targets) {
        for (LabelNode target : targets) {
            processEdge(now, target);
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
        int size = source.instructions.size();
        idx2Block = new BytecodeBlock[size];
        boolean[] fallThroughTable = new boolean[size];
        Arrays.fill(fallThroughTable, true);
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
            idx2Block[getIndex(now.handler)] =
                    createNewBlock(now.handler, getExceptionType(now.type));
        }

        FlattenExceptionTable fet = new FlattenExceptionTable(source);
        boolean inTry = false;
        int[] trySwitch = fet.buildExceptionSwitches();
        int trySwitchIndex = 0;
        for (int i = 0; i < size; ++i) {
            while (trySwitchIndex < trySwitch.length && trySwitch[trySwitchIndex] == i) {
                inTry = !inTry;
                trySwitchIndex++;
            }
            AbstractInsnNode now = source.instructions.get(i);
            boolean needNoBlock = true;
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
                    if (inTry) {
                        needNoBlock = false;
                    }
                } else if (now instanceof IincInsnNode iincInsnNode) {
                    if (inTry) {
                        needNoBlock = false;
                    }
                }
            }

            if (!needNoBlock) {
                AbstractInsnNode next = now.getNext();
                if (next != null) {
                    getBlock(next);
                }
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
                if (start + 1 == end && edge.getOpcode() == -1) {
                    // empty block
                    idx2Block[start] = idx2Block[i];
                    current = idx2Block[i];
                    start = i;
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
    }

    private void addExceptionEdges() {
        for (TryCatchBlockNode now : source.tryCatchBlocks) {
            BytecodeBlock handler = getBlock(now.handler);
            BytecodeBlock start = getBlock(now.start);
            BytecodeBlock end = getBlock(now.end);
            for (int i = start.getIndex(); i < end.getIndex(); ++i) {
                g.addExceptionEdge(i, handler.getIndex());
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

    private int maxBlockCounter = 0;
    private BytecodeBlock createNewBlock(LabelNode label) {
        maxBlockCounter++;
        return new BytecodeBlock(label, null);
    }

    private BytecodeBlock createNewBlock(LabelNode label, Type exceptionType) {
        maxBlockCounter++;
        return new BytecodeBlock(label, null, exceptionType);
    }

    private LabelNode createNewLabel(AbstractInsnNode next) {
        return new LabelNode() {
            @Override
            public AbstractInsnNode getNext() {
                return next;
            }
        };
    }

//    private final Set<LabelNode> ignoredLabels = new HashSet<>();
    /**
     * Bridge the blocks that a wrongly separated by regarding every LabelNode as an entry of a block.
     * We regard these pairs to be bridged: {(pred, succ) | pred.outEdges = {succ} && succ.inEdges = {pred}}
     * Processes:
     * 1. concat the 2 blocks;
     * 2. delete the entry for the successor in the label2Block map.
     */
//    private void bridgeWronglySeparatedBlocks() {
//        BytecodeBlock entry = label2Block.get(this.entry);
//        Set<BytecodeBlock> visited = new HashSet<>();

//        /*
//        Temporary solution:
//        Blocks associating the labels that is the start of a try block should be ignored in concatenating process.
//        Collect those labels now for later query.
//         */
//        for (TryCatchBlockNode node : source.tryCatchBlocks) {
//            ignoredLabels.add(node.start);
//            ignoredLabels.add(node.end);
//            ignoredLabels.add(node.handler);
//        }

//        dfsConcatenateBlocks(entry, visited);
//        source.tryCatchBlocks.forEach(i -> dfsConcatenateBlocks(label2Block.get(i.handler), visited)); // mey trigger exception. be careful when used.
//    }

//    private void dfsConcatenateBlocks(BytecodeBlock bb, Set<BytecodeBlock> visitedSet) {
//        boolean bridgeable = true;
//        while (bridgeable) {
//            bridgeable = concatenateSuccIfPossible(bb);
//        }

//        visitedSet.add(bb);

//        // bb.setComplete();

//        for (var succ : bb.outEdges()) {
//            if (!visitedSet.contains(succ)) {
//                dfsConcatenateBlocks(succ, visitedSet);
//            }
//        }
//    }

//    /**
//     * Concatenate the successor.
//     */
//    private boolean concatenateSuccIfPossible(BytecodeBlock pred) {
//        if (pred.outEdges().size() != 1) return false;

//        var succ = pred.outEdges().get(0);
//        assert !succ.isCatch(); // There should be no inEdges for exception blocks.
//        if (succ.inEdges().size() != 1) return false;

//        if (ignoredLabels.contains(succ.label())) return false;

//        // Do not concatenate blocks that are explicitly declared to be separated,
//        // because they could be separated by exception labelNodes.
//        // i.e. GOTO, SWITCH.
//        // But if succ is empty, the concatenation is ok.
//        if (isCFEdge(pred.getLastBytecode()) && !succ.instr().isEmpty())
//            return false;

//        // Main concatenating process:
//        pred.instr().addAll(succ.instr());
//        pred.outEdges().clear();
//        pred.outEdges().addAll(succ.outEdges());
//        for (var succSucc : succ.outEdges()) {
//            boolean b = succSucc.inEdges().remove(succ);
//            assert b; // Maybe redundant.
//            succSucc.inEdges().add(pred);
//        }
//        pred.setFallThrough(succ.fallThrough());

//        // Remove the succ from label2Block.
//        label2Block.remove(succ.label());

//        return true;
//    }

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

//        boolean canBeTraversedInBytecodeOrder = canBeTraversedInBytecodeOrder();
        boolean canBeTraversedInBytecodeOrder = false;
        // assert canBeTraversedInBytecodeOrder; // This assertion should be satisfied at most times. Remove when released.
        if (canBeTraversedInBytecodeOrder) {
            for (var bb : blockSortedList) {
                buildBlockStmt(bb);
            }
        } else {
            Set<BytecodeBlock> visited = new HashSet<>();
            Queue<BytecodeBlock> workList = new LinkedList<>();
            workList.offer(entry);
            source.tryCatchBlocks.forEach(i -> workList.offer(getBlockFromLabel(i.handler)));
            while (workList.peek() != null) {
                BytecodeBlock bb = workList.poll();
                if (visited.contains(bb)) {
                    continue;
                }
                visited.add(bb);

                buildBlockStmt(bb);

                for (int i = 0; i < getOutEdgeCount(bb); ++i) {
                    BytecodeBlock succ = getOutEdge(bb, i);
                    if (!visited.contains(succ)) {
                        workList.offer(succ);
                    }
                }
            }
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

    boolean isInEdgeEmpty(BytecodeBlock block) {
        return getInEdgeCount(block) == 0;
    }

    boolean isOutEdgeEmpty(BytecodeBlock block) {
        return getOutEdgeCount(block) == 0;
    }
}
