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

package pascal.taie.frontend.java.ir.typing;

import pascal.taie.frontend.java.ir.IRUtils;
import pascal.taie.frontend.java.ir.ssa.FrontendPhiStmt;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.ArrayLengthExp;
import pascal.taie.ir.exp.BitwiseExp;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.ComparisonExp;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.ExpVisitor;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InstanceOfExp;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.InvokeInterface;
import pascal.taie.ir.exp.InvokeSpecial;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.InvokeVirtual;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.NegExp;
import pascal.taie.ir.exp.NewArray;
import pascal.taie.ir.exp.NewMultiArray;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.ShiftExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignStmt;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Goto;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.LookupSwitch;
import pascal.taie.ir.stmt.Monitor;
import pascal.taie.ir.stmt.Nop;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.ir.stmt.TableSwitch;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.classes.JMethod;

import java.util.List;
import java.util.Map;

final class StmtVarReplacer {

    private final JMethod method;

    private final Map<Var, Var> useReplacements;

    private final Map<Var, Var> defReplacements;

    StmtVarReplacer(JMethod method, Map<Var, Var> useReplacements, Map<Var, Var> defReplacements) {
        this.method = method;
        this.defReplacements = defReplacements;
        this.useReplacements = useReplacements;
    }

    /**
     * Creates a new statement with variables replaced according to the configuration.
     */
    Stmt replace(Stmt stmt) {
        Stmt newStmt = stmt.accept(new StmtVisitor<>() {
            @Override
            public Stmt visit(If stmt) {
                return new If((ConditionExp) replaceVarInExp(stmt.getCondition()));
            }

            @Override
            public Stmt visit(TableSwitch stmt) {
                return new TableSwitch(replaceUse(stmt.getVar()), stmt.getLowIndex(), stmt.getHighIndex());
            }

            @Override
            public Stmt visit(LookupSwitch stmt) {
                return new LookupSwitch(replaceUse(stmt.getVar()), stmt.getCaseValues());
            }

            @Override
            public Stmt visit(Return stmt) {
                return new Return(replaceUse(stmt.getValue()));
            }

            @Override
            public Stmt visit(Throw stmt) {
                return new Throw(replaceUse(stmt.getExceptionRef()));
            }

            @Override
            public Stmt visit(Catch stmt) {
                return new Catch((Var) replaceLValue(stmt.getExceptionRef()));
            }

            @Override
            public Stmt visit(Monitor stmt) {
                return new Monitor(stmt.isEnter() ? Monitor.Op.ENTER : Monitor.Op.EXIT, replaceUse(stmt.getObjectRef()));
            }

            @Override
            public Stmt visitDefault(Stmt stmt) {
                if (stmt instanceof FrontendPhiStmt phi) {
                    assert useReplacements.isEmpty();
                    return new FrontendPhiStmt(phi.getBase(), replaceDef(phi.getLValue()), phi.getRValue());
                } else if (stmt instanceof AssignStmt<?, ?> assign) {
                    removeOldStmtFromRel(stmt);
                    return IRUtils.newAssignStmt(method, replaceLValue(assign.getLValue()), replaceRValue(assign.getRValue()));
                } else if (stmt instanceof Invoke invoke) {
                    removeOldStmtFromRel(stmt);
                    return new Invoke(invoke.getContainer(), (InvokeExp) replaceVarInExp(invoke.getInvokeExp()),
                            invoke.getLValue() == null ? null : (Var) replaceLValue(invoke.getLValue()));
                } else {
                    assert stmt instanceof Nop || stmt instanceof Goto;
                    return stmt;
                }
            }
        });

        newStmt.setLineNumber(stmt.getLineNumber());
        return newStmt;
    }

    private Var replaceUse(Var v) {
        return useReplacements.getOrDefault(v, v);
    }

    private Var replaceDef(Var v) {
        return defReplacements.getOrDefault(v, v);
    }

    private List<Var> replaceUses(List<Var> l) {
        return l.stream()
                .map(this::replaceUse)
                .toList();
    }

    private LValue replaceLValue(LValue l) {
        if (l instanceof Var v) {
            return replaceDef(v);
        } else if (l instanceof ArrayAccess access) {
            return (ArrayAccess) replaceVarInExp(access);
        } else if (l instanceof FieldAccess access) {
            return (FieldAccess) replaceVarInExp(access);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private RValue replaceRValue(RValue r) {
        return (RValue) replaceVarInExp(r);
    }

    private Exp replaceVarInExp(Exp e) {
        return e.accept(new ExpVisitor<>() {
            @Override
            public Exp visit(Var var) {
                return replaceUse(var);
            }

            @Override
            public Exp visit(InstanceFieldAccess fieldAccess) {
                return new InstanceFieldAccess(fieldAccess.getFieldRef(), replaceUse(fieldAccess.getBase()));
            }

            @Override
            public Exp visit(ArrayAccess arrayAccess) {
                return new ArrayAccess(replaceUse(arrayAccess.getBase()), replaceUse(arrayAccess.getIndex()));
            }

            @Override
            public Exp visit(NewArray newArray) {
                return new NewArray(newArray.getType(), replaceUse(newArray.getLength()));
            }

            @Override
            public Exp visit(NewMultiArray newMultiArray) {
                return new NewMultiArray(newMultiArray.getType(), replaceUses(newMultiArray.getLengths()));
            }

            @Override
            public Exp visit(InvokeInterface invoke) {
                return new InvokeInterface(invoke.getMethodRef(), replaceUse(invoke.getBase()), replaceUses(invoke.getArgs()));
            }

            @Override
            public Exp visit(InvokeSpecial invoke) {
                return new InvokeSpecial(invoke.getMethodRef(), replaceUse(invoke.getBase()), replaceUses(invoke.getArgs()));
            }

            @Override
            public Exp visit(InvokeStatic invoke) {
                return new InvokeStatic(invoke.getMethodRef(), replaceUses(invoke.getArgs()));
            }

            @Override
            public Exp visit(InvokeVirtual invoke) {
                return new InvokeVirtual(invoke.getMethodRef(), replaceUse(invoke.getBase()), replaceUses(invoke.getArgs()));
            }

            @Override
            public Exp visit(InvokeDynamic invoke) {
                return new InvokeDynamic(invoke.getBootstrapMethodHandle(), invoke.getBootstrapMethodRef(),
                        invoke.getMethodName(), invoke.getMethodType(), invoke.getBootstrapArgs(),
                        replaceUses(invoke.getArgs()));
            }

            @Override
            public Exp visit(ArrayLengthExp exp) {
                return new ArrayLengthExp(replaceUse(exp.getBase()));
            }

            @Override
            public Exp visit(NegExp exp) {
                return new NegExp(replaceUse(exp.getOperand()));
            }

            @Override
            public Exp visit(ArithmeticExp exp) {
                return new ArithmeticExp(exp.getOperator(), replaceUse(exp.getOperand1()), replaceUse(exp.getOperand2()));
            }

            @Override
            public Exp visit(BitwiseExp exp) {
                return new BitwiseExp(exp.getOperator(), replaceUse(exp.getOperand1()), replaceUse(exp.getOperand2()));
            }

            @Override
            public Exp visit(ComparisonExp exp) {
                return new ComparisonExp(exp.getOperator(), replaceUse(exp.getOperand1()), replaceUse(exp.getOperand2()));
            }

            @Override
            public Exp visit(ConditionExp exp) {
                return new ConditionExp(exp.getOperator(), replaceUse(exp.getOperand1()), replaceUse(exp.getOperand2()));
            }

            @Override
            public Exp visit(ShiftExp exp) {
                return new ShiftExp(exp.getOperator(), replaceUse(exp.getOperand1()), replaceUse(exp.getOperand2()));
            }

            @Override
            public Exp visit(InstanceOfExp exp) {
                return new InstanceOfExp(replaceUse(exp.getValue()), exp.getCheckedType());
            }

            @Override
            public Exp visit(CastExp exp) {
                return new CastExp(replaceUse(exp.getValue()), exp.getCastType());
            }

            @Override
            public Exp visitDefault(Exp exp) {
                assert exp.getUses().isEmpty();
                return exp;
            }
        });
    }

    /**
     * Remove the old statement from the vars' relevant stmts it references.
     */
    private void removeOldStmtFromRel(Stmt oldStmt) {
        if (oldStmt instanceof Invoke i) {
            if (i.getInvokeExp() instanceof InvokeInstanceExp exp) {
                exp.getBase().removeRelevantStmt(oldStmt);
            }
        } else if (oldStmt instanceof LoadArray l) {
            l.getRValue().getBase().removeRelevantStmt(oldStmt);
        } else if (oldStmt instanceof StoreArray s) {
            s.getLValue().getBase().removeRelevantStmt(oldStmt);
        } else if (oldStmt instanceof LoadField lf) {
            if (lf.getFieldAccess() instanceof InstanceFieldAccess instanceFieldAccess) {
                instanceFieldAccess.getBase().removeRelevantStmt(oldStmt);
            }
        } else if (oldStmt instanceof StoreField sf) {
            if (sf.getFieldAccess() instanceof InstanceFieldAccess instanceFieldAccess) {
                instanceFieldAccess.getBase().removeRelevantStmt(oldStmt);
            }
        }
    }
}
