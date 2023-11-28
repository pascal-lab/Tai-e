package pascal.taie.frontend.newfrontend;

import pascal.taie.frontend.newfrontend.ssa.PhiStmt;
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

public class Lenses {

    private final JMethod method;

    private final Map<Var, Var> useSigma;

    private final Map<Var, Var> defSigma;

    public Lenses(JMethod method, Map<Var, Var> useSigma, Map<Var, Var> defSigma) {
        this.method = method;
        this.defSigma = defSigma;
        this.useSigma = useSigma;
    }

    public Var subSt(Var v) {
        return useSigma.getOrDefault(v, v);
    }

    public Var defSubSt(Var v) {
        return defSigma.getOrDefault(v, v);
    }

    public List<Var> subSt(List<Var> l) {
        return l.stream()
                .map(this::subSt)
                .toList();
    }

    public LValue leftSubSt(LValue l) {
        if (l instanceof Var v) {
            return defSubSt(v);
        } else if (l instanceof ArrayAccess access) {
            return (ArrayAccess) subSt(access);
        } else if (l instanceof FieldAccess access) {
            return (FieldAccess) subSt(access);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public RValue rightSubst(RValue r) {
        return (RValue) subSt(r);
    }

    public Exp subSt(Exp e) {
        return e.accept(new ExpVisitor<>() {
            @Override
            public Exp visit(Var var) {
                return subSt(var);
            }

            @Override
            public Exp visit(InstanceFieldAccess fieldAccess) {
                return new InstanceFieldAccess(fieldAccess.getFieldRef(), subSt(fieldAccess.getBase()));
            }

            @Override
            public Exp visit(ArrayAccess arrayAccess) {
                return new ArrayAccess(subSt(arrayAccess.getBase()), subSt(arrayAccess.getIndex()));
            }

            @Override
            public Exp visit(NewArray newArray) {
                return new NewArray(newArray.getType(), subSt(newArray.getLength()));
            }

            @Override
            public Exp visit(NewMultiArray newMultiArray) {
                return new NewMultiArray(newMultiArray.getType(), subSt(newMultiArray.getLengths()));
            }

            @Override
            public Exp visit(InvokeInterface invoke) {
                return new InvokeInterface(invoke.getMethodRef(), subSt(invoke.getBase()), subSt(invoke.getArgs()));
            }

            @Override
            public Exp visit(InvokeSpecial invoke) {
                return new InvokeSpecial(invoke.getMethodRef(), subSt(invoke.getBase()), subSt(invoke.getArgs()));
            }

            @Override
            public Exp visit(InvokeStatic invoke) {
                return new InvokeStatic(invoke.getMethodRef(), subSt(invoke.getArgs()));
            }

            @Override
            public Exp visit(InvokeVirtual invoke) {
                return new InvokeVirtual(invoke.getMethodRef(), subSt(invoke.getBase()), subSt(invoke.getArgs()));
            }

            @Override
            public Exp visit(InvokeDynamic invoke) {
                return new InvokeDynamic(invoke.getBootstrapMethodRef(),
                        invoke.getMethodName(), invoke.getMethodType(), invoke.getBootstrapArgs(),
                        subSt(invoke.getArgs()));
            }

            @Override
            public Exp visit(ArrayLengthExp exp) {
                return new ArrayLengthExp(subSt(exp.getBase()));
            }

            @Override
            public Exp visit(NegExp exp) {
                return new NegExp(subSt(exp.getOperand()));
            }

            @Override
            public Exp visit(ArithmeticExp exp) {
                return new ArithmeticExp(exp.getOperator(), subSt(exp.getOperand1()), subSt(exp.getOperand2()));
            }

            @Override
            public Exp visit(BitwiseExp exp) {
                return new BitwiseExp(exp.getOperator(), subSt(exp.getOperand1()), subSt(exp.getOperand2()));
            }

            @Override
            public Exp visit(ComparisonExp exp) {
                return new ComparisonExp(exp.getOperator(), subSt(exp.getOperand1()), subSt(exp.getOperand2()));
            }

            @Override
            public Exp visit(ConditionExp exp) {
                return new ConditionExp(exp.getOperator(), subSt(exp.getOperand1()), subSt(exp.getOperand2()));
            }

            @Override
            public Exp visit(ShiftExp exp) {
                return new ShiftExp(exp.getOperator(), subSt(exp.getOperand1()), subSt(exp.getOperand2()));
            }

            @Override
            public Exp visit(InstanceOfExp exp) {
                return new InstanceOfExp(subSt(exp.getValue()), exp.getCheckedType());
            }

            @Override
            public Exp visit(CastExp exp) {
                return new CastExp(subSt(exp.getValue()), exp.getCastType());
            }

            @Override
            public Exp visitDefault(Exp exp) {
                assert exp.getUses().isEmpty();
                return exp;
            }
        });
    }

    public Stmt subSt(Stmt stmt) {
        Stmt newStmt = stmt.accept(new StmtVisitor<>() {
            @Override
            public Stmt visit(If stmt) {
                return new If((ConditionExp) subSt(stmt.getCondition()));
            }

            @Override
            public Stmt visit(TableSwitch stmt) {
                return new TableSwitch(subSt(stmt.getVar()), stmt.getLowIndex(), stmt.getHighIndex());
            }

            @Override
            public Stmt visit(LookupSwitch stmt) {
                return new LookupSwitch(subSt(stmt.getVar()), stmt.getCaseValues());
            }

            @Override
            public Stmt visit(Return stmt) {
                return new Return(subSt(stmt.getValue()));
            }

            @Override
            public Stmt visit(Throw stmt) {
                return new Throw(subSt(stmt.getExceptionRef()));
            }

            @Override
            public Stmt visit(Catch stmt) {
                return new Catch((Var) leftSubSt(stmt.getExceptionRef()));
            }

            @Override
            public Stmt visit(Monitor stmt) {
                return new Monitor(stmt.isEnter() ? Monitor.Op.ENTER : Monitor.Op.EXIT, subSt(stmt.getObjectRef()));
            }

            @Override
            public Stmt visit(PhiStmt stmt) {
                assert useSigma.isEmpty();
                return new PhiStmt(stmt.getBase(), defSubSt(stmt.getLValue()), stmt.getRValue());
            }

            @Override
            public Stmt visitDefault(Stmt stmt) {
                if (stmt instanceof AssignStmt<?, ?> stmt1) {
                    fixRelStmts(stmt);
                    return Utils.getAssignStmt(method, leftSubSt(stmt1.getLValue()), rightSubst(stmt1.getRValue()));
                } else if (stmt instanceof Invoke invoke) {
                    fixRelStmts(stmt);
                    return new Invoke(invoke.getContainer(), (InvokeExp) subSt(invoke.getInvokeExp()),
                            invoke.getLValue() == null ? null : (Var) leftSubSt(invoke.getLValue()));
                } else {
                    assert stmt instanceof Nop || stmt instanceof Goto;
                    return stmt;
                }
            }
        });

        newStmt.setLineNumber(stmt.getLineNumber());
        return newStmt;
    }

    private void fixRelStmts(Stmt oldStmt) {
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
