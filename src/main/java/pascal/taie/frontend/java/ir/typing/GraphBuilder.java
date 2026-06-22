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

import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.frontend.java.ir.ssa.FrontendPhiStmt;
import pascal.taie.ir.exp.ArrayLengthExp;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Binary;
import pascal.taie.ir.stmt.Cast;
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
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;

import java.util.List;

import static pascal.taie.language.type.BooleanType.BOOLEAN;
import static pascal.taie.language.type.IntType.INT;

/**
 * Visits IR statements to construct edges, types and constraints in the type flow graph.
 */
final class GraphBuilder implements StmtVisitor<Void> {
    private final FrontendTypeSystem typeSystem;
    private final TypeFlowGraph graph;
    private final JMethod method;

    public GraphBuilder(FrontendTypeSystem typeSystem, TypeFlowGraph graph, JMethod method) {
        this.typeSystem = typeSystem;
        this.graph = graph;
        this.method = method;
    }

    @Override
    public Void visit(New stmt) {
        graph.addType(stmt.getLValue(), stmt.getRValue().getType());
        return null;
    }

    @Override
    public Void visit(AssignLiteral stmt) {
        Type t;
        if (stmt.getRValue() instanceof StringLiteral) {
            t = typeSystem.stringType();
        } else {
            t = stmt.getRValue().getType();
        }
        graph.addType(stmt.getLValue(), t);
        return null;
    }

    @Override
    public Void visit(Copy stmt) {
        graph.addVarTypeFlow(stmt.getRValue(), stmt.getLValue(), FlowKind.VAR_VAR);
        return null;
    }

    @Override
    public Void visit(LoadArray stmt) {
        graph.addVarTypeFlow(stmt.getRValue().getBase(), stmt.getLValue(), FlowKind.ARRAY_VAR);
        return null;
    }

    @Override
    public Void visit(StoreArray stmt) {
        Type rType = stmt.getRValue().getType();
        if (rType instanceof PrimitiveType) {
            // skips primitive type
            // example for that:
            // 1. a = new byte[10]
            // 2. a[1] = 1
            // the right `IntLiteral(1)` MUST NOT infer `a` to be `int[]`
            // i.e. we only trust the line 1., not line 2.
            // and there must be something like line 1.,
            // or the classfile will not pass the verification
            return null;
        }
        graph.addVarTypeFlow(stmt.getRValue(), stmt.getLValue().getBase(), FlowKind.VAR_ARRAY);
        return null;
    }

    @Override
    public Void visit(LoadField stmt) {
        graph.addType(stmt.getLValue(), stmt.getRValue().getType());
        if (stmt.getRValue() instanceof InstanceFieldAccess instanceFieldAccess) {
            graph.addUseConstraint(instanceFieldAccess.getBase(),
                    instanceFieldAccess.getFieldRef().getDeclaringClass().getType());
        }
        return null;
    }

    @Override
    public Void visit(StoreField stmt) {
        if (stmt.getLValue().getType() instanceof ReferenceType r) {
            graph.addUseConstraint(stmt.getRValue(), r);
        }
        return null;
    }

    @Override
    public Void visit(Binary stmt) {
        Type t = stmt.getRValue().getType();
        if (t != null) {
            graph.addType(stmt.getLValue(), t);
        } else {
            graph.addVarTypeFlow(stmt.getRValue().getOperand1(), stmt.getLValue(), FlowKind.VAR_VAR);
        }
        return null;
    }

    @Override
    public Void visit(Unary stmt) {
        if (stmt.getRValue() instanceof ArrayLengthExp) {
            graph.addType(stmt.getLValue(), INT);
        } else {
            graph.addVarTypeFlow(stmt.getRValue().getOperand(), stmt.getLValue(), FlowKind.VAR_VAR);
        }
        return null;
    }

    @Override
    public Void visit(InstanceOf stmt) {
        graph.addType(stmt.getLValue(), BOOLEAN);
        return null;
    }

    @Override
    public Void visit(Cast stmt) {
        graph.addType(stmt.getLValue(), stmt.getRValue().getType());
        return null;
    }

    @Override
    public Void visit(Invoke stmt) {
        Var lValue = stmt.getLValue();
        InvokeExp rValue = stmt.getRValue();
        if (lValue != null) {
            graph.addType(lValue, rValue.getType());
        }

        if (rValue instanceof InvokeInstanceExp invokeInstanceExp) {
            Var base = invokeInstanceExp.getBase();
            ReferenceType decl = invokeInstanceExp.getMethodRef().getDeclaringClass().getType();
            if (!stmt.getMethodRef().getName().equals(MethodNames.INIT)) {
                graph.addUseConstraint(base, decl);
            } else {
                graph.addInitConstraint(base, decl);
            }
        }

        if (rValue instanceof InvokeDynamic) {
            return null;
        }
        List<Type> paraTypes = rValue.getMethodRef().getParameterTypes();
        List<Var> args = rValue.getArgs();
        for (int i = 0; i < args.size(); ++i) {
            Type paraType = paraTypes.get(i);
            Var arg = args.get(i);
            if (paraType instanceof ReferenceType r) {
                graph.addUseConstraint(arg, r);
            }
        }
        return null;
    }

    @Override
    public Void visit(Return stmt) {
        Type retType = method.getReturnType();
        if (retType instanceof ReferenceType r) {
            assert stmt.getValue() != null;
            graph.addUseConstraint(stmt.getValue(), r);
        }
        return null;
    }

    @Override
    public Void visitDefault(Stmt stmt) {
        if (stmt instanceof FrontendPhiStmt phi) {
            Var lValue = phi.getLValue();
            for (RValue v : phi.getRValue().getUses()) {
                graph.addVarTypeFlow((Var) v, lValue, FlowKind.VAR_VAR);
            }
        }
        return null;
    }
}
