package pascal.taie.frontend.newfrontend;

import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.ArrayLengthExp;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.ComparisonExp;
import pascal.taie.ir.exp.ConditionExp;
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
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.MethodNames;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static pascal.taie.frontend.newfrontend.Utils.*;

public class TypeInference0 {

    private final AsmIRBuilder builder;

    private final List<Set<Type>> localTypeConstrains;

    private final List<Set<Type>> localTypeAssigns;

    private final int[] localCells;

    private final int varSize;

    public TypeInference0(AsmIRBuilder builder) {
        this.builder = builder;
        varSize = builder.manager.getVars().size();

        localTypeConstrains = new ArrayList<>(Collections.nCopies(varSize, null));
        localTypeAssigns = new ArrayList<>(Collections.nCopies(varSize, null));
        localCells = builder.manager.getSlotTable();
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
            // assert ! (now instanceof Uninitialized);
            v.setType(now);
        }
    }

    private Type computeLocalType(Var v) {
        Set<Type> allTypes = localTypeAssigns.get(v.getIndex());
        assert !allTypes.isEmpty();
        Type now = allTypes.iterator().next();
        if (allTypes.size() == 1) {
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
                assert allTypes.stream().allMatch(t1 -> t1 instanceof ReferenceType);
                Set<ReferenceType> res = lca(allTypes.stream()
                        .filter(t1 -> ! (t1 instanceof NullType) && ! (t1 instanceof Uninitialized))
                        .map(i -> (ReferenceType) i)
                        .collect(Collectors.toSet()));
                if (res.isEmpty())  {
                    now = Utils.getObject();
                } else if (res.size() == 1) {
                    now = res.iterator().next();
                } else {
                    now = null;
                    int count = -1;
                    for (ReferenceType type : res) {
                        int newCount = (int) constrains.stream()
                                .filter(c -> isAssignable(c, type)).count();
                        if (newCount > count) {
                            now = type;
                            count = newCount;
                        }
                    }
                    if (now == null) {
                        now = getObject();
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
                assert slot != -1;
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
        }
        else if (builder.manager.isNotSpecialVar(var)) {
            var.setType(t);
        }
    }

    private boolean isLocal(Var v) {
        return localTypeConstrains.get(v.getIndex()) != null
                || localCells[v.getIndex()] != -1;
    }

    private void setType(Typing typing, Var v, Type t) {
        typing.setType(v, t);
        setTypeForTemp(v, t);
    }

    private void newTypeAssign(Var var, Type t, Typing typing) {
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
        setType(typing, var, resultType);
    }

    private void newTypeArrayLoad(Var target, ArrayAccess array, Typing typing) {
        Var base = array.getBase();
        Type t = getType(typing, base);
        if (t instanceof ArrayType arrayType) {
            Type targetType = arrayType.elementType();
            setType(typing, target, targetType);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private Set<BytecodeBlock> visited;

    public void build() {
        buildLocalTypes();
        setThisParamRet();
        inferTypes();
        setTypeForLocal();
        CastingInsert insert = new CastingInsert(builder);
        insert.build();
    }

    private void buildLocalTypes() {
        for (BytecodeBlock block : builder.blockSortedList) {
            if (block.getFrame() != null) {
                block.getInitTyping().forEach((k, v) -> {
                    if (v != Uninitialized.UNINITIALIZED) {
                        putMultiSet(localTypeConstrains, k, v);
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

    private void setThisParamRet() {
        JMethod m = this.builder.method;
        if (! m.isStatic()) {
            Var thisVar = this.builder.manager.getThisVar();
            assert thisVar != null;
            thisVar.setType(m.getDeclaringClass().getType());
        }
        for (int i = 0; i < m.getParamCount(); ++i) {
            Var paramI = this.builder.manager.getParams().get(i);
            Type typeI = m.getParamTypes().get(i);
            paramI.setType(typeI);
        }
    }

    private void inferTypes() {
        visited = Sets.newHybridSet();
        for (BytecodeBlock block : builder.blockSortedList) {
            Type[] initTyping;
            initTyping = new Type[varSize];
            List<Object> frameLocalType;
            if (! visited.contains(block)) {
                if (block.inEdges().isEmpty() && ! block.isCatch()) {
                    frameLocalType = List.of();
                } else {
                    var tempInitTyping = block.getInitTyping();
                    tempInitTyping.forEach((k, v) -> {
                        initTyping[k.getIndex()] = v;
                    });
                    frameLocalType = block.getFrameLocalType();
                }
                inferTypesForBlock(block, new Typing(initTyping, frameLocalType));
            }
        }
    }

    private void inferTypesForBlock(BytecodeBlock block, Typing typing) {
        visited.add(block);
        for (Stmt stmt : getStmts(block)) {
            stmt.accept(new StmtVisitor<Void> () {
                @Override
                public Void visit(New stmt) {
                    newTypeAssign(stmt.getLValue(), stmt.getRValue().getType(), typing);
                    return StmtVisitor.super.visit(stmt);
                }

                @Override
                public Void visit(AssignLiteral stmt) {
                    Literal l = stmt.getRValue();
                    Type t;
                    if (l instanceof StringLiteral) {
                        t = Utils.getString();
                    } else if (l instanceof ClassLiteral) {
                        t = Utils.getClassType(ClassNames.CLASS);
                    } else {
                        t = l.getType();
                    }
                    newTypeAssign(stmt.getLValue(), t, typing);
                    return StmtVisitor.super.visit(stmt);
                }

                @Override
                public Void visit(Copy stmt) {
                    newTypeAssign(stmt.getLValue(), List.of(stmt.getRValue()), typing);
                    return StmtVisitor.super.visit(stmt);
                }

                @Override
                public Void visit(LoadArray stmt) {
                    newTypeArrayLoad(stmt.getLValue(), stmt.getRValue(), typing);
                    return StmtVisitor.super.visit(stmt);
                }

                @Override
                public Void visit(StoreArray stmt) {
                    Var base = stmt.getArrayAccess().getBase();
                    if (isLocal(base)) {
                        Type t = getType(typing, stmt.getRValue());
                        if (t instanceof ReferenceType referenceType && referenceType != NullType.NULL) {
                            putMultiSet(localTypeAssigns, base, wrap1(referenceType));
                        }
                    }
                    return StmtVisitor.super.visit(stmt);
                }

                @Override
                public Void visit(LoadField stmt) {
                    newTypeAssign(stmt.getLValue(), stmt.getRValue().getType(), typing);
                    addConstrainsForFieldAccess(stmt.getFieldAccess());
                    return StmtVisitor.super.visit(stmt);
                }

                @Override
                public Void visit(StoreField stmt) {
                    addConstrainsForFieldAccess(stmt.getFieldAccess());
                    return StmtVisitor.super.visit(stmt);
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
                    return StmtVisitor.super.visit(stmt);
                }

                @Override
                public Void visit(Unary stmt) {
                    if (stmt.getRValue() instanceof ArrayLengthExp arrayLengthExp) {
                        newTypeAssign(stmt.getLValue(), arrayLengthExp.getType(), typing);
                    } else {
                        newTypeAssign(stmt.getLValue(), List.of(stmt.getRValue().getOperand()), typing);
                    }
                    return StmtVisitor.super.visit(stmt);
                }

                @Override
                public Void visit(InstanceOf stmt) {
                    newTypeAssign(stmt.getLValue(), stmt.getRValue().getType(), typing);
                    return StmtVisitor.super.visit(stmt);
                }

                @Override
                public Void visit(Cast stmt) {
                    newTypeAssign(stmt.getLValue(), stmt.getRValue().getType(), typing);
                    return StmtVisitor.super.visit(stmt);
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

                    if (! (exp instanceof InvokeDynamic)) {
                        List<Var> params = exp.getArgs();
                        List<Type> paramTypes = exp.getMethodRef().getParameterTypes();
                        for (int i = 0; i < exp.getArgCount(); ++i) {
                            Var v = params.get(i);
                            Type t = paramTypes.get(i);
                            addTypeConstrain(v, t);
                        }
                    }
                    return StmtVisitor.super.visit(stmt);
                }

                @Override
                public Void visit(Return stmt) {
                    if (stmt.getValue() != null) {
                        addTypeConstrain(stmt.getValue(), builder.method.getReturnType());
                    }
                    return StmtVisitor.super.visit(stmt);
                }

                @Override
                public Void visit(Catch stmt) {
                    Type t = block.getExceptionHandlerType();
                    assert t != null;
                    newTypeAssign(stmt.getExceptionRef(), t, typing);
                    return StmtVisitor.super.visit(stmt);
                }
            });
        }

        if (block.fallThrough() != null && block.fallThrough().getFrame() == null) {
            inferTypesForBlock(block.fallThrough(), typing);
        }
    }

    List<Stmt> getStmts(BytecodeBlock block) {
        return block.getStmts();
    }

    private record Typing(Type[] typing, List<Object> frameLocalType) {
        Type getType(Var v) {
            return typing[v.getIndex()];
        }

        void setType(Var v, Type t) {
            typing[v.getIndex()] = t;
        }
    }

}
