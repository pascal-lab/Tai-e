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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import pascal.taie.ir.exp.DoubleLiteral;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.LongLiteral;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static pascal.taie.language.type.DoubleType.DOUBLE;
import static pascal.taie.language.type.LongType.LONG;

/**
 * Simulates JVM operand stack during bytecode-to-IR conversion.
 * <p>
 * This class encapsulates all stack manipulation logic,
 * including push/pop operations, expression lifting to variables,
 * and stack safety enforcement.
 */
final class StackSimulator {

    /**
     * Sentinel value representing the top half of a double-word value on the stack.
     */
    private static final StackItem TOP = new StackItem(Top.TOP, null);

    private final JMethod method;

    private final VarManager manager;

    /**
     * Callback interface for associating generated statements with bytecode instructions.
     */
    private final BiConsumer<AbstractInsnNode, Stmt> stmtAssociator;

    StackSimulator(JMethod method, VarManager manager,
                   BiConsumer<AbstractInsnNode, Stmt> StmtAssociator) {
        this.manager = manager;
        this.method = method;
        this.stmtAssociator = StmtAssociator;
    }

    /**
     * Pushes expression onto stack, ensuring stack safety for side-effecting expressions.
     */
    void pushExp(AbstractInsnNode node, Stack<StackItem> stack, Exp exp) {
        assert !(exp instanceof Top);
        if (Utils.mayHaveSideEffect(exp)) {
            ensureStackSafety(stack, Utils::mayHaveSideEffect);
        }
        stack.push(new StackItem(exp, node));
        if (isDword(node, exp)) {
            stack.push(TOP);
        }
    }

    /**
     * Pushes a constant onto stack.
     */
    void pushConst(AbstractInsnNode node, Stack<StackItem> stack, Literal literal) {
        if (manager.peekConstVar(literal)) {
            pushExp(node, stack, manager.getConstVar(literal));
        } else {
            pushExp(node, stack, literal);
        }
    }

    /**
     * Pops an expression from the stack, skipping Top markers.
     */
    StackItem popExp(Stack<StackItem> stack) {
        StackItem item1 = popStack(stack);
        if (item1.exp() instanceof Top) {
            StackItem item2 = popStack(stack);
            assert !(item2.exp() instanceof Top);
            return item2;
        } else {
            return item1;
        }
    }

    /**
     * Basic pop operation.
     */
    StackItem popStack(Stack<StackItem> stack) {
        return stack.pop();
    }

    /**
     * Pops from stack and ensures the result is a Var.
     */
    Var popVar(Stack<StackItem> stack) {
        StackItem e = popExp(stack);
        liftToVar(e);
        return e.var();
    }

    /**
     * Pops from stack and assigns to the specified variable.
     */
    Stmt popToVar(Stack<StackItem> stack, Var v) {
        StackItem top = popExp(stack);
        if (top.exp() instanceof StackPhi) {
            liftToVar(top);
        } else {
            ensureStackSafety(stack, e -> e == v || e.getUses().contains(v));
        }
        return Utils.newAssignStmt(method, v, top.exp());
    }

    /**
     * Pops from stack for side effects only.
     */
    void popToEffect(Stack<StackItem> stack) {
        StackItem item = popStack(stack);
        Exp e = item.exp();
        if (e instanceof Top) {
        } else {
            expToEffect(item);
        }
    }

    /**
     * Converts expression to side effect statement.
     */
    void expToEffect(StackItem item) {
        Exp e = item.exp();
        if (e instanceof InvokeExp invokeExp) {
            stmtAssociator.accept(item.origin(), new Invoke(method, invokeExp));
        } else if (Utils.mayHaveSideEffect(e)) {
            stmtAssociator.accept(item.origin(),
                    Utils.newAssignStmt(method, manager.getTempVar(), e));
        }
    }

    void automaticPopToEffect(Stack<StackItem> stack) {
        StackItem item = popExp(stack);
        expToEffect(item);
    }

    /**
     * Ensures the item is a Var, if not, lifts it to a Var.
     * Does nothing for Top. Sets the item.var for Var.
     */
    void liftToVar(StackItem item) {
        Exp exp = item.exp();
        if (exp instanceof Top) {
        } else if (exp instanceof Var var) {
            item.lift(var);
        } else {
            Var var = toVar(exp, item.origin());
            item.lift(var);
        }
    }

    /**
     * Forces lifting to a new Var, even when expression is already a Var.
     * Emits a new ($-v = e) statement.
     */
    void forceLiftToVar(StackItem item) {
        Var var = toVar(item.exp(), item.origin());
        item.lift(var);
    }

    /**
     * Converts expression to a Var, creating assignment statement if needed.
     */
    Var toVar(Exp e, AbstractInsnNode orig) {
        assert !(e instanceof Var v && manager.isTempVar(v));
        if (e instanceof StackPhi phi) {
            phi.setUsed();
            assert phi.getVar() != null;
            return phi.getVar();
        }

        Var v;
        if (e instanceof NullLiteral) {
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
        Stmt auxStmt = Utils.newAssignStmt(method, v, e);
        stmtAssociator.accept(orig, auxStmt);
        return v;
    }

    /**
     * Ensures stack safety by lifting expressions matching the predicate to Vars.
     */
    void ensureStackSafety(Stack<StackItem> stack, Function<Exp, Boolean> predicate) {
        for (StackItem item : stack) {
            Exp exp = item.exp();
            if (exp instanceof Top || exp instanceof StackPhi) {
                continue;
            }
            if (predicate.apply(exp)) {
                forceLiftToVar(item);
            }
        }
    }

    /**
     * Performs JVM stack manipulation operations (DUP, POP, SWAP, etc.).
     */
    void performStackOp(Stack<StackItem> stack, int opcode) {
        switch (opcode) {
            case Opcodes.POP -> popToEffect(stack);
            case Opcodes.POP2 -> {
                popToEffect(stack);
                popToEffect(stack);
            }
            case Opcodes.DUP -> {
                StackItem item = popStack(stack);
                Exp e = item.exp();
                assert !(e instanceof Top);
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
                StackItem e1 = popStack(stack);
                StackItem e2 = popStack(stack);
                assert !(e1.exp() instanceof Top) && !(e2.exp() instanceof Top);
                stack.push(e1);
                stack.push(e2);
            }
            default -> throw new UnsupportedOperationException();
        }
    }

    /**
     * Performs DUP_X* operations.
     * Takes 'takes' items, skips 'seps' items, then pushes taken items twice.
     */
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

    /**
     * Checks if an expression represents a double-word value (long or double).
     */
    private static boolean isDword(AbstractInsnNode node, Exp e) {
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
}
