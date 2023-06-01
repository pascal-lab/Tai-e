package pascal.taie.frontend.newfrontend;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.MethodNames;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Pair;

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
                            stmt.getRValue().getBase().removeRelevantStmt(stmt);
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
                            stmt.getLValue().getBase().removeRelevantStmt(stmt);
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
                            if (stmt.getFieldAccess() instanceof InstanceFieldAccess access) {
                                access.getBase().removeRelevantStmt(stmt);
                            }
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
                            Type baseType = invokeInstanceExp.getBase().getType();
                            Var base = invokeInstanceExp.getBase();
                            if (!isAssignable(t, baseType)) {
                                if (stmt.getMethodRef().getName().equals(MethodNames.INIT)) {
                                    // prev stmt is new
                                    // TODO: add tests for fallback
                                    Pair<New, Integer> find = findNewInBlock(newStmts, base);
                                    if (find != null) {
                                        Stmt newStmt = find.first();
                                        int idx = find.second();
                                        Var v = builder.manager.getTempVar();
                                        v.setType(t);
                                        Lenses l = new Lenses(builder.method, Map.of(base, v), Map.of(base, v));
                                        Stmt invoke = l.subSt(stmt);
                                        Stmt newStmtReal = l.subSt(newStmt);
                                        newStmts.set(idx, newStmtReal);
                                        newStmts.add(invoke);
                                        Copy cp = new Copy(base, v);
                                        // note: if new stmt is find, i.e.
                                        //    v = new C;
                                        //    ...  (v can not occur in here, or we will not perform this transform)
                                        //    invokespecical v.<init>();
                                        // then, C <: type(v) is confirmed. So, after transform
                                        //  (1)  v1 = new C;
                                        //       ...
                                        //  (2)  invokespecial v1.<init>();
                                        //  (3)  v = v1;
                                        // type(v1) = C, type(v1) <: type(v).
                                        // So there is no need to check the validity of (3).
                                        assert isAssignable(cp.getLValue().getType(),
                                                cp.getRValue().getType());
                                        return cp;
                                    } else {
                                        logger.atInfo().log("[CASTING] fallback solution for stage1");
                                    }
                                }

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

    Pair<New, Integer> findNewInBlock(List<Stmt> newStmts, Var target) {
        int idx = newStmts.size() - 1;
        for (; idx >= 0; idx--) {
            Stmt now = newStmts.get(idx);
            if (now instanceof New newStmt && newStmt.getLValue() == target) {
                return new Pair<>(newStmt, idx);
            } else if (now.getUses().contains(target)
                || now.getDef().isPresent() && now.getDef().get() == target) {
                return null;
            }
        }
        return null;
    }
}
