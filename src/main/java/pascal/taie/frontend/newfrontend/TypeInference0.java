package pascal.taie.frontend.newfrontend;

import pascal.taie.ir.exp.ArrayAccess;
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
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TypeInference0 {

    AsmIRBuilder builder;
    MultiMap<Var, Type> localTypes;

    public TypeInference0(AsmIRBuilder builder) {
        this.builder = builder;
        localTypes = Maps.newMultiMap();
        inferTypes();
        setTypeForLocal();
        insertCasting();
    }

    private void setTypeForTemp(Var var, Type t) {
        if (! localTypes.containsKey(var)) {
            var.setType(t);
        }
    }

    private void setTypeForLocal() {
        for (Var v : localTypes.keySet()) {
            Set<Type> allTypes = localTypes.get(v);
            assert allTypes.size() > 0;
            Type now = allTypes.iterator().next();
            for (Type t : allTypes) {
                if (now instanceof PrimitiveType) {
                    assert t == now;
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

    public void newTypeAssign(Var var, Type t, Map<Var, Type> typing) {
        typing.put(var, t);
        setTypeForTemp(var, t);
    }

    public void newTypeAssign(Var var, List<Var> rValues, Map<Var, Type> typing) {
        Set<Type> types = rValues
                .stream()
                .map(typing::get)
                .collect(Collectors.toSet());
        assert types.size() == 1;
        Type resultType = types.stream().findAny().get();
        typing.put(var, resultType);
        setTypeForTemp(var, resultType);
    }

    public void newTypeArrayLoad(Var target, ArrayAccess array, Map<Var, Type> typing) {
        Var base = array.getBase();
        Type t = typing.get(base);
        if (t instanceof ArrayType arrayType) {
            typing.put(target, arrayType);
            setTypeForTemp(target, t);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public void inferTypes() {
        for (BytecodeBlock block : builder.label2Block.values()) {
            Map<Var, Type> typing = getInitTyping(block);

            for (Stmt stmt : builder.getStmts(block)) {
                stmt.accept(new StmtVisitor<Void> () {
                    @Override
                    public Void visit(New stmt) {
                        newTypeAssign(stmt.getLValue(), stmt.getRValue().getType(), typing);
                        return StmtVisitor.super.visit(stmt);
                    }

                    @Override
                    public Void visit(AssignLiteral stmt) {
                        newTypeAssign(stmt.getLValue(), stmt.getRValue().getType(), typing);
                        return StmtVisitor.super.visit(stmt);
                    }

                    @Override
                    public Void visit(Copy stmt) {
                        newTypeAssign(stmt.getLValue(), List.of(stmt.getLValue()), typing);
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
                        newTypeAssign(stmt.getLValue(), List.of(stmt.getRValue().getOperand1(),
                                stmt.getRValue().getOperand2()), typing);
                        return StmtVisitor.super.visit(stmt);
                    }

                    @Override
                    public Void visit(Unary stmt) {
                        newTypeAssign(stmt.getLValue(), List.of(stmt.getRValue().getOperand()), typing);
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
                        // TODO: set exception type here
                        return StmtVisitor.super.visit(stmt);
                    }
                });
            }
        }
    }

    public void insertCasting() {

    }

    public Map<Var, Type> getInitTyping(BytecodeBlock block) {
        return null;
    }

}
