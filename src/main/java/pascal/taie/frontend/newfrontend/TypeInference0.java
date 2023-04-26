package pascal.taie.frontend.newfrontend;

import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.ArrayLengthExp;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.ComparisonExp;
import pascal.taie.ir.exp.ConditionExp;
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
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.ir.stmt.Unary;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TypeInference0 {

    AsmIRBuilder builder;
    MultiMap<Var, Type> localTypes;

    public TypeInference0(AsmIRBuilder builder) {
        this.builder = builder;
        localTypes = Maps.newMultiMap();
    }

    private boolean isSubtype(Type supertype, Type subtype) {
        ClassHierarchy hierarchy = BuildContext.get().getClassHierarchy();
        ClassType OBJECT = Utils.getObject();
        ClassType CLONEABLE = Utils.getCloneable();
        ClassType SERIALIZABLE = Utils.getSerializable();

        if (subtype.equals(supertype)) {
            return true;
        } else if (subtype instanceof NullType) {
            return supertype instanceof ReferenceType;
        } else if (subtype instanceof ClassType) {
            if (supertype instanceof ClassType) {
                return hierarchy.isSubclass(
                        ((ClassType) supertype).getJClass(),
                        ((ClassType) subtype).getJClass());
            }
        } else if (subtype instanceof ArrayType) {
            if (supertype instanceof ClassType) {
                // JLS (11 Ed.), Chapter 10, Arrays
                return supertype == OBJECT ||
                        supertype == CLONEABLE ||
                        supertype == SERIALIZABLE;
            } else if (supertype instanceof ArrayType superArray) {
                ArrayType subArray = (ArrayType) subtype;
                Type superBase = superArray.baseType();
                Type subBase = subArray.baseType();
                if (superArray.dimensions() == subArray.dimensions()) {
                    if (subBase.equals(superBase)) {
                        return true;
                    } else if (superBase instanceof ClassType &&
                            subBase instanceof ClassType) {
                        return hierarchy.isSubclass(
                                ((ClassType) superBase).getJClass(),
                                ((ClassType) subBase).getJClass());
                    }
                } else if (superArray.dimensions() < subArray.dimensions()) {
                    return superBase == OBJECT ||
                            superBase == CLONEABLE ||
                            superBase == SERIALIZABLE;
                }
            }
        }
        return false;
    }

    /**
     * @return if <code>t1 <- t2</code> is valid
     */
    private boolean isAssignable(Type t1, Type t2) {
        if (t1 == PrimitiveType.INT) {
            return canHoldsInt(t2);
        } else {
            return isSubtype(t1, t2);
        }
    }

    private boolean canHoldsInt(Type t) {
        return t instanceof PrimitiveType p && p.asInt();
    }

    private void setTypeForTemp(Var var, Type t) {
        if (! localTypes.containsKey(var)
                && ! builder.manager.getRetVars().contains(var)
                && ! builder.manager.isSpecialVar(var)) {
            var.setType(t);
        }
    }

    private void setTypeForLocal() {
        for (Var v : localTypes.keySet()) {
            if (v.getType() != null) {
                continue;
            }
            Set<Type> allTypes = localTypes.get(v);
            assert allTypes.size() > 0;
            Type now = allTypes.iterator().next();
            if (allTypes.size() == 1) {
                v.setType(now);
                continue;
            }
            for (Type t : allTypes) {
                if (now instanceof PrimitiveType) {
                    assert t == now || canHoldsInt(t) && canHoldsInt(now);
                } else {
                    assert now instanceof ReferenceType;
                    assert t instanceof ReferenceType;
                    Set<ReferenceType> set =
                            Utils.lca((ReferenceType) now, (ReferenceType) t);
                    if (set.size() == 1) {
                        now = set.iterator().next();
                    } else {
                        now = Utils.getObject();
                        break;
                    }
                }
            }
            v.setType(now);
        }
    }

    private Type getType(Map<Var, Type> typing, Var v) {
        if (v.getType() != null) {
            return v.getType();
        } else {
            Type t = typing.get(v);
            assert t != null;
            return t;
        }
    }

    private void setType(Map<Var, Type> typing, Var v, Type t) {
        typing.put(v, t);
        setTypeForTemp(v, t);
    }

    public void newTypeAssign(Var var, Type t, Map<Var, Type> typing) {
        setType(typing, var, t);
    }

    public void newTypeAssign(Var var, List<Var> rValues, Map<Var, Type> typing) {
        List<Type> types = rValues
                .stream()
                .map(v -> getType(typing, v))
                .distinct()
                .toList();
        assert types.size() == 1 ||
                types.stream().allMatch(this::canHoldsInt);
        Type resultType = types.get(0);
        setType(typing, var, resultType);
    }

    public void newTypeArrayLoad(Var target, ArrayAccess array, Map<Var, Type> typing) {
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
        insertCasting();
    }

    private void buildLocalTypes() {
        for (BytecodeBlock block : builder.blockSortedList) {
            if (block.getFrame() != null) {
                block.getInitTyping().forEach((k, v) -> {
                    localTypes.put(k, v);
                });
            }
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

        for (Var ret : this.builder.manager.getRetVars()) {
            if (ret.getType() != null) {
                assert isAssignable(m.getReturnType(), ret.getType());
            } else {
                ret.setType(m.getReturnType());
            }
        }
    }

    public void inferTypes() {
        visited = Sets.newHybridSet();
        for (BytecodeBlock block : builder.blockSortedList) {
            Map<Var, Type> initTyping;
            if (! visited.contains(block)) {
                if (block.inEdges().size() == 0 && ! block.isCatch()) {
                    initTyping = Maps.newMap();
                } else {
                    initTyping = block.getInitTyping();
                }
                inferTypesForBlock(block, initTyping);
            }
        }
    }

    public void inferTypesForBlock(BytecodeBlock block, Map<Var, Type> typing) {
        visited.add(block);
        for (Stmt stmt : builder.getStmts(block)) {
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
                    return StmtVisitor.super.visit(stmt);
                }

                @Override
                public Void visit(LoadField stmt) {
                    newTypeAssign(stmt.getLValue(), stmt.getRValue().getType(), typing);
                    return StmtVisitor.super.visit(stmt);
                }

                @Override
                public Void visit(StoreField stmt) {
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

        if (block.fallThrough() != null) {
            inferTypesForBlock(block.fallThrough(), typing);
        }
    }

    public void insertCasting() {
    }

    public Map<Var, Type> getInitTyping(BytecodeBlock block) {
        return block.getInitTyping();
    }

}
