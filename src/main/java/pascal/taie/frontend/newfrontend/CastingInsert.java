package pascal.taie.frontend.newfrontend;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.type.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static pascal.taie.frontend.newfrontend.Utils.*;

public class CastingInsert {

    private final AsmIRBuilder builder;

    private static final Logger logger = LogManager.getLogger("Casting");

    private Stmt currentStmt;

    public CastingInsert(AsmIRBuilder builder) {
        this.builder = builder;
    }

    private Cast getNewCast(Var left, Var right, Type t) {
        logger.atInfo().log("[CASTING] Current stmt: " + currentStmt + "\n" +
                            "          Var " + right + " With Type: " + right.getType() + "\n" +
                            "          Excepted Type: " + t + "\n" +
                            "          In method: " + builder.method);
        return new Cast(left, new CastExp(right, t));
    }

    private Type maySplitStmt(LValue l, RValue r) {
        Type lType = l.getType();
        Type rType = r.getType();
        if (! isAssignable(lType, rType)) {
            return lType;
        } else {
            return null;
        }
    }

    public void build() {

        for (BytecodeBlock block : builder.getAllBlocks()) {

            List<Stmt> newStmts = new ArrayList<>();

            for (Stmt stmt : getStmts(block)) {
                currentStmt = stmt;
                Stmt newStmt = stmt.accept(new StmtVisitor<>() {
                    @Override
                    public Stmt visit(Copy stmt) {
                        Type t = maySplitStmt(stmt.getLValue(), stmt.getRValue());
                        if (t != null) {
                            return getNewCast(stmt.getLValue(), stmt.getRValue(), t);
                        } else {
                            return stmt;
                        }
                    }

                    @Override
                    public Stmt visit(LoadArray stmt) {
                        Type t = maySplitStmt(stmt.getLValue(), stmt.getRValue());
                        if (t != null) {
                            Var v = builder.manager.getTempVar();
                            v.setType(stmt.getRValue().getType());
                            newStmts.add(new LoadArray(v, stmt.getRValue()));
                            return getNewCast(stmt.getLValue(), v, t);
                        } else {
                            return stmt;
                        }
                    }

                    @Override
                    public Stmt visit(StoreArray stmt) {
                        Type t = maySplitStmt(stmt.getLValue(), stmt.getRValue());
                        if (t != null) {
                            Var v = builder.manager.getTempVar();
                            v.setType(t);
                            newStmts.add(getNewCast(v, stmt.getRValue(), t));
                            return new StoreArray(stmt.getLValue(), v);
                        } else {
                            return stmt;
                        }
                    }

                    @Override
                    public Stmt visit(StoreField stmt) {
                        Type t = maySplitStmt(stmt.getLValue(), stmt.getRValue());
                        if (t != null) {
                            Var v = builder.manager.getTempVar();
                            v.setType(t);
                            newStmts.add(getNewCast(v, stmt.getRValue(), t));
                            return new StoreField(stmt.getLValue(), v);
                        } else {
                            return stmt;
                        }
                    }

                    @Override
                    public Stmt visit(Invoke stmt) {
                        if (stmt.getRValue() instanceof InvokeInstanceExp invokeInstanceExp) {
                            JClass jClass = stmt.getRValue().getMethodRef().getDeclaringClass();
                            assert jClass != null;
                            Type t = jClass.getType();
                            if (!isAssignable(t, invokeInstanceExp.getBase().getType())) {
                                Var v = builder.manager.getTempVar();
                                v.setType(t);
                                newStmts.add(getNewCast(v, invokeInstanceExp.getBase(), t));
                                Lenses l = new Lenses(builder.method, Map.of(invokeInstanceExp.getBase(), v), Map.of());
                                return l.subSt(stmt);
                            } else {
                                return stmt;
                            }
                        }
                        return stmt;
                    }

                    @Override
                    public Stmt visit(Return stmt) {
                        Type t = builder.method.getReturnType();
                        if (stmt.getValue() != null &&
                                !isAssignable(t, stmt.getValue().getType())) {
                            Var v = builder.manager.getTempVar();
                            newStmts.add(getNewCast(v, stmt.getValue(), t));
                            builder.manager.getRetVars().remove(stmt.getValue());
                            builder.manager.getRetVars().add(v);
                            return new Return(v);
                        } else {
                            return stmt;
                        }
                    }

                    @Override
                    public Stmt visitDefault(Stmt stmt) {
                        return stmt;
                    }
                });

                newStmts.add(newStmt);
            }

            block.setStmts(newStmts);
        }
    }

    List<Stmt> getStmts(BytecodeBlock block) {
        return block.getStmts();
    }
}
