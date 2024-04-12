package pascal.taie.frontend.newfrontend;

import pascal.taie.frontend.newfrontend.ssa.PhiStmt;
import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.AssignStmt;
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
import pascal.taie.ir.stmt.Nop;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.ir.stmt.SwitchStmt;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.ir.stmt.Unary;

import java.util.function.Consumer;

/**
 * Naive deforestation of
 *
 * <pre>
 * {@code
 *     for (LValue l : stmt.getUse()) {
 *         if (l instanceof Var v) {
 *             consumer.accept(v);
 *         }
 *     }
 * }
 * </pre>
 */
public class StmtVarVisitor {

    public static void visitUse(Stmt stmt, Consumer<Var> consumer) {
        if (stmt instanceof Cast cast) {
            consumer.accept(cast.getRValue().getValue());
        } else if (stmt instanceof InstanceOf instanceOf) {
            consumer.accept(instanceOf.getRValue().getValue());
        } else if (stmt instanceof StoreField storeField) {
            consumer.accept(storeField.getRValue());
            visitFieldUse(storeField.getFieldAccess(), consumer);
        } else if (stmt instanceof LoadField loadField) {
            visitFieldUse(loadField.getRValue(), consumer);
        } else if (stmt instanceof Binary binary) {
            BinaryExp rValue = binary.getRValue();
            consumer.accept(rValue.getOperand1());
            consumer.accept(rValue.getOperand2());
        } else if (stmt instanceof Unary unary) {
            consumer.accept(unary.getRValue().getOperand());
        } else if (stmt instanceof Copy copy) {
            consumer.accept(copy.getRValue());
        } else if (stmt instanceof LoadArray loadArray) {
            visitArrayUse(loadArray.getArrayAccess(), consumer);
        } else if (stmt instanceof StoreArray storeArray) {
            consumer.accept(storeArray.getRValue());
            visitArrayUse(storeArray.getArrayAccess(), consumer);
        } else if (stmt instanceof Invoke invoke) {
            InvokeExp exp = invoke.getInvokeExp();
            if (exp instanceof InvokeInstanceExp invokeInstanceExp) {
                consumer.accept(invokeInstanceExp.getBase());
            }

            for (Var v : exp.getArgs()) {
                consumer.accept(v);
            }
        } else if (stmt instanceof Return r) {
            Var use = r.getValue();
            if (use != null) {
                consumer.accept(use);
            }
        } else if (stmt instanceof Monitor monitor) {
            consumer.accept(monitor.getObjectRef());
        } else if (stmt instanceof If ifStmt) {
            ConditionExp condition = ifStmt.getCondition();
            consumer.accept(condition.getOperand1());
            consumer.accept(condition.getOperand2());
        } else if (stmt instanceof SwitchStmt switchStmt) {
            consumer.accept(switchStmt.getVar());
        } else if (stmt instanceof Throw throwStmt) {
            consumer.accept(throwStmt.getExceptionRef());
        } else if (stmt instanceof Catch | stmt instanceof Goto
                | stmt instanceof New | stmt instanceof AssignLiteral
                | stmt instanceof Nop) {
            // Do nothing
            return;
        } else if (stmt instanceof PhiStmt phiStmt) {
            for (RValue v : phiStmt.getRValue().getUses()) {
                if (v instanceof Var var) {
                    consumer.accept(var);
                }
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static void visitDef(Stmt stmt, Consumer<Var> consumer) {
        if (stmt instanceof AssignStmt<?, ?> assignStmt) {
            LValue l = assignStmt.getLValue();
            if (l instanceof Var v) {
                consumer.accept(v);
            }
        } else if (stmt instanceof Catch catchStmt) {
            consumer.accept(catchStmt.getExceptionRef());
        } else if (stmt instanceof Invoke invoke) {
            Var v = invoke.getLValue();
            if (v != null) {
                consumer.accept(v);
            }
        }
    }

    private static void visitFieldUse(FieldAccess fieldAccess, Consumer<Var> consumer) {
        if (fieldAccess instanceof InstanceFieldAccess instanceFieldAccess) {
            consumer.accept(instanceFieldAccess.getBase());
        }
    }

    private static void visitArrayUse(ArrayAccess arrayAccess, Consumer<Var> consumer) {
        consumer.accept(arrayAccess.getBase());
        consumer.accept(arrayAccess.getIndex());
    }
}
