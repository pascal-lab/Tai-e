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

package pascal.taie.frontend.newfrontend.bcir;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.frontend.newfrontend.FrontendContext;
import pascal.taie.frontend.newfrontend.FrontendStmtVisitor;
import pascal.taie.frontend.newfrontend.Lenses;
import pascal.taie.frontend.newfrontend.Utils;
import pascal.taie.frontend.newfrontend.main.IRBuildingPhase;
import pascal.taie.frontend.newfrontend.main.NewFrontendIRComponent;
import pascal.taie.frontend.newfrontend.report.TaieCastingInfo;
import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.ExpMutator;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.InvokeInterface;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

public class CastingInsert extends NewFrontendIRComponent {

    private final BytecodeIRBuilder builder;

    private static final Logger logger = LogManager.getLogger(CastingInsert.class);

    private Stmt currentStmt;

    private final Map<FlowTypeInfo, Var> flowTypeCache;

    public CastingInsert(BytecodeIRBuilder builder, FrontendContext context) {
        super(context, IRBuildingPhase.BYTECODE_TYPE_INFERENCE);
        this.builder = builder;
        this.flowTypeCache = Maps.newHybridMap();
    }

    private boolean isAssignable(Type left, Type right) {
        return Utils.isAssignable(tCtx(), left, right);
    }

    private Cast getNewCast(Var left, Var right, Type t) {
        logger.atTrace().log("[CASTING] Current stmt: " + currentStmt + "\n" +
                            "          Var " + right + " With Type: " + right.getType() + "\n" +
                            "          Excepted Type: " + t + "\n" +
                            "          In method: " + builder.method);
        TaieCastingInfo info = new TaieCastingInfo(
                builder.method,
                currentStmt,
                t,
                right,
                right.getType()
        );
        ctx().getFrontendStats().castingInfos().put(builder.method, info);
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

    private Stmt ensureValidArrayType(ArrayAccess access, Stmt stmt, BytecodeBlock block, List<Stmt> newStmts) {
        Type t = typeSystem().getArrayType(tCtx().object(), 1);
        if (access.getBase().getType() instanceof ArrayType) {
            return stmt;
        } else {
            Var local = requireFlowTypeVar(block, access.getBase(), newStmts, t);
            if (local != null) {
                Lenses lenses = new Lenses(builder.method, Map.of(access.getBase(), local), Map.of());
                return lenses.subSt(stmt);
            } else {
                Var v1 = builder.manager.getTempVar();
                ExpMutator.setType(v1, t);
                newStmts.add(getNewCast(v1, access.getBase(), t));
                Lenses lenses = new Lenses(builder.method, Map.of(access.getBase(), v1), Map.of());
                return lenses.subSt(stmt);
            }
        }
    }

    private Stmt ensureValidFieldAccess(FieldAccess access, Stmt stmt, BytecodeBlock block, List<Stmt> newStmts) {
        if (access instanceof InstanceFieldAccess instance) {
            Var base = instance.getBase();
            Type t = instance.getFieldRef().getDeclaringClass().getType();
            if (isAssignable(t, base.getType())) {
                return stmt;
            } else {
                Var v1 = requireFlowTypeVar(block, base, newStmts, t);
                if (v1 != null) {
                    assert isAssignable(t, v1.getType());
                    Lenses lenses = new Lenses(builder.method, Map.of(base, v1), Map.of());
                    return lenses.subSt(stmt);
                } else {
                    v1 = builder.manager.getTempVar();
                    ExpMutator.setType(v1, t);
                    newStmts.add(getNewCast(base, v1, t));
                    Lenses lenses = new Lenses(builder.method, Map.of(base, v1), Map.of());
                    return lenses.subSt(stmt);
                }
            }
        } else {
            return stmt;
        }
    }

    public void build() {

        for (BytecodeBlock block : builder.blockSortedList) {

            List<Stmt> newStmts = new ArrayList<>(block.getStmts().size());

            for (Stmt stmt : getStmts(block)) {
                currentStmt = stmt;
                Stmt newStmt = stmt.accept(new FrontendStmtVisitor<>() {
                    @Override
                    public Stmt visit(Copy stmt) {
                        Var right = stmt.getRValue();
                        Type t = maySplitStmt(stmt.getLValue(), right);
                        if (t == NullType.NULL) {
                            return new AssignLiteral(stmt.getLValue(), NullLiteral.get());
                        }
                        if (t != null) {
                            Pair<DefinitionStmt<?, ?>, Integer> newInBlock =
                                    findNewInBlock(newStmts, right);
                            if (newInBlock != null) {
                                RValue rValue = newInBlock.first().getRValue();
                                if (isAssignable(t, rValue.getType())) {
                                    Var v = requireFlowTypeVar(block, right, newStmts, t);
                                    return stage1Transform(stmt, right, v);
                                }
                            }
                            return getNewCast(stmt.getLValue(), right, t);
                        } else {
                            return stmt;
                        }
                    }

                    @Override
                    public Stmt visit(LoadArray stmt) {
                        stmt = (LoadArray) ensureValidArrayType(stmt.getArrayAccess(), stmt, block, newStmts);
                        Type t = maySplitStmt(stmt.getLValue(), stmt.getRValue());
                        if (t != null) {
                            Var v = builder.manager.getTempVar();
                            ExpMutator.setType(v, stmt.getRValue().getType());
                            stmt.getRValue().getBase().removeRelevantStmt(stmt);
                            newStmts.add(new LoadArray(v, stmt.getRValue()));
                            return getNewCast(stmt.getLValue(), v, t);
                        } else {
                            return stmt;
                        }
                    }

                    @Override
                    public Stmt visit(StoreArray stmt) {
                        // stmt = (StoreArray) ensureValidArrayType(stmt.getArrayAccess(), stmt, block, newStmts);
                        // Type t = maySplitStmt(stmt.getLValue(), stmt.getRValue());
                        // if (t != null) {
                        //     Var v = builder.manager.getTempVar();
                        //     v.setType(t);
                        //     stmt.getLValue().getBase().removeRelevantStmt(stmt);
                        //     newStmts.add(getNewCast(v, stmt.getRValue(), t));
                        //     return new StoreArray(stmt.getLValue(), v);
                        // } else {
                        //     return stmt;
                        // }
                        // Ignore array store for now
                        // because it will throw `ArrayStoreException`,
                        // instead of `ClassCastException`,
                        // insert cast here may change runtime behavior
                        return stmt;
                    }

                    @Override
                    public Stmt visit(StoreField stmt) {
                        stmt = (StoreField) ensureValidFieldAccess(stmt.getFieldAccess(), stmt, block, newStmts);
                        Type t = maySplitStmt(stmt.getLValue(), stmt.getRValue());
                        if (t != null) {
                            Var v = builder.manager.getTempVar();
                            ExpMutator.setType(v, t);
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
                    public Stmt visit(LoadField stmt) {
                        return ensureValidFieldAccess(stmt.getFieldAccess(), stmt, block, newStmts);
                    }

                    @Override
                    public Stmt visit(Invoke stmt) {
                        Invoke prevStmt = stmt;
                        if (stmt.getRValue() instanceof InvokeInstanceExp invokeInstanceExp) {
                            JClass jClass = stmt.getRValue().getMethodRef().getDeclaringClass();
                            assert jClass != null;
                            Type t = jClass.getType();
                            Type baseType = invokeInstanceExp.getBase().getType();
                            Var base = invokeInstanceExp.getBase();
                            if (!isAssignable(t, baseType)) {
                                if (! (invokeInstanceExp instanceof InvokeInterface)) {
                                    // prev stmt is new
                                    // TODO: add tests for fallback
                                    Var v = requireFlowTypeVar(block, invokeInstanceExp.getBase(), newStmts, t);
                                    if (v != null) {
                                        // if this file is a valid bytecode class, then it's checked
                                        // which means flowType is always assignable to required type
                                        // assert isAssignable(t, v.getType());
                                        return stage1Transform(stmt, base, v);
                                    } else {
                                        logger.atTrace().log("[CASTING] fallback solution for stage1");
                                    }
                                }

                                Var v = builder.manager.getTempVar();
                                ExpMutator.setType(v, t);
                                newStmts.add(getNewCast(v, invokeInstanceExp.getBase(), t));
                                Lenses l = new Lenses(builder.method, Map.of(invokeInstanceExp.getBase(), v), Map.of());
                                prevStmt = (Invoke) l.subSt(stmt);
                            }
                        }
                        if (stmt.isDynamic()) {
                            return prevStmt;
                        }
                        Map<Var, Var> m = null;
                        for (int i = 0; i < prevStmt.getInvokeExp().getArgCount(); ++i) {
                            Var arg = prevStmt.getInvokeExp().getArg(i);
                            Type t = prevStmt.getMethodRef().getParameterTypes().get(i);
                            if (! isAssignable(t, arg.getType())) {
                                Var v = requireFlowTypeVar(block, arg, newStmts, t);
                                if (v != null) {
                                    prevStmt = (Invoke) stage1Transform(prevStmt, arg, v);
                                } else {
                                    v = builder.manager.getTempVar();
                                    ExpMutator.setType(v, t);
                                    newStmts.add(getNewCast(v, arg, t));
                                    if (m == null) {
                                        m = Maps.newMap();
                                    }
                                    m.put(arg, v);
                                }
                            }
                        }
                        if (m != null) {
                            Lenses l = new Lenses(builder.method, m, Map.of());
                            return l.subSt(prevStmt);
                        }
                        return prevStmt;
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
                            ExpMutator.setType(v, t);
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

    private Stmt stage1Transform(Stmt stmt, Var base, Var v) {
        Lenses l = new Lenses(builder.method, Map.of(base, v), Map.of(base, v));
        return l.subSt(stmt);
    }

    List<Stmt> getStmts(BytecodeBlock block) {
        return block.getStmts();
    }

    Pair<DefinitionStmt<?, ?>, Integer> findNewInBlock(List<Stmt> newStmts, Var target) {
        int idx = newStmts.size() - 1;
        for (; idx >= 0; idx--) {
            Stmt now = newStmts.get(idx);
            if (now instanceof DefinitionStmt<?, ?> newStmt && newStmt.getLValue() == target) {
                return new Pair<>(newStmt, idx);
            }
        }
        return null;
    }

    /**
     * Use this function to do global -> local conversion
     * @return offered variable with local type, maybe null
     */
    private Var requireFlowTypeVar(BytecodeBlock block, Var globalVar, List<Stmt> newStmts, Type targetType) {
        if (builder.isUSE_SSA()) {
            return null;
        }
        FlowTypeInfo info = new FlowTypeInfo(block, globalVar);
        if (flowTypeCache.containsKey(info)) {
            return flowTypeCache.get(info);
        } else {
            // 1. try to split new in this block
            //    e.g. a : java.lang.Object = new int[];
            //      => v1 = new int[];
            //         a = v1;
            //         v1 is the "flow type var"
            Pair<DefinitionStmt<?, ?>, Integer> newInBlock = findNewInBlock(newStmts, globalVar);
            if (newInBlock != null) {
                DefinitionStmt<?, ?> def = newInBlock.first();
                if (!isAssignable(targetType, def.getRValue().getType())) {
                    return null;
                }

                if (def instanceof Copy copy) {
                    return copy.getRValue();
                }
                Var v1 = builder.manager.getTempVar();
                ExpMutator.setType(v1, def.getRValue().getType());
                Lenses lenses = new Lenses(builder.method, Map.of(), Map.of(globalVar, v1));
                newStmts.set(newInBlock.second(), lenses.subSt(def));
                newStmts.add(newInBlock.second() + 1, new Copy(globalVar, v1));
                flowTypeCache.put(info, v1);
                return v1;
            }

            // TODO: 2. if frame is available,
            if (builder.isFrameUsable()) {
                return null;
            }

            // can't get flow type, use worse solution
            return null;
        }
    }

    private record FlowTypeInfo(BytecodeBlock block, Var var) { }
}
