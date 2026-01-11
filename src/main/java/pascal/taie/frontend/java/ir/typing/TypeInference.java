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
import pascal.taie.frontend.java.ir.BytecodeBlock;
import pascal.taie.frontend.java.ir.IRBuilderContext;
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
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;

import java.util.List;
import java.util.Optional;


import static pascal.taie.language.type.BooleanType.BOOLEAN;
import static pascal.taie.language.type.IntType.INT;

/**
 * Pruning-based type inference based on the constraint graph.
 */
public class TypeInference {

    final IRBuilderContext context;

    final TypeFlowGraph graph;

    private boolean needCasting;

    public TypeInference(IRBuilderContext context) {
        this.context = context;
        this.needCasting = false;
        this.graph = new TypeFlowGraph(context.typeSystem, context.varManager.getAllVars().size());
    }

    static Optional<Type> plusOneArray(Type t, FrontendTypeSystem typeSystem) {
        if (t instanceof NullType) {
            return Optional.empty();
        }
        Type baseType;
        int dim;
        if (t instanceof ArrayType at) {
            baseType = at.baseType();
            dim = at.dimensions() + 1;
        } else {
            baseType = t;
            dim = 1;
        }
        return Optional.of(typeSystem.getArrayType(baseType, dim));
    }

    static Optional<Type> subOneArray(Type t) {
        if (t instanceof ArrayType at) {
            return Optional.of(at.elementType());
        } else {
            return Optional.empty();
        }
    }

    class ConstraintVisitor implements StmtVisitor<Void> {
        @Override
        public Void visit(New stmt) {
            graph.addConstantEdge(stmt.getRValue().getType(), stmt.getLValue());
            return null;
        }

        @Override
        public Void visit(AssignLiteral stmt) {
            Type t;
            if (stmt.getRValue() instanceof StringLiteral) {
                t = context.typeSystem.stringType();
            } else {
                t = stmt.getRValue().getType();
            }
            graph.addConstantEdge(t, stmt.getLValue());
            return null;
        }

        @Override
        public Void visit(Copy stmt) {
            graph.addVarEdge(stmt.getRValue(), stmt.getLValue(), EdgeKind.VAR_VAR);
            return null;
        }

        @Override
        public Void visit(LoadArray stmt) {
            graph.addVarEdge(stmt.getRValue().getBase(), stmt.getLValue(), EdgeKind.ARRAY_VAR);
            return null;
        }

        @Override
        public Void visit(StoreArray stmt) {
            Type rType = stmt.getRValue().getType();
            // skips primitive type
            if (rType instanceof PrimitiveType) {
                return null;
            }
            graph.addVarEdge(stmt.getRValue(), stmt.getLValue().getBase(), EdgeKind.VAR_ARRAY);
            return null;
        }

        @Override
        public Void visit(LoadField stmt) {
            graph.addConstantEdge(stmt.getRValue().getType(), stmt.getLValue());
            if (stmt.getRValue() instanceof InstanceFieldAccess instanceFieldAccess) {
                // TODO: maybe resolve() or can just setType() ?
                graph.addUseConstrain(instanceFieldAccess.getBase(),
                        instanceFieldAccess.getFieldRef().getDeclaringClass().getType());
            }
            return null;
        }

        @Override
        public Void visit(StoreField stmt) {
            if (stmt.getLValue().getType() instanceof ReferenceType r) {
                graph.addUseConstrain(stmt.getRValue(), r);
            }
            return null;
        }

        @Override
        public Void visit(Binary stmt) {
            Type t = stmt.getRValue().getType();
            if (t != null) {
                graph.addConstantEdge(t, stmt.getLValue());
            } else {
                graph.addVarEdge(stmt.getRValue().getOperand1(), stmt.getLValue(), EdgeKind.VAR_VAR);
            }
            return null;
        }

        @Override
        public Void visit(Unary stmt) {
            if (stmt.getRValue() instanceof ArrayLengthExp) {
                graph.addConstantEdge(INT, stmt.getLValue());
            } else {
                graph.addVarEdge(stmt.getRValue().getOperand(), stmt.getLValue(), EdgeKind.VAR_VAR);
            }
            return null;
        }

        @Override
        public Void visit(InstanceOf stmt) {
            graph.addConstantEdge(BOOLEAN, stmt.getLValue());
            return null;
        }

        @Override
        public Void visit(Cast stmt) {
            graph.addConstantEdge(stmt.getRValue().getType(), stmt.getLValue());
            return null;
        }

        @Override
        public Void visit(Invoke stmt) {
            Var lValue = stmt.getLValue();
            InvokeExp rValue = stmt.getRValue();
            if (lValue != null) {
                graph.addConstantEdge(rValue.getType(), lValue);
            }

            if (rValue instanceof InvokeInstanceExp invokeInstanceExp) {
                Var base = invokeInstanceExp.getBase();
                ReferenceType decl = invokeInstanceExp.getMethodRef().getDeclaringClass().getType();
                if (!stmt.getMethodRef().getName().equals(MethodNames.INIT)) {
                    graph.addUseConstrain(base, decl);
                } else {
                    graph.getNode(base).addInitConstraint(decl);
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
                    graph.addUseConstrain(arg, r);
                }
            }
            return null;
        }

        @Override
        public Void visit(Return stmt) {
            Type retType = context.method.getReturnType();
            if (retType instanceof ReferenceType r) {
                assert stmt.getValue() != null;
                graph.addUseConstrain(stmt.getValue(), r);
            }
            return null;
        }

        @Override
        public Void visitDefault(Stmt stmt) {
            if (stmt instanceof FrontendPhiStmt phi) {
                Var lValue = phi.getLValue();
                for (RValue v : phi.getRValue().getUses()) {
                    graph.addVarEdge((Var) v, lValue, EdgeKind.VAR_VAR);
                }
            }
            return null;
        }
    }

    public void build() {
        addThisParam();
        ConstraintVisitor visitor = new ConstraintVisitor();
        for (BytecodeBlock block : context.cfg) {
            if (block.getExceptionHandlerTypes() != null) {
                Var ref = null;
                for (Stmt stmt : block.getStmts()) {
                    if (stmt instanceof Catch catchStmt) {
                        ref = catchStmt.getExceptionRef();
                        break;
                    }
                }
                if (ref != null) {
                    for (ReferenceType t : block.getExceptionHandlerTypes()) {
                        graph.addConstantEdge(t, ref);
                    }
                }
            }
            for (Stmt stmt : block.getStmts()) {
                stmt.accept(visitor);
            }
        }

        graph.inferTypes();
        needCasting = graph.setTypes();
        if (needCasting) {
            CastingInserter inserter = new CastingInserter(context);
            inserter.build();
        }
    }

    private void addThisParam() {
        JMethod m = context.method;
        if (!context.method.isStatic()) {
            Var thisVar = this.context.varManager.getThisVar();
            graph.addConstantEdge(m.getDeclaringClass().getType(), thisVar);
        }

        for (int i = 0; i < m.getParamCount(); ++i) {
            Var paramI = context.varManager.getParams().get(i);
            Type typeI = m.getParamType(i);
            graph.addConstantEdge(typeI, paramI);
        }
    }
}
