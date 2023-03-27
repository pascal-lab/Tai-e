package pascal.taie.frontend.newfrontend;

import org.apache.commons.lang3.NotImplementedException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
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
import pascal.taie.ir.DefaultIR;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.ArrayAccess;
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
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInterface;
import pascal.taie.ir.exp.InvokeSpecial;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.InvokeVirtual;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.LongLiteral;
import pascal.taie.ir.exp.NegExp;
import pascal.taie.ir.exp.NewArray;
import pascal.taie.ir.exp.NewExp;
import pascal.taie.ir.exp.NewInstance;
import pascal.taie.ir.exp.NewMultiArray;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.ShiftExp;
import pascal.taie.ir.exp.StaticFieldAccess;
import pascal.taie.ir.exp.UnaryExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Binary;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.Goto;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.InstanceOf;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.Monitor;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.ir.stmt.SwitchStmt;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.ir.stmt.Unary;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.VoidType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
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

    private final JMethod method;

    private final JSRInlinerAdapter source;

    public Map<LabelNode, BytecodeBlock> getLabel2Block() {
        return label2Block;
    }

    private Map<LabelNode, BytecodeBlock> label2Block;

    private LabelNode entry;

    private final VarManager manager;

    private final Map<Exp, AbstractInsnNode> exp2origin;

    private final Map<AbstractInsnNode, Stmt> asm2Stmt;

    private final Map<AbstractInsnNode, List<Stmt>> auxiliaryStmts;

    public AsmIRBuilder(JMethod method, JSRInlinerAdapter source) {
        this.method = method;
        this.source = source;
        assert source.parameters == null;
        this.manager = new VarManager(method, source.parameters, source.localVariables, source.instructions);
        this.asm2Stmt = Maps.newMap();
        this.exp2origin = Maps.newMap();
        this.auxiliaryStmts = Maps.newMap();
    }

    public void build() {
        buildCFG();
    }

    private Stmt getAssignStmt(LValue left, Exp e) {
        if (left instanceof Var v) {
            if (e instanceof BinaryExp binaryExp) {
                return new Binary(v, binaryExp);
            } else if (e instanceof Literal l) {
                return new AssignLiteral(v, l);
            } else if (e instanceof CastExp cast) {
                return new Cast(v, cast);
            } else if (e instanceof UnaryExp unaryExp) {
                return new Unary(v, unaryExp);
            } else if (e instanceof Var v1) {
                return new Copy(v, v1);
            } else if (e instanceof FieldAccess fieldAccess) {
                return new LoadField(v, fieldAccess);
            } else if (e instanceof InvokeExp invokeExp) {
                return new Invoke(method, invokeExp, v);
            } else if (e instanceof NewExp newExp)  {
                return new New(method, v, newExp);
            } else if (e instanceof ArrayAccess access) {
                return new LoadArray(v, access);
            } else if (e instanceof InstanceOfExp instanceOfExp) {
                return new InstanceOf(v, instanceOfExp);
            }
            else {
                throw new NotImplementedException();
            }
        } else if (left instanceof ArrayAccess arrayAccess) {
            assert e instanceof Var;
            return new StoreArray(arrayAccess, (Var) e);
        } else if (left instanceof FieldAccess fieldAccess) {
            assert e instanceof Var;
            return new StoreField(fieldAccess, (Var) e);
        }
        throw new NotImplementedException();
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

    private void assocStmt(AbstractInsnNode node, Stmt stmt) {
        asm2Stmt.put(node, stmt);
    }

    private Var toVar(Exp e) {
        Var v = manager.getTempVar();
        Stmt auxStmt = getAssignStmt(v, e);
        assocStmt(getOrig(e), auxStmt);
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
        ensureStackSafety(stack, e -> e.getUses().contains(v));
        return getAssignStmt(v, top);
    }

    private void popToEffect(Stack<Exp> stack) {
        // normally, this should only be used to pop a InvokeExp
        Exp e = popExp(stack);
        if (e instanceof InvokeExp invokeExp) {
            assocStmt(exp2origin.get(e), new Invoke(method, invokeExp));
        } else if (maySideEffect(e)) {
            assocStmt(exp2origin.get(e), getAssignStmt(manager.getTempVar(), e));
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
        // TODO: handle const value properly
        Var v = manager.getConstVar(literal);
        stack.push(v);
        if (literal instanceof LongLiteral || literal instanceof DoubleLiteral) {
            stack.push(Top.Top);
        }
        assocStmt(node, getAssignStmt(v, literal));
    }

    public void buildIR() {
        traverseBlocks();
        setLineNumber();
        List<Stmt> stmts = getStmts();
        Var thisVar = manager.getThisVar();
        List<Var> params = manager.getParams();
        List<Var> vars = manager.getVars();
        Set<Var> retVars = manager.getRetVars();
        List<ExceptionEntry> entries = getExceptionTable();
        ir = new DefaultIR(method, thisVar, params, retVars , vars, stmts, entries);
    }

    private Stmt getFirstStmt(LabelNode label) {
        BytecodeBlock block = label2Block.get(label);
        assert block != null;
        if (block.getFirstStmt() != null) {
            return block.getFirstStmt();
        }

        for (AbstractInsnNode node : block.instr()) {
            if (asm2Stmt.containsKey(node)) {
                Stmt first = asm2Stmt.get(node);
                block.setFirstStmt(first);
                return first;
            }
        }

        if (auxiliaryStmts.containsKey(block.getLastBytecode())) {
            return auxiliaryStmts.get(block.getLastBytecode()).get(0);
        }

        throw new IllegalStateException();
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

    private List<Stmt> getStmts() {
        List<Stmt> res = new ArrayList<>();
        // TODO: start from 0 or 1 ?
        int counter = 0;

        for (AbstractInsnNode node : source.instructions) {
            if (asm2Stmt.containsKey(node)) {
                Stmt stmt = asm2Stmt.get(node);
                setJumpTargets(node, stmt);
                stmt.setIndex(counter++);
                res.add(stmt);
            }

            if (auxiliaryStmts.containsKey(node)) {
                for (Stmt stmt : auxiliaryStmts.get(node)) {
                    stmt.setIndex(counter++);
                    res.add(stmt);
                }
            }
        }
        return res;
    }

    private List<ExceptionEntry> getExceptionTable() {
        List<ExceptionEntry> res = new ArrayList<>();
        for (TryCatchBlockNode node : source.tryCatchBlocks) {
            Stmt start = getFirstStmt(node.start);
            Stmt end = getFirstStmt(node.end);
            Stmt handler = getFirstStmt(node.handler);
            String name = node.type == null ? getThrowable() : node.type;
            ReferenceType expType = BuildContext.get().fromAsmInternalName(name);
            res.add(new ExceptionEntry(start, end, (Catch) handler, (ClassType) expType));
        }
        return res;
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
        ClassType owner = (ClassType)
                BuildContext.get().fromAsmInternalName(methodInsnNode.owner);
        Pair<List<Type>, Type> desc = BuildContext.get().fromAsmMethodType(methodInsnNode.desc);
        String name = methodInsnNode.name;
        boolean isStatic = opcode == Opcodes.INVOKESTATIC;
        MethodRef ref = MethodRef.get(owner.getJClass(), name, desc.first(), desc.second(), isStatic);

        List<Var> args = new ArrayList<>();
        for (int i = 0; i < desc.first().size(); ++i) {
            args.add(popVar(stack));
        }
        Collections.reverse(args);
        Var base = isStatic ? null : popVar(stack);

        return switch (opcode) {
            case Opcodes.INVOKESTATIC -> new InvokeStatic(ref, args);
            case Opcodes.INVOKEVIRTUAL -> new InvokeVirtual(ref, base, args);
            case Opcodes.INVOKEINTERFACE -> new InvokeInterface(ref, base, args);
            case Opcodes.INVOKESPECIAL -> new InvokeSpecial(ref, base, args);
            default -> throw new IllegalStateException();
        };
    }

    private ArrayAccess getArrayAccess(Stack<Exp> nowStack) {
        Var idx = popVar(nowStack);
        Var ref = popVar(nowStack);
        ArrayAccess access = new ArrayAccess(ref, idx);
        return access;
    }

    private void storeExp(VarInsnNode varNode, Stack<Exp> stack) {
        int idx = varNode.var;
        Var v = manager.getLocal(idx, varNode);
        Stmt stmt = popToVar(stack, v);
        assocStmt(varNode, stmt);
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

    private void mergeStack1(List<Stmt> auxiliary, Stack<Exp> nowStack, Stack<Var> targetStack) {
        Var v = targetStack.pop();
        Exp e = peekExp(nowStack);
        if (e == v) {
            popExp(nowStack);
        } else {
            Stmt stmt = popToVar(nowStack, v);
            auxiliary.add(stmt);
        }
    }

    private void mergeStack(BytecodeBlock bb, Stack<Exp> nowStack, Stack<Var> target) {
        List<Stmt> auxiliary = new ArrayList<>();
        Stack<Exp> nowStack1 = new Stack<>();
        Stack<Var> target1 = new Stack<>();
        nowStack1.addAll(nowStack);
        target1.addAll(target);
        while (! nowStack1.isEmpty()) {
            mergeStack1(auxiliary, nowStack1, target1);
        }
        if (auxiliary.size() != 0) {
            auxiliaryStmts.put(bb.getLastBytecode(), auxiliary);
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
            default -> throw new IllegalArgumentException();
        }
    }

    private void buildBlockStmt(BytecodeBlock block) {
        Stack<Var> inStack = block.getInStack();
        Stack<Exp> nowStack = new Stack<>();
        Iterator<AbstractInsnNode> instr = block.instr().iterator();

        if (block.isCatch()) {
            // TODO: log when first instr of a catch block is not astore_x
            if (instr.hasNext()) {
                AbstractInsnNode insnNode = instr.next();
                if (insnNode.getOpcode() == Opcodes.ASTORE) {
                    VarInsnNode node = (VarInsnNode) insnNode;
                    assocStmt(node, new Catch(manager.getLocal(node.var, node)));
                }
            }
        } else {
            nowStack.addAll(inStack);
        }

        while (instr.hasNext()) {
            AbstractInsnNode node = instr.next();
            if (node instanceof VarInsnNode varNode) {
                switch (varNode.getOpcode()) {
                    case Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD ->
                        pushExp(node, nowStack, manager.getLocal(varNode.var, varNode));
                    case Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE ->
                            storeExp(varNode, nowStack);
                    default -> // we can never reach here, JSRInlineAdapter should eliminate all rets
                            throw new IllegalStateException();
                }
            } else if (node instanceof InsnNode insnNode) {
                int opcode = insnNode.getOpcode();
                if (opcode == Opcodes.NOP) {
                    continue;
                } else if (opcode == Opcodes.ARRAYLENGTH) {
                    pushExp(node, nowStack, popExp(nowStack));
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
                    pushExp(node, nowStack, new NewArray((ArrayType) type, length));
                } else if (opcode == Opcodes.INSTANCEOF) {
                    Var obj = popVar(nowStack);
                    pushExp(node, nowStack, new InstanceOfExp(obj, type));
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
                    default -> throw new IllegalStateException();
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
                Var v = manager.getLocal(inc.var, node);
                auxiliaryStmts.put(node, List.of(getAssignStmt(v, cst)));
            }
        }

        if (block.outEdges().size() == 0) {
            // no out edges, must be a return / throw block
            // do nothing
            return;
        }

        if (block.getOutStack() == null) {
            // Web has not been constructed. So all the succs do not have inStack.
            block.setOutStack(regularizeStack(block, nowStack));
        } else {
            Stack<Var> target = block.getOutStack();
            mergeStack(block, nowStack, target);
        }
    }

    private Stack<Var> regularizeStack(BytecodeBlock bb, Stack<Exp> origin) {
        /*
            1. conversion from non-Var Exp to Var,
            2. no the same Vars in a stack.

            The conversion should have effect on the InsnNode that generated the exp.
         */
        Stack<Var> target = new Stack<>();
        for (Exp e : origin) {
            if (e instanceof Top) {
                continue;
            }
            else if (e instanceof Var v && manager.isTempVar(v)) {
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
            label2Block.put(now.handler, new BytecodeBlock(now.handler, null, true));
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
                if (! (now instanceof LabelNode) && ! (now instanceof LineNumberNode)) {
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

        bridgeWronglySeparatedBlocks();
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
            throw new IllegalStateException();
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
        assert !pred.isCatch();
        if (pred.outEdges().size() != 1) return false;

        var succ = pred.outEdges().get(0);
        assert !succ.isCatch();
        if (succ.inEdges().size() != 1) return false;

        if (ignoredLabels.contains(succ.label())) return false;

        // Do not concatenate blocks that are explicitly declared to be separated
        // i.e. GOTO, SWITCH
        int opcode = pred.getLastBytecode().getOpcode();
        if (opcode == Opcodes.GOTO || opcode == Opcodes.TABLESWITCH || opcode == Opcodes.LOOKUPSWITCH)
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

    private List<BytecodeBlock> getLinearBBList() {
        BytecodeBlock entry = label2Block.get(this.entry);
        Set<BytecodeBlock> hasInStack = new HashSet<>();
        hasInStack.add(entry);
        hasInStack.addAll(
                label2Block.values().stream().filter(BytecodeBlock::isCatch).toList()
        );

        List<BytecodeBlock> bytecodeBlockList =
                label2Block.keySet().stream()
                .sorted((a, b) -> source.instructions.indexOf(a.getNext()) - source.instructions.indexOf(b.getNext()))
                .toList()
                .stream()
                .map(label -> label2Block.get(label))
                .toList();
        for (var bb : bytecodeBlockList) {
            if (!hasInStack.contains(bb)) {
                return null;
            }
            hasInStack.addAll(bb.outEdges());
        }

        assert hasInStack.containsAll(bytecodeBlockList); // This assertion should be satisfied at most times. Remove when released.
        assert new HashSet<>(bytecodeBlockList).containsAll(hasInStack); // This assertion should be satisfied at most times. Remove when released.

        return bytecodeBlockList;
    }

    private void traverseBlocks() {
        BytecodeBlock entry = label2Block.get(this.entry);
        entry.setInStack(new Stack<>());

        List<BytecodeBlock> bytecodeBlockList = getLinearBBList();
        assert bytecodeBlockList != null; // This assertion should be satisfied at most times. Remove when released.
        if (bytecodeBlockList != null) {
            for (var bb : bytecodeBlockList) {
                buildBlockStmt(bb);
            }
        } else {
            Set<BytecodeBlock> visited = new HashSet<>();
            Queue<BytecodeBlock> workList = new LinkedList<>();
            workList.offer(entry);
            source.tryCatchBlocks.forEach(i -> workList.offer(label2Block.get(i.handler)));
            while (workList.peek() != null) {
                BytecodeBlock bb = workList.poll();
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
                    assert currentLineNumber != -1
                            : "Violating our assumption: before the first node associating a stmt must be a LineNumberNode";
                    var stmt = asm2Stmt.get(insnNode);
                    if (stmt != null) {
                        stmt.setLineNumber(currentLineNumber);
                    }

                    // TODO: set lineNumber for stmt lists in the node (i.e. auxiliary stmts).
                }
            }
        }
    }

    public IR getIr() {
        return ir;
    }


}
