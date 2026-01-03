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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.objectweb.asm.Opcodes;
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
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.ArrayLengthExp;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.BitwiseExp;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.ComparisonExp;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.DoubleLiteral;
import pascal.taie.ir.exp.ExpMutator;
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
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Goto;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.LookupSwitch;
import pascal.taie.ir.stmt.Monitor;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
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
import pascal.taie.util.collection.Pair;


import static pascal.taie.language.type.BooleanType.BOOLEAN;
import static pascal.taie.language.type.ByteType.BYTE;
import static pascal.taie.language.type.CharType.CHAR;
import static pascal.taie.language.type.DoubleType.DOUBLE;
import static pascal.taie.language.type.FloatType.FLOAT;
import static pascal.taie.language.type.IntType.INT;
import static pascal.taie.language.type.LongType.LONG;
import static pascal.taie.language.type.ShortType.SHORT;

/**
 * Translates bytecode instructions within a single basic block into Tai-e's statements.
 */
final class BytecodeProcessor {

    // --- Dependencies ---
    private final FrontendTypeSystem typeSystem;
    private final VarManager varManager;
    private final JMethod method;
    private final boolean isSSA;
    private final OperandStack operandStack;
    private final SlotManager slotManager;
    private final StmtManager stmtManager;

    BytecodeProcessor(FrontendTypeSystem typeSystem, VarManager varManager, JMethod method, boolean isSSA, OperandStack operandStack, SlotManager slotManager, StmtManager stmtManager) {
        this.typeSystem = typeSystem;
        this.varManager = varManager;
        this.method = method;
        this.isSSA = isSSA;
        this.operandStack = operandStack;
        this.slotManager = slotManager;
        this.stmtManager = stmtManager;
    }

    void processBlock2Stmts(BytecodeBlock block) {
        slotManager.enterBlock(block);
        operandStack.initializeStack(block);
        Iterator<AbstractInsnNode> insnIter = block.getInsns().iterator();

        if (isSSA) {
            slotManager.emitSSAPhisForSlot(block);
        }
        // skips all non-bytecode insn
        AbstractInsnNode insn = insnIter.next();
        while (insn.getOpcode() == -1 && insnIter.hasNext()) {
            insn = insnIter.next();
            if (insn instanceof FrameNode f) {
                block.setFrame(f);
            } else if (insn instanceof LineNumberNode l) {
                stmtManager.setLineNumber(l.line);
            }
        }
        // now, insn must be:
        // 1. the first "real" bytecode insn, or
        // 2. the last "fake" bytecode insn
        if (block.isCatch()) {
            if (insn.getOpcode() != -1) {
                Var catchVar;
                // insn is the first bytecode insn for this block
                // for most cases, this should be a store insn
                // this insn stores the exception object to a local var
                if (insn.getOpcode() == Opcodes.ASTORE) {
                    catchVar = slotManager.storeCatchVar(insn);
                } else {
                    // else
                    // * for java source, insn should be POP *
                    // 1. make a catch stmt with temp var
                    // 2. push this temp var onto stack
                    catchVar = varManager.getTempVar();
                    stmtManager.associateStmt(insn, new Catch(catchVar));
                    operandStack.pushExp(insn, catchVar);
                    processInsn2Stmt(insn);
                }
                List<ClassType> handlerTypes = Objects.requireNonNull(block.getExceptionHandlerTypes());
                if (handlerTypes.size() == 1) {
                    ExpMutator.setType(catchVar, handlerTypes.get(0));
                } else {
                    // let type inference decide the type
                    varManager.setNonSSA(catchVar);
                }
            }
            // `insn.getOpcode() == -1` which means the last bytecode is also synthetic
            // this block is totally empty. Do nothing.
        } else {
            // process the first bytecode insn
            if (insn.getOpcode() != -1) {
                processInsn2Stmt(insn);
            }
        }

        while (insnIter.hasNext()) {
            AbstractInsnNode currInsn = insnIter.next();
            processInsn2Stmt(currInsn);
        }

        // Temp fix. Add a nop to represent a block. Used in ssa.
        if (isSSA) {
            stmtManager.ensureBlockNotEmpty(block);
        }

        operandStack.saveOutStackAndClear();
        slotManager.exitBlock();
    }

    private void processInsn2Stmt(AbstractInsnNode insn) {
        if (insn instanceof VarInsnNode varInsn) {
            switch (varInsn.getOpcode()) {
                case Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD -> {
                    Var v = slotManager.loadVar(varInsn.var, insn);
                    operandStack.pushExp(insn, v);
                }
                case Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE,
                     Opcodes.ASTORE -> slotManager.storeVar(varInsn.var, varInsn, operandStack);
                default -> // we can never reach here, JSRInlineAdapter should eliminate all rets
                        throw new UnsupportedOperationException();
            }
        } else if (insn instanceof InsnNode basicInsn) {
            int opcode = basicInsn.getOpcode();
            if (opcode == Opcodes.NOP) {
                return;
            } else if (opcode == Opcodes.ARRAYLENGTH) {
                operandStack.pushExp(insn, new ArrayLengthExp(operandStack.popVar()));
            } else if (opcode == Opcodes.ATHROW) {
                throwException(basicInsn);
            } else if (opcode == Opcodes.MONITORENTER) {
                Var obj = operandStack.popVar();
                stmtManager.associateStmt(insn, new Monitor(Monitor.Op.ENTER, obj));
            } else if (opcode == Opcodes.MONITOREXIT) {
                Var obj = operandStack.popVar();
                stmtManager.associateStmt(insn, new Monitor(Monitor.Op.EXIT, obj));
            } else if (isBinaryInsn(opcode)) {
                operandStack.pushExp(insn, getBinaryExp(opcode));
            } else if (isReturnInsn(opcode)) {
                returnExp(basicInsn);
            } else if (isConstInsn(opcode)) {
                operandStack.pushConst(insn, toConstValue(basicInsn));
            } else if (isPrimCastInsn(opcode)) {
                operandStack.pushExp(insn, getCastExp(toCastType(opcode)));
            } else if (isNegInsn(opcode)) {
                Var v1 = operandStack.popVar();
                operandStack.pushExp(insn, new NegExp(v1));
            } else if (isStackInsn(opcode)) {
                operandStack.performStackOp(opcode);
            } else if (isArrayLoadInsn(opcode)) {
                ArrayAccess access = getArrayAccess();
                operandStack.pushExp(insn, access);
            } else if (isArrayStoreInsn(opcode)) {
                Var value = operandStack.popVar();
                ArrayAccess access = getArrayAccess();
                storeExp(insn, access, value);
            } else {
                throw new UnsupportedOperationException();
            }
        } else if (insn instanceof JumpInsnNode jump) {
            if (jump.getOpcode() == Opcodes.GOTO) {
                stmtManager.associateStmt(jump, new Goto());
            } else {
                ConditionExp cond = getIfExp(jump.getOpcode());
                stmtManager.associateStmt(jump, new If(cond));
            }
        } else if (insn instanceof LdcInsnNode ldc) {
            operandStack.pushConst(insn, Utils.fromObject(typeSystem, ldc.cst));
        } else if (insn instanceof TypeInsnNode typeInsn) {
            int opcode = typeInsn.getOpcode();
            ReferenceType type = typeSystem.fromAsmInternalName(typeInsn.desc);
            if (opcode == Opcodes.CHECKCAST) {
                operandStack.pushExp(insn, getCastExp(type));
            } else if (opcode == Opcodes.NEW) {
                operandStack.pushExp(insn, new NewInstance((ClassType) type));
            } else if (opcode == Opcodes.ANEWARRAY) {
                Var length = operandStack.popVar();
                int dims = 1;
                Type base;
                if (type instanceof ArrayType arrayType) {
                    dims += arrayType.dimensions();
                    base = arrayType.baseType();
                } else {
                    base = type;
                }
                ArrayType arrayType = typeSystem.getArrayType(base, dims);
                operandStack.pushExp(insn, new NewArray(arrayType, length));
            } else if (opcode == Opcodes.INSTANCEOF) {
                Var obj = operandStack.popVar();
                operandStack.pushExp(insn, new InstanceOfExp(obj, type));
            } else {
                throw new UnsupportedOperationException();
            }
        } else if (insn instanceof IntInsnNode intInsn) {
            int opcode = intInsn.getOpcode();
            if (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) {
                operandStack.pushConst(insn, IntLiteral.get(intInsn.operand));
            } else if (opcode == Opcodes.NEWARRAY) {
                PrimitiveType base = switch (intInsn.operand) {
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
                ArrayType arrayType = typeSystem.getArrayType(base, 1);
                Var length = operandStack.popVar();
                operandStack.pushExp(insn, new NewArray(arrayType, length));
            } else {
                assert false;
            }
        } else if (insn instanceof FieldInsnNode fieldInsn) {
            int opcode = fieldInsn.getOpcode();
            ClassType owner = (ClassType) typeSystem.fromAsmInternalName(fieldInsn.owner);
            Type type = typeSystem.fromAsmTypeDesc(fieldInsn.desc);
            String name = fieldInsn.name;
            FieldRef ref = FieldRef.get(owner.getJClass(), name, type,
                    opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC);
            switch (opcode) {
                case Opcodes.GETSTATIC -> operandStack.pushExp(insn, new StaticFieldAccess(ref));
                case Opcodes.GETFIELD -> {
                    Var v1 = operandStack.popVar();
                    operandStack.pushExp(insn, new InstanceFieldAccess(ref, v1));
                }
                case Opcodes.PUTSTATIC -> {
                    FieldAccess access = new StaticFieldAccess(ref);
                    Var v1 = operandStack.popVar();
                    operandStack.ensureStackSafety(Utils::mayHaveSideEffect);
                    storeExp(insn, access, v1);
                }
                case Opcodes.PUTFIELD -> {
                    Var value = operandStack.popVar();
                    Var base = operandStack.popVar();
                    FieldAccess access = new InstanceFieldAccess(ref, base);
                    operandStack.ensureStackSafety(Utils::mayHaveSideEffect);
                    storeExp(insn, access, value);
                }
                default -> throw new UnsupportedOperationException();
            }
        } else if (insn instanceof MethodInsnNode methodInsn) {
            InvokeExp exp = getInvokeExp(methodInsn);
            operandStack.pushExp(insn, exp);
            if (exp.getType() == VoidType.VOID) {
                operandStack.popToEffect();
            }
        } else if (insn instanceof MultiANewArrayInsnNode multiArrayInsn) {
            Type type = typeSystem.fromAsmTypeDesc(multiArrayInsn.desc);
            assert type instanceof ArrayType;

            List<Var> lengths = new ArrayList<>();
            // ..., count1, [count2, ...] ->
            for (int i = 0; i < multiArrayInsn.dims; ++i) {
                lengths.add(operandStack.popVar());
            }
            Collections.reverse(lengths);

            operandStack.pushExp(insn, new NewMultiArray((ArrayType) type, lengths));
        } else if (insn instanceof IincInsnNode inc) {
            operandStack.pushConst(insn, IntLiteral.get(inc.incr));
            Var cst = operandStack.popVar();
            Var v = slotManager.loadVar(inc.var, insn);
            operandStack.pushExp(inc, new ArithmeticExp(ArithmeticExp.Op.ADD, v, cst));
            slotManager.storeVar(inc.var, inc, operandStack);
        } else if (insn instanceof InvokeDynamicInsnNode indyInsn) {
            MethodHandle handle = Utils.fromAsmHandle(typeSystem, indyInsn.bsm);
            List<Literal> bootArgs = Arrays.stream(indyInsn.bsmArgs)
                    .map((o) -> Utils.fromObject(typeSystem, o)).toList();
            assert handle.isMethodRef();
            Pair<List<Type>, Type> paramRets =
                    typeSystem.fromAsmMethodDesc(indyInsn.desc);
            List<Var> args = new ArrayList<>();
            for (int i = 0; i < paramRets.first().size(); ++i) {
                args.add(operandStack.popVar());
            }
            Collections.reverse(args);
            operandStack.pushExp(insn, new InvokeDynamic(
                    handle,
                    handle.getMethodRef(),
                    indyInsn.name,
                    MethodType.get(paramRets.first(), paramRets.second()),
                    bootArgs,
                    args));

        } else if (insn instanceof TableSwitchInsnNode tableSwitch) {
            Var v = operandStack.popVar();
            stmtManager.associateStmt(insn, new TableSwitch(v, tableSwitch.min, tableSwitch.max));
        } else if (insn instanceof LookupSwitchInsnNode lookupSwitch) {
            Var v = operandStack.popVar();
            stmtManager.associateStmt(insn, new LookupSwitch(v, lookupSwitch.keys));
        } else if (insn instanceof LabelNode || insn instanceof FrameNode) {
            // do nothing
            return;
        } else if (insn instanceof LineNumberNode lineNumber) {
            stmtManager.setLineNumber(lineNumber.line);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void throwException(InsnNode insn) {
        Var v = operandStack.popVar();
        stmtManager.associateStmt(insn, new Throw(v));
    }

    private ConditionExp getIfExp(int opcode) {
        Var v1;
        Var v2;
        if (isInRange(opcode, Opcodes.IFEQ, Opcodes.IFLE)) {
            v1 = operandStack.popVar();
            v2 = varManager.getConstVar(IntLiteral.get(0));
        } else if (opcode == Opcodes.IFNULL || opcode == Opcodes.IFNONNULL) {
            v1 = operandStack.popVar();
            v2 = varManager.getNullLiteral();
        } else {
            v2 = operandStack.popVar();
            v1 = operandStack.popVar();
        }
        return new ConditionExp(toCondOp(opcode), v1, v2);
    }

    private BinaryExp getBinaryExp(int opcode) {
        Var v2 = operandStack.popVar();
        Var v1 = operandStack.popVar();
        if (isArithmeticInsn(opcode)) {
            return new ArithmeticExp(toArithmeticOp(opcode), v1, v2);
        } else if (isBitwiseInsn(opcode)) {
            return new BitwiseExp(toBitwiseOp(opcode), v1, v2);
        } else if (isComparisonInsn(opcode)) {
            return new ComparisonExp(toCmpOp(opcode), v1, v2);
        } else if (isShiftInsn(opcode)) {
            return new ShiftExp(toShiftOp(opcode), v1, v2);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private CastExp getCastExp(Type t) {
        Var v1 = operandStack.popVar();
        return new CastExp(v1, t);
    }

    private InvokeExp getInvokeExp(MethodInsnNode methodInsnNode) {
        int opcode = methodInsnNode.getOpcode();
        JClass owner = typeSystem.toJClass(methodInsnNode.owner);
        assert owner != null;
        Pair<List<Type>, Type> desc = typeSystem.fromAsmMethodDesc(methodInsnNode.desc);
        String name = methodInsnNode.name;
        boolean isStatic = opcode == Opcodes.INVOKESTATIC;
        MethodRef ref = MethodRef.get(owner, name, desc.first(), desc.second(), isStatic, methodInsnNode.itf);

        List<Var> args = new ArrayList<>();
        for (int i = 0; i < desc.first().size(); ++i) {
            args.add(operandStack.popVar());
        }
        Collections.reverse(args);
        Var base = isStatic ? null : operandStack.popVar();

        assert ref.getParameterTypes().size() == args.size();
        return switch (opcode) {
            case Opcodes.INVOKESTATIC -> new InvokeStatic(ref, args);
            case Opcodes.INVOKEVIRTUAL -> new InvokeVirtual(ref, base, args);
            case Opcodes.INVOKEINTERFACE -> new InvokeInterface(ref, base, args);
            case Opcodes.INVOKESPECIAL -> new InvokeSpecial(ref, base, args);
            default -> throw new UnsupportedOperationException();
        };
    }

    private ArrayAccess getArrayAccess() {
        Var idx = operandStack.popVar();
        Var ref = operandStack.popVar();
        return new ArrayAccess(ref, idx);
    }

    private void storeExp(AbstractInsnNode insn, LValue left, RValue right) {
        Stmt stmt = Utils.newAssignStmt(method, left, right);
        stmtManager.associateStmt(insn, stmt);
    }

    private void returnExp(InsnNode insn) {
        // now, empty the stack, ensure all expression with side effect is generated
        operandStack.ensureStackSafety(Utils::mayHaveSideEffect);
        int opcode = insn.getOpcode();
        if (opcode == Opcodes.RETURN) {
            stmtManager.associateStmt(insn, new Return());
        } else {
            Var v = operandStack.popVar();
            varManager.addReturnVar(v);
            stmtManager.associateStmt(insn, new Return(v));
        }
    }

    // ========== Opcode Range Check ==========

    private static boolean isInRange(int opcode, int min, int max) {
        return opcode >= min && opcode <= max;
    }

    // ========== Instruction Classification ==========

    private static boolean isConstInsn(int opcode) {
        return opcode == Opcodes.ACONST_NULL ||
                isInRange(opcode, Opcodes.ICONST_M1, Opcodes.ICONST_5) ||
                isInRange(opcode, Opcodes.FCONST_0, Opcodes.FCONST_2) ||
                isInRange(opcode, Opcodes.LCONST_0, Opcodes.LCONST_1) ||
                isInRange(opcode, Opcodes.DCONST_0, Opcodes.DCONST_1);
    }

    private static boolean isAddInsn(int opcode) {
        return isInRange(opcode, Opcodes.IADD, Opcodes.DADD);
    }

    private static boolean isSubInsn(int opcode) {
        return isInRange(opcode, Opcodes.ISUB, Opcodes.DSUB);
    }

    private static boolean isMulInsn(int opcode) {
        return isInRange(opcode, Opcodes.IMUL, Opcodes.DMUL);
    }

    private static boolean isDivInsn(int opcode) {
        return isInRange(opcode, Opcodes.IDIV, Opcodes.DDIV);
    }

    private static boolean isRemInsn(int opcode) {
        return isInRange(opcode, Opcodes.IREM, Opcodes.DREM);
    }

    private static boolean isNegInsn(int opcode) {
        return isInRange(opcode, Opcodes.INEG, Opcodes.DNEG);
    }

    private static boolean isPrimCastInsn(int opcode) {
        return isInRange(opcode, Opcodes.I2L, Opcodes.I2S);
    }

    private static boolean isBinaryInsn(int opcode) {
        return (isInRange(opcode, Opcodes.IADD, Opcodes.LXOR) && !isNegInsn(opcode)) ||
                isComparisonInsn(opcode);
    }

    private static boolean isArithmeticInsn(int opcode) {
        return isInRange(opcode, Opcodes.IADD, Opcodes.DREM);
    }

    private static boolean isBitwiseInsn(int opcode) {
        return isInRange(opcode, Opcodes.IAND, Opcodes.LXOR);
    }

    private static boolean isShiftInsn(int opcode) {
        return isInRange(opcode, Opcodes.ISHL, Opcodes.LUSHR);
    }

    private static boolean isComparisonInsn(int opcode) {
        return isInRange(opcode, Opcodes.LCMP, Opcodes.DCMPG);
    }

    private static boolean isReturnInsn(int opcode) {
        return isInRange(opcode, Opcodes.IRETURN, Opcodes.RETURN);
    }

    private static boolean isStackInsn(int opcode) {
        return isInRange(opcode, Opcodes.POP, Opcodes.SWAP);
    }

    private static boolean isArrayLoadInsn(int opcode) {
        return isInRange(opcode, Opcodes.IALOAD, Opcodes.SALOAD);
    }

    private static boolean isArrayStoreInsn(int opcode) {
        return isInRange(opcode, Opcodes.IASTORE, Opcodes.SASTORE);
    }

    // ========== Constant Value Extraction ==========

    private static Literal toConstValue(InsnNode node) {
        int opcode = node.getOpcode();
        if (opcode == Opcodes.ACONST_NULL) {
            return NullLiteral.get();
        } else if (isInRange(opcode, Opcodes.ICONST_M1, Opcodes.ICONST_5)) {
            return IntLiteral.get(opcode - Opcodes.ICONST_M1 - 1);
        } else if (isInRange(opcode, Opcodes.FCONST_0, Opcodes.FCONST_2)) {
            return FloatLiteral.get(opcode - Opcodes.FCONST_0 + 0.0f);
        } else if (isInRange(opcode, Opcodes.DCONST_0, Opcodes.DCONST_1)) {
            return DoubleLiteral.get(opcode - Opcodes.DCONST_0 + 0.0);
        } else if (isInRange(opcode, Opcodes.LCONST_0, Opcodes.LCONST_1)) {
            return LongLiteral.get(opcode - Opcodes.LCONST_0);
        } else {
            throw new IllegalArgumentException("Not a constant instruction: " + opcode);
        }
    }

    // ========== Opcode Unification ==========

    private static int unifyIfOp(int opcode) {
        if (isInRange(opcode, Opcodes.IFEQ, Opcodes.IFLE)) {
            return opcode - Opcodes.IFEQ + Opcodes.IF_ICMPEQ;
        } else if (isInRange(opcode, Opcodes.FCMPL, Opcodes.FCMPG)) {
            return opcode - Opcodes.FCMPL + Opcodes.DCMPL;
        } else {
            return opcode;
        }
    }

    // ========== Opcode to Tai-e IR Operator Conversion ==========

    private static Type toCastType(int opcode) {
        return switch (opcode) {
            case Opcodes.L2I, Opcodes.F2I, Opcodes.D2I -> INT;
            case Opcodes.I2L, Opcodes.F2L, Opcodes.D2L -> LONG;
            case Opcodes.I2F, Opcodes.L2F, Opcodes.D2F -> FLOAT;
            case Opcodes.I2D, Opcodes.L2D, Opcodes.F2D -> DOUBLE;
            case Opcodes.I2B -> BYTE;
            case Opcodes.I2S -> SHORT;
            case Opcodes.I2C -> CHAR;
            default -> throw new IllegalArgumentException("Unknown cast opcode: " + opcode);
        };
    }

    private static ShiftExp.Op toShiftOp(int opcode) {
        return switch (opcode) {
            case Opcodes.ISHL, Opcodes.LSHL -> ShiftExp.Op.SHL;
            case Opcodes.ISHR, Opcodes.LSHR -> ShiftExp.Op.SHR;
            case Opcodes.IUSHR, Opcodes.LUSHR -> ShiftExp.Op.USHR;
            default -> throw new IllegalArgumentException("Unknown shift opcode: " + opcode);
        };
    }

    private static ConditionExp.Op toCondOp(int opcode) {
        opcode = unifyIfOp(opcode);
        return switch (opcode) {
            case Opcodes.IF_ICMPEQ, Opcodes.IF_ACMPEQ, Opcodes.IFNULL -> ConditionExp.Op.EQ;
            case Opcodes.IF_ICMPNE, Opcodes.IF_ACMPNE, Opcodes.IFNONNULL -> ConditionExp.Op.NE;
            case Opcodes.IF_ICMPLT -> ConditionExp.Op.LT;
            case Opcodes.IF_ICMPGE -> ConditionExp.Op.GE;
            case Opcodes.IF_ICMPGT -> ConditionExp.Op.GT;
            case Opcodes.IF_ICMPLE -> ConditionExp.Op.LE;
            default -> throw new IllegalArgumentException("Unknown condition opcode: " + opcode);
        };
    }

    private static ArithmeticExp.Op toArithmeticOp(int opcode) {
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
        } else {
            throw new IllegalArgumentException("Unknown arithmetic opcode: " + opcode);
        }
    }

    private static BitwiseExp.Op toBitwiseOp(int opcode) {
        return switch (opcode) {
            case Opcodes.IAND, Opcodes.LAND -> BitwiseExp.Op.AND;
            case Opcodes.IOR, Opcodes.LOR -> BitwiseExp.Op.OR;
            case Opcodes.IXOR, Opcodes.LXOR -> BitwiseExp.Op.XOR;
            default -> throw new IllegalArgumentException("Unknown bitwise opcode: " + opcode);
        };
    }

    private static ComparisonExp.Op toCmpOp(int opcode) {
        return switch (opcode) {
            case Opcodes.LCMP -> ComparisonExp.Op.CMP;
            case Opcodes.DCMPG, Opcodes.FCMPG -> ComparisonExp.Op.CMPG;
            case Opcodes.DCMPL, Opcodes.FCMPL -> ComparisonExp.Op.CMPL;
            default -> throw new IllegalArgumentException("Unknown comparison opcode: " + opcode);
        };
    }
}
