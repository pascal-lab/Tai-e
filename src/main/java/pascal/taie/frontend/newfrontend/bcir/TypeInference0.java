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

import pascal.taie.frontend.newfrontend.FrontendContext;
import pascal.taie.frontend.newfrontend.FrontendStmtVisitor;
import pascal.taie.frontend.newfrontend.Uninitialized;
import pascal.taie.frontend.newfrontend.Utils;
import pascal.taie.frontend.newfrontend.main.IRBuildingPhase;
import pascal.taie.frontend.newfrontend.main.NewFrontendIRComponent;
import pascal.taie.frontend.newfrontend.ssa.FrontendPhiStmt;
import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.ArrayLengthExp;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.ComparisonExp;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.ExpMutator;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.ShiftExp;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Binary;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.InstanceOf;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.ir.stmt.Unary;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.MethodNames;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static pascal.taie.frontend.newfrontend.Utils.canHoldsInt;
import static pascal.taie.frontend.newfrontend.Utils.fromAsmFrameType;
import static pascal.taie.frontend.newfrontend.Utils.isAssignable;
import static pascal.taie.frontend.newfrontend.Utils.lca;
import static pascal.taie.frontend.newfrontend.Utils.wrap1;
import static pascal.taie.language.type.BooleanType.BOOLEAN;
import static pascal.taie.language.type.ByteType.BYTE;
import static pascal.taie.language.type.CharType.CHAR;
import static pascal.taie.language.type.IntType.INT;
import static pascal.taie.language.type.ShortType.SHORT;

// TODO: moving to newfrontend.typing package

/**
 * Type inference based on stack map frames
 */
public class TypeInference0 extends NewFrontendIRComponent {

    private final BytecodeIRBuilder builder;

    private final List<Set<Type>> localTypeConstrains;

    private final List<Set<Type>> localTypeAssigns;

    private final int[] localCells;

    private final int varSize;

    private final ClassType stringType;

    public TypeInference0(BytecodeIRBuilder builder, FrontendContext context) {
        super(context, IRBuildingPhase.BYTECODE_TYPE_INFERENCE);
        this.builder = builder;
        varSize = builder.manager.getVars().size();

        localTypeConstrains = new ArrayList<>(varSize);
        for (int i = 0; i < varSize; ++i) {
            localTypeConstrains.add(null);
        }
        localTypeAssigns = new ArrayList<>(varSize);
        for (int i = 0; i < varSize; ++i) {
            localTypeAssigns.add(null);
        }
        localCells = builder.manager.getSlotTable();
        stringType = tCtx().string();
    }

    private void putMultiSet(List<Set<Type>> set, Var v, Type t) {
        if (set.get(v.getIndex()) == null) {
            set.set(v.getIndex(), Sets.newHybridSet());
        }
        set.get(v.getIndex()).add(t);
    }

    private void setTypeForLocal() {
        for (int i = 0; i < varSize; ++i) {
            Var v = builder.manager.getVars().get(i);
            if (v.getType() != null) {
                continue;
            }
            if (localTypeAssigns.get(i) == null) {
                continue;
            }
            Type now = computeLocalType(v);
            assert ! (now instanceof Uninitialized);
            ExpMutator.setType(v, now);
        }
    }

    private Type computeLocalType(Var v) {
        Set<Type> allTypes = localTypeAssigns.get(v.getIndex());
        assert !allTypes.isEmpty();
        Type now = allTypes.iterator().next();
        if (allTypes.size() == 1) {
            if (now == Uninitialized.UNINITIALIZED) {
                return tCtx().object();
            }
            if (now == tCtx().object()) {
                Set<Type> constrains = localTypeConstrains.get(v.getIndex());
                if (constrains != null && constrains.size() == 1) {
                    return constrains.iterator().next();
                }
            }
            return now;
        }
        Set<Type> constrains = localTypeConstrains.get(v.getIndex());
        if (constrains == null) {
            constrains = Set.of();
        }
        for (Type t : allTypes) {
            if (now instanceof PrimitiveType) {
                assert t == now || canHoldsInt(t) && canHoldsInt(now);
            } else {
                assert allTypes.stream().allMatch(t1 -> t1 instanceof ReferenceType ||
                        t1 instanceof Uninitialized);
                Set<ReferenceType> res = lca(tCtx(),
                        allTypes.stream()
                        .filter(t1 -> ! (t1 instanceof NullType) && ! (t1 instanceof Uninitialized))
                        .map(i -> (ReferenceType) i)
                        .collect(Collectors.toSet()));
                if (res.isEmpty())  {
                    now = tCtx().object();
                } else if (res.size() == 1) {
                    now = res.iterator().next();
                } else {
                    now = null;
                    int count = -1;
                    for (ReferenceType type : res) {
                        int newCount = (int) constrains.stream()
                                .filter(c -> isAssignable(tCtx(), c, type)).count();
                        if (newCount > count) {
                            now = type;
                            count = newCount;
                        }
                    }
                    if (now == null) {
                        now = tCtx().object();
                    }
                }
                break;
            }
        }
        return now;
    }

    private Type getType(Typing typing, Var v) {
        if (v.getType() != null) {
            return v.getType();
        } else {
            Type t = typing.getType(v);
            if (t == null) {
                int slot = localCells[v.getIndex()];
                assert slot != -1 && slot < typing.frameLocalType().size();
                Object frameLocalType = typing.frameLocalType().get(slot);
                Type currentType = fromAsmFrameType(frameLocalType);
                typing.setType(v, currentType);
                putMultiSet(localTypeConstrains, v, currentType);
                return currentType;
            } else {
                return t;
            }
        }
    }

    private void setTypeForTemp(Var var, Type t) {
        if (isLocal(var)) {
            putMultiSet(localTypeAssigns, var, t);
        } else {
            if (var.getType() != null) {
//                assert !builder.manager.isNotSpecialVar(var);
                return;
            }
            if (t == Uninitialized.UNINITIALIZED) {
                ExpMutator.setType(var, tCtx().object());
            } else {
                ExpMutator.setType(var, t);
            }
        }
    }

    private boolean isLocal(Var v) {
        return !builder.varSSAInfo.isSSAVar(v);
    }

    private void setType(Typing typing, Var v, Type t) {
        typing.setType(v, t);
        setTypeForTemp(v, t);
    }

    private void newTypeAssign(Var var, Type t, Typing typing) {
        if (var.getType() != null) {
            return;
        }
        setType(typing, var, t);
    }

    private void newTypeAssign(Var var, List<Var> rValues, Typing typing) {
        // Reference impl:
        // types = rValues
        //         .map(v -> getType(typing, v))
        //         .distinct()
        //         .toList();
        List<Type> types = new ArrayList<>(rValues.size());
        for (Var v : rValues) {
            Type t = getType(typing, v);
            if (!types.contains(t)) {
                types.add(t);
            }
        }
        assert types.size() == 1 ||
                types.stream().allMatch(Utils::canHoldsInt);
        Type resultType = types.get(0);
        if (resultType == BOOLEAN || resultType == BYTE ||
                resultType == SHORT || resultType == CHAR) {
            resultType = INT;
        }
        setType(typing, var, resultType);
    }

    private void newTypeArrayLoad(Var target, ArrayAccess array, Typing typing) {
        Var base = array.getBase();
        Type t = getType(typing, base);
        if (t instanceof ArrayType arrayType) {
            Type targetType = arrayType.elementType();
            setType(typing, target, targetType);
        } else if (t instanceof NullType) {
            setType(typing, target, t);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public void build() {
        buildLocalTypes();
        inferTypes();
        setTypeForLocal();
        CastingInsert insert = new CastingInsert(builder, ctx());
        insert.build();
    }

    private void buildLocalTypes() {
        for (BytecodeBlock block : builder.blockSortedList) {
            if (block.getFrame() != null) {
                block.visitInitTyping((v, t) -> {
                    if (t != Uninitialized.UNINITIALIZED) {
                        putMultiSet(localTypeConstrains, v, t);
                    }
                });
            }
        }
    }

    private void addConstrainsForFieldAccess(FieldAccess access) {
        if (access instanceof InstanceFieldAccess instanceFieldAccess) {
            Var base = instanceFieldAccess.getBase();
            addTypeConstrain(base, instanceFieldAccess.getFieldRef().getDeclaringClass().getType());
        }
    }

    private void addTypeConstrain(Var base, Type constrain) {
        if (isLocal(base)) {
            putMultiSet(localTypeConstrains, base, constrain);
        }
    }

    private void setThisParamRet(Typing typing) {
        JMethod m = this.builder.method;
        if (! m.isStatic()) {
            Var thisVar = this.builder.manager.getThisVar();
            assert thisVar != null;
            setType(typing, thisVar, m.getDeclaringClass().getType());
        }
        for (int i = 0; i < m.getParamCount(); ++i) {
            Var paramI = this.builder.manager.getParams().get(i);
            Type typeI = m.getParamTypes().get(i);
            setType(typing, paramI, typeI);
        }
    }

    private void inferTypes() {
//        visited = new boolean[builder.blockSortedList.size()];
//        for (BytecodeBlock block : builder.blockSortedList) {
//            if (!visited[block.getIndex()]) {
//                inferTypesForBlock(block, getBlockInitTyping(block, varSize));
//            }
//        }
        int[] postOrder = builder.getPostOrder();
        Typing t = new Typing(new Type[varSize], List.of());
        setThisParamRet(t);
        for (int index = postOrder.length - 1; index >= 0; --index) {
            int i = postOrder[index];
            BytecodeBlock block = builder.blockSortedList.get(i);
            if (block.getFrame() != null) {
                t = loadNewBlockTyping(block, t);
            }
            inferTypesForBlock(block, t);
        }
    }

    Typing getBlockInitTyping(BytecodeBlock block, int varSize) {
        List<Object> frameLocalType;
        Type[] initTyping = new Type[varSize];
        if (builder.isInEdgeEmpty(block) && ! block.isCatch()) {
            frameLocalType = List.of();
        } else {
            block.visitInitTyping((v, t) -> {
                initTyping[v.getIndex()] = t;
            });
            frameLocalType = block.getFrameLocalType();
        }
        return new Typing(initTyping, frameLocalType);
    }

    Typing loadNewBlockTyping(BytecodeBlock block, Typing typing) {
        Type[] newTyping = typing.typing;
        // only NON-SSA need this, it will cause problem in SSA
        if (!builder.isUSE_SSA()) {
            for (int i = 0; i < varSize; ++i) {
                int slot = localCells[i];
                if (slot != -1) {
                    newTyping[i] = null;
                }
            }
        }
        block.visitInitTyping((k, v) -> {
            if (v != Uninitialized.UNINITIALIZED) {
                newTyping[k.getIndex()] = v;
            }
        });
        typing.setFrameLocalType(block.getFrameLocalType());
        return typing;
    }

    private void inferTypesForBlock(BytecodeBlock block, Typing typing) {
        StmtVisitor<Void> visitor = new FrontendStmtVisitor<>() {
            @Override
            public Void visit(New stmt) {
                newTypeAssign(stmt.getLValue(), stmt.getRValue().getType(), typing);
                return null;
            }

            @Override
            public Void visit(AssignLiteral stmt) {
                Literal l = stmt.getRValue();
                Type t;
                if (l instanceof StringLiteral) {
                    t = stringType;
                } else if (l instanceof ClassLiteral) {
                    t = tCtx().klass();
                } else {
                    t = l.getType();
                }
                newTypeAssign(stmt.getLValue(), t, typing);
                return null;
            }

            @Override
            public Void visit(Copy stmt) {
                newTypeAssign(stmt.getLValue(), List.of(stmt.getRValue()), typing);
                return null;
            }

            @Override
            public Void visit(LoadArray stmt) {
                newTypeArrayLoad(stmt.getLValue(), stmt.getRValue(), typing);
                return null;
            }

            @Override
            public Void visit(StoreArray stmt) {
                Var base = stmt.getArrayAccess().getBase();
                if (isLocal(base)) {
                    Type t = getType(typing, stmt.getRValue());
                    // TODO: this rule is useless, and may not be safe
                    //       But currently works well, check & remove this in the future
                    if (t instanceof ReferenceType referenceType && referenceType != NullType.NULL) {
                        putMultiSet(localTypeAssigns, base, wrap1(tCtx(), referenceType));
                    }
                }
                return null;
            }

            @Override
            public Void visit(LoadField stmt) {
                newTypeAssign(stmt.getLValue(), stmt.getRValue().getType(), typing);
                addConstrainsForFieldAccess(stmt.getFieldAccess());
                return null;
            }

            @Override
            public Void visit(StoreField stmt) {
                addConstrainsForFieldAccess(stmt.getFieldAccess());
                addTypeConstrain(stmt.getRValue(), stmt.getFieldAccess().getFieldRef().getType());
                return null;
            }

            @Override
            public Void visit(Binary stmt) {
                BinaryExp binaryExp = stmt.getRValue();
                if (binaryExp instanceof ConditionExp || binaryExp instanceof ComparisonExp) {
                    newTypeAssign(stmt.getLValue(), binaryExp.getType(), typing);
                } else if (binaryExp instanceof ShiftExp shiftExp) {
                    assert canHoldsInt(getType(typing, shiftExp.getOperand2()));
                    newTypeAssign(stmt.getLValue(), getType(typing, shiftExp.getOperand1()), typing);
                } else {
                    newTypeAssign(stmt.getLValue(), List.of(stmt.getRValue().getOperand1(),
                            stmt.getRValue().getOperand2()), typing);
                }
                return null;
            }

            @Override
            public Void visit(Unary stmt) {
                if (stmt.getRValue() instanceof ArrayLengthExp arrayLengthExp) {
                    newTypeAssign(stmt.getLValue(), arrayLengthExp.getType(), typing);
                } else {
                    newTypeAssign(stmt.getLValue(), List.of(stmt.getRValue().getOperand()), typing);
                }
                return null;
            }

            @Override
            public Void visit(InstanceOf stmt) {
                newTypeAssign(stmt.getLValue(), stmt.getRValue().getType(), typing);
                return null;
            }

            @Override
            public Void visit(Cast stmt) {
                newTypeAssign(stmt.getLValue(), stmt.getRValue().getType(), typing);
                return null;
            }

            @Override
            public Void visit(Invoke stmt) {
                if (stmt.getLValue() != null) {
                    newTypeAssign(stmt.getLValue(), stmt.getRValue().getType(), typing);
                }

                InvokeExp exp = stmt.getInvokeExp();
                if (exp instanceof InvokeInstanceExp invokeInstanceExp) {
                    // TODO: use better rule
                    if (!invokeInstanceExp.getMethodRef().getName().equals(MethodNames.INIT)) {
                        addTypeConstrain(invokeInstanceExp.getBase(),
                                invokeInstanceExp.getMethodRef().getDeclaringClass().getType());
                    }
                }

                if (!(exp instanceof InvokeDynamic)) {
                    List<Var> params = exp.getArgs();
                    List<Type> paramTypes = exp.getMethodRef().getParameterTypes();
                    for (int i = 0; i < exp.getArgCount(); ++i) {
                        Var v = params.get(i);
                        Type t = paramTypes.get(i);
                        addTypeConstrain(v, t);
                    }
                }
                return null;
            }

            @Override
            public Void visit(Return stmt) {
                if (stmt.getValue() != null) {
                    addTypeConstrain(stmt.getValue(), builder.method.getReturnType());
                }
                return null;
            }

            @Override
            public Void visit(Catch stmt) {
                List<ClassType> exceptionTypes = block.getExceptionHandlerTypes();
                assert exceptionTypes != null;
                // calculate lca here
                Set<ReferenceType> res = lca(tCtx(), Sets.newSet(exceptionTypes));
                ReferenceType type = res.isEmpty() ? tCtx().throwable() : res.iterator().next();
                newTypeAssign(stmt.getExceptionRef(), type, typing);
                return null;
            }

            @Override
            public Void visit(FrontendPhiStmt stmt) {
                Var base = stmt.getBase();
                int slot = localCells[base.getIndex()];
                if (slot != -1) {
                    // load from frame
                    assert slot < typing.frameLocalType().size();
                    Type t = fromAsmFrameType(typing.frameLocalType().get(slot));
                    // DON'T use newTypeAssign, it will lose precision
                    // frame type is type constrain, not type assign
                    typing.setType(stmt.getLValue(), t);
                }
                return null;
            }
        };
        for (Stmt stmt : getStmts(block)) {
            stmt.accept(visitor);
        }

        if (builder.isUSE_SSA()) {
            // add type assigns for phi stmts
            addPhiAssigns(block, typing);
        }
    }

    private void addPhiAssigns(BytecodeBlock block, Typing typing) {
        for (int i = 0; i < builder.getMergedOutEdgesCount(block); ++i) {
            BytecodeBlock succ = builder.getMergedOutEdge(block, i);
            for (Stmt stmt : getStmts(succ)) {
                if (stmt instanceof Catch) {
                    continue;
                }
                if (stmt instanceof FrontendPhiStmt frontendPhiStmt) {
                    Var v = frontendPhiStmt.getLValue();
                    if (v.getType() != null) {
                        continue;
                    }
                    Var var = frontendPhiStmt.getRValue().findVar(block);
                    Set<Type> assigns = localTypeAssigns.get(var.getIndex());
                    if (assigns != null) {
                        for (Type t : assigns) {
                            putMultiSet(localTypeAssigns, v, t);
                        }
                    } else {
                        Type t = getType(typing, var);
                        putMultiSet(localTypeAssigns, v, t);
                    }
                } else {
                    // no more phi stmts
                    break;
                }
            }
        }
    }

    List<Stmt> getStmts(BytecodeBlock block) {
        return block.getStmts();
    }

    static final class Typing {
        private final Type[] typing;
        private List<Object> frameLocalType;

        Typing(Type[] typing, List<Object> frameLocalType) {
            this.typing = typing;
            this.frameLocalType = frameLocalType;
        }

        Type getType(Var v) {
            return typing[v.getIndex()];
        }

        void setType(Var v, Type t) {
            typing[v.getIndex()] = t;
        }

        public List<Object> frameLocalType() {
            return frameLocalType;
        }

        public void setFrameLocalType(List<Object> frameLocalType) {
            this.frameLocalType = frameLocalType;
        }

        @Override
        public String toString() {
            return "Typing{" +
                    "typing=" + java.util.Arrays.toString(typing) +
                    ", frameLocalType=" + frameLocalType +
                    '}';
        }
    }

}
