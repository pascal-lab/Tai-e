package pascal.taie.frontend.newfrontend;

import org.apache.commons.lang3.NotImplementedException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import pascal.taie.ir.DefaultIR;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ArithmeticExp;
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
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInterface;
import pascal.taie.ir.exp.InvokeSpecial;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.InvokeVirtual;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.LongLiteral;
import pascal.taie.ir.exp.NegExp;
import pascal.taie.ir.exp.NewInstance;
import pascal.taie.ir.exp.NullLiteral;
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
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.Goto;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.ir.stmt.SwitchStmt;
import pascal.taie.ir.stmt.Unary;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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

    private final Map<Exp, AbstractInsnNode> exp2Orig;

    private final Map<AbstractInsnNode, Stmt> asm2Stmt;

    public AsmIRBuilder(JMethod method, JSRInlinerAdapter source) {
        this.method = method;
        this.source = source;
        this.manager = new VarManager(method, source.parameters, source.localVariables, source.instructions);
        this.asm2Stmt = Maps.newMap();
        this.exp2Orig = Maps.newMap();
    }

    public void build() {
        buildCFG();
    }

    private Stmt getAssignStmt(Var v, Exp e) {
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
        } else {
            throw new NotImplementedException();
        }
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

    private AbstractInsnNode getOrig(Exp e) {
        return exp2Orig.get(e);
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
        exp2Orig.put(e, node);
        ensureStackSafety(stack, this::maySideEffect);
        stack.push(e);
        if (isDword(node, e)) {
            stack.push(Top.getInstance());
        }
    }

    private void pushConst(AbstractInsnNode node, Stack<Exp> stack, Literal literal) {
        // TODO: handle const value properly
        Var v = manager.getConstVar(literal);
        stack.push(v);
        if (literal instanceof LongLiteral || literal instanceof DoubleLiteral) {
            stack.push(Top.getInstance());
        }
        assocStmt(node, getAssignStmt(v, literal));
    }

    public void buildIR() {
        traverseBlocks();
        List<Stmt> stmts = getStmts();
        Var thisVar = manager.getThisVar();
        List<Var> params = manager.getParams();
        List<Var> vars = manager.getVars();
        Set<Var> retVars = manager.getRetVars();
        List<ExceptionEntry> entries = new ArrayList<>();
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

    private Literal getConstValue(InsnNode node) {
        int opcode = node.getOpcode();
        if (opcode == Opcodes.ACONST_NULL) {
            return NullLiteral.get();
        } else if (inRange(opcode, Opcodes.ICONST_M1, Opcodes.ICONST_5)) {
            return IntLiteral.get(opcode - Opcodes.ICONST_M1 + -1);
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
        ClassType owner = BuildContext.get().fromAsmInternalName(methodInsnNode.owner);
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

    private void storeExp(VarInsnNode varNode, Stack<Exp> stack) {
        int idx = varNode.var;
        Var v = manager.getLocal(idx, varNode);
        Exp now = popExp(stack);
        ensureStackSafety(stack, e -> e.getUses().contains(v));
        assocStmt(varNode, getAssignStmt(v, now));
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

    private void buildBlockStmt(BytecodeBlock block) {
        Stack<Var> inStack = block.getInStack();
        Stack<Exp> nowStack = new Stack<>();
        nowStack.addAll(inStack);

        for (AbstractInsnNode node : block.instr()) {
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
                ClassType type = BuildContext.get().fromAsmInternalName(typeNode.desc);
                if (opcode == Opcodes.CHECKCAST) {
                    pushExp(node, nowStack, getCastExp(nowStack, type));
                } else if (opcode == Opcodes.NEW) {
                    pushExp(node, nowStack, new NewInstance(type));
                }
            } else if (node instanceof IntInsnNode intNode) {
                int opcode = intNode.getOpcode();
                if (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) {
                    pushConst(node, nowStack, IntLiteral.get(intNode.operand));
                }
            } else if (node instanceof FieldInsnNode fieldInsnNode) {
                int opcode = fieldInsnNode.getOpcode();
                ClassType owner = BuildContext.get().fromAsmInternalName(fieldInsnNode.owner);
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
                        assocStmt(fieldInsnNode, new StoreField(access, v1));
                    }
                    case Opcodes.PUTFIELD -> {
                        Var value = popVar(nowStack);
                        Var base = popVar(nowStack);
                        assocStmt(fieldInsnNode, new StoreField(new InstanceFieldAccess(ref, base), value));
                    }
                    default -> throw new IllegalStateException();
                }
            } else if (node instanceof MethodInsnNode methodInsnNode) {
                pushExp(node, nowStack, getInvokeExp(methodInsnNode, nowStack));
            }
        }

        if (block.getOutStack() == null) {
            // Web has not been constructed. So all the succs do not have inStack.
            block.setOutStack(regularizeStack(nowStack));
        } else {
            // TODO: merge stack
            // In the early stage of development, we assume that there is no Var remained in the nowStack.
        }
    }

    private Stack<Var> regularizeStack(Stack<Exp> origin) {
        //
        /*
            TODO: regularization, including:
            1. conversion from non-Var Exp to Var,
            2. no the same Vars in a stack.

            The conversion should have effect on the InsnNode that generated the exp.
            In the early stage of development, we assume that there is no Exp remained in the nowStack,
            so an empty Stack is enough.
         */
        return new Stack<>();
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
    }

    private BytecodeBlock getBlock(LabelNode label) {
        return label2Block.computeIfAbsent(label, this::createNewBlock);
    }

    private BytecodeBlock createNewBlock(LabelNode label) {
        return new BytecodeBlock(label, new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), null);
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
        LabelNode node = new LabelNode() {
            @Override
            public AbstractInsnNode getNext() {
                return next;
            }
        };

        return node;
    }

    private void traverseBlocks() {
        Set<BytecodeBlock> visited = new HashSet<>();
        BytecodeBlock entry = label2Block.get(this.entry);
        entry.setInStack(new Stack<>());

        Queue<BytecodeBlock> workList = new LinkedList<>();
        workList.offer(entry);

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

    public IR getIr() {
        return ir;
    }


}
