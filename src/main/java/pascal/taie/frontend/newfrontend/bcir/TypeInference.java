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
import pascal.taie.frontend.newfrontend.Utils;
import pascal.taie.frontend.newfrontend.main.IRBuildingPhase;
import pascal.taie.frontend.newfrontend.main.NewFrontendIRComponent;
import pascal.taie.frontend.newfrontend.ssa.FrontendPhiStmt;
import pascal.taie.ir.exp.ArrayLengthExp;
import pascal.taie.ir.exp.ExpMutator;
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

import static pascal.taie.frontend.newfrontend.Utils.isIntAssignable;
import static pascal.taie.frontend.newfrontend.Utils.isPrimitiveArrayType;
import static pascal.taie.frontend.newfrontend.Utils.isAssignable;
import static pascal.taie.language.type.BooleanType.BOOLEAN;
import static pascal.taie.language.type.IntType.INT;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

// TODO: moving to newfrontend.typing package

/**
 * <p>Type inference based on the constraint graph.</p>
 *
 * <p>The algorithm is based on (1), but contains non-trivial improvement</p>
 * <p>(1) Ben Bellamy, Pavel Avgustinov, Oege de Moor, and Damien Sereni. 2008. Efficient local type inference.
 * SIGPLAN Not. 43, 10 (September 2008), 475–492. <a href="https://doi.org/10.1145/1449955.1449802">link</a>
 */
public class TypeInference extends NewFrontendIRComponent {

    final BytecodeIRBuilder builder;

    final TypingFlowGraph graph;

    private boolean needCasting;

    public TypeInference(BytecodeIRBuilder builder, FrontendContext context) {
        super(context, IRBuildingPhase.BYTECODE_TYPE_INFERENCE);
        this.builder = builder;
        graph = new TypingFlowGraph(builder.manager.getVars().size());
        this.needCasting = false;
    }

    public Set<ReferenceType> lca(ReferenceType r1, ReferenceType r2) {
        return Utils.lca(tCtx(), r1, r2);
    }

    public Optional<Type> plusOneArray(Type t) {
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
        return Optional.of(typeSystem().getArrayType(baseType, dim));
    }

    public static Optional<Type> subOneArray(Type t) {
        if (t instanceof ArrayType at) {
            return Optional.of(at.elementType());
        } else {
            return Optional.empty();
        }
    }

    class ConstraintVisitor implements FrontendStmtVisitor<Void> {
        @Override
        public Void visit(New stmt) {
            graph.addConstantEdge(stmt.getRValue().getType(), stmt.getLValue());
            return null;
        }

        @Override
        public Void visit(AssignLiteral stmt) {
            Type t;
            if (stmt.getRValue() instanceof StringLiteral) {
                t = tCtx().string();
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
            Type retType = builder.method.getReturnType();
            if (retType instanceof ReferenceType r) {
                assert stmt.getValue() != null;
                graph.addUseConstrain(stmt.getValue(), r);
            }
            return null;
        }

        @Override
        public Void visit(FrontendPhiStmt stmt) {
            Var lValue = stmt.getLValue();
            for (RValue v : stmt.getRValue().getUses()) {
                graph.addVarEdge((Var) v, lValue, EdgeKind.VAR_VAR);
            }
            return null;
        }
    }

    public void build() {
        addThisParam();
        ConstraintVisitor visitor = new ConstraintVisitor();
        for (BytecodeBlock block : builder.blockSortedList) {
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
        setTypes(graph);
        if (needCasting) {
            CastingInsert insert = new CastingInsert(builder, ctx());
            insert.build();
        }
    }

    private void setTypes(TypingFlowGraph graph) {
        for (TypingFlowNode node : graph.nodes) {
            if (node == null) {
                continue;
            }
            Var v = node.var;
            if (v.getType() != null) {
                continue;
            }
            if (node.primitiveType != null) {
                ExpMutator.setType(v, node.primitiveType);
            } else {
                ReferenceType target = node.referenceType;
                if (target == null) {
                    target = NullType.NULL;
                }
                if (!node.isUseValid(target)) {
                    this.needCasting = true;
                }
                if (node.initConstraints != null) {
                    for (Type t : node.initConstraints) {
                        if (!isAssignable(tCtx(), t, target)) {
                            this.needCasting = true;
                        }
                    }
                }
                ExpMutator.setType(v, target);
            }
        }
    }

    private void addThisParam() {
        JMethod m = builder.method;
        if (! builder.method.isStatic()) {
            Var thisVar = this.builder.manager.getThisVar();
            graph.addConstantEdge(m.getDeclaringClass().getType(), thisVar);
        }

        for (int i = 0; i < m.getParamCount(); ++i) {
            Var paramI = builder.manager.getParams().get(i);
            Type typeI = m.getParamType(i);
            graph.addConstantEdge(typeI, paramI);
        }
    }

    class TypingFlowGraph {
        private final TypingFlowNode[] nodes;

        TypingFlowGraph(int varSize) {
            this.nodes = new TypingFlowNode[varSize];
        }

        private TypingFlowNode getNode(Var var) {
            if (nodes[var.getIndex()] == null) {
                nodes[var.getIndex()] = new TypingFlowNode(var);
            }
            return nodes[var.getIndex()];
        }

        public void addConstantEdge(Type t, Var v) {
            assert v != null;
            TypingFlowNode node = getNode(v);
            node.setNewType(t);
        }

        public void addVarEdge(Var from, Var to, EdgeKind kind) {
            if (from.getType() != null && kind != EdgeKind.VAR_ARRAY) {
                computeFlowOutType(from.getType(), kind).ifPresent((t) -> {
                    addConstantEdge(t, to);
                });
            } else {
                TypingFlowNode n1 = getNode(from);
                TypingFlowNode n2 = getNode(to);
                TypingFlowEdge edge = new TypingFlowEdge(kind, n1, n2);
                n2.addNewInEdge(edge);
                n1.addNewOutEdge(edge);
            }
        }

        public void addUseConstrain(Var v, ReferenceType constrain) {
            TypingFlowNode node = getNode(v);
            node.addNewUseConstrain(constrain);
        }

        public void inferTypes() {
            inferUseConstrains();

            Queue<FlowType> queue = new LinkedList<>();
            for (TypingFlowNode node : nodes) {
                if (node == null) {
                    continue;
                }
                if (node.primitiveType != null) {
                    PrimitiveType t = node.primitiveType;
                    flowOutType(queue, node, t);
                } else if (node.types != null) {
                    node.firstResolve();
                    flowOutType(queue, node, node.referenceType);
                }
            }

            while (!queue.isEmpty()) {
                FlowType now = queue.poll();
                spreadingFlowType(queue, now);
            }
        }

        public void inferUseConstrains() {
            Queue<FlowType> queue = new LinkedList<>();
            for (TypingFlowNode node : nodes) {
                if (node == null) {
                    continue;
                }
                if (node.useValidConstrains != null) {
                    for (ReferenceType t : node.useValidConstrains) {
                        flowOutConstrains(queue, node, t);
                    }
                }
            }

            while (!queue.isEmpty()) {
                FlowType now = queue.poll();
                spreadingUseConstrains(queue, now);
            }
        }

        private void spreadingUseConstrains(Queue<FlowType> queue, FlowType type) {
            TypingFlowNode now = type.edge.source;
            Optional<Type> optionalType = type.getSourceType();
            if (optionalType.isEmpty()) {
                return;
            }
            Type t = optionalType.get();
            if (t instanceof ReferenceType t1) {
                if (now.addNewUseConstrain(t1)) {
                    flowOutConstrains(queue, now, t1);
                }
            }
        }

        private void spreadingFlowType(Queue<FlowType> queue, FlowType type) {
            if (type.edge.kind == EdgeKind.VAR_ARRAY) {
                return;
            }
            TypingFlowNode now = type.edge.target;
            Optional<Type> optionalType = type.getTargetType();
            if (optionalType.isEmpty()) {
                return;
            }
            Type t = optionalType.get();
            if (t instanceof PrimitiveType pType) {
                if (now.onNewPrimitiveType(pType)) {
                    flowOutType(queue, now, pType);
                }
            } else if (t instanceof ReferenceType rType) {
                boolean needSpread = now.onNewReferenceType(type.edge.kind, rType);
                if (needSpread) {
                    flowOutType(queue, now, now.referenceType);
                }
            } else {
                throw new UnsupportedOperationException();
            }
        }

        private void flowOutType(Queue<FlowType> queue, TypingFlowNode node, PrimitiveType pType) {
            if (node.outEdges == null) {
                return;
            }
            for (TypingFlowEdge e : node.outEdges) {
                queue.add(new FlowType(e, pType));
            }
        }

        private void flowOutType(Queue<FlowType> queue, TypingFlowNode node, ReferenceType type) {
            if (node.outEdges == null) {
                return;
            }
            for (TypingFlowEdge e : node.outEdges) {
                queue.add(new FlowType(e, type));
            }
        }

        private void flowOutConstrains(Queue<FlowType> queue, TypingFlowNode node, ReferenceType type) {
            if (node.inEdges == null) {
                return;
            }
            for (TypingFlowEdge e : node.inEdges) {
                queue.add(new FlowType(e, type));
            }
        }
    }

    final class FlowType {
        private final TypingFlowEdge edge;
        private final Type type;

        FlowType(TypingFlowEdge edge, Type type) {
            this.edge = edge;
            this.type = type;
        }

        Optional<Type> getTargetType() {
            return switch (edge.kind) {
                case VAR_VAR -> Optional.of(type);
                case VAR_ARRAY -> plusOneArray(type);
                case ARRAY_VAR -> subOneArray(type);
            };
        }

        Optional<Type> getSourceType() {
            return switch (edge.kind) {
                case VAR_VAR -> Optional.of(type);
                case VAR_ARRAY -> subOneArray(type);
                case ARRAY_VAR -> plusOneArray(type);
            };
        }

        public Type type() {
            return type;
        }
    }

    private Optional<Type> computeFlowOutType(Type t, EdgeKind kind) {
        return switch (kind) {
            case VAR_VAR -> Optional.of(t);
            case VAR_ARRAY -> plusOneArray(t);
            case ARRAY_VAR -> subOneArray(t);
        };
    }

    final class TypingFlowNode {
        private final Var var;
        @Nullable
        private Set<ReferenceType> types;
        @Nullable
        private PrimitiveType primitiveType;

        @Nullable
        private ReferenceType referenceType;

        private Set<ReferenceType> useValidConstrains;

        private List<ReferenceType> initConstraints;

        private List<TypingFlowEdge> inEdges;

        private List<TypingFlowEdge> outEdges;

        TypingFlowNode(Var var) {
            this.var = var;
            useValidConstrains = null;
            inEdges = null;
            outEdges = null;
        }

        public void setNewType(Type t) {
            if (t instanceof PrimitiveType pType) {
                onNewPrimitiveType(pType);
            } else if (t instanceof ReferenceType rType) {
                addReferenceType(rType);
            } else {
                throw new UnsupportedOperationException();
            }
        }

        /**
         * @param t new incoming primitive type
         * @return if type changes
         */
        public boolean onNewPrimitiveType(PrimitiveType t) {
            // TODO: perform numeric promotion
            assert types == null;
            assert t != null;
            if (this.primitiveType == null) {
                this.primitiveType = t;
                return true;
            } else {
                if (this.primitiveType == t) {
                    return false;
                } else if (isIntAssignable(primitiveType, t)) {
                    return false;
                } else {
                    throw new UnsupportedOperationException();
                }
            }
        }

        public void addReferenceType(ReferenceType t) {
            assert primitiveType == null;
            if (types == null) {
                types = Sets.newHybridSet();
            }
            types.add(t);
        }

        public boolean onNewReferenceType(EdgeKind kind, ReferenceType t) {
            if (kind == EdgeKind.VAR_ARRAY && isPrimitiveArrayType(t)) {
                // example for that:
                // 1. a = new int[10]
                // 2. a[1] = 1
                // the right `ByteLiteral(1)` MUST NOT infer `a` to be `byte[]`
                // i.e. we only trust the line 1., not line 2.
                // and there must be something like line 1.,
                // or the classfile will not pass the verification
                return false;
            }

            if (referenceType == null) {
                referenceType = t;
                return true;
            } else {
                ReferenceType temp = referenceType;
                referenceType = getNextType(referenceType, t);
                // TODO: ==?
                return temp != referenceType;
            }
        }

        public void firstResolve() {
            assert types != null;
            for (ReferenceType r : types) {
                onNewReferenceType(null, r);
            }
        }

        private boolean isUseValid(ReferenceType t) {
            if (useValidConstrains == null) {
                return true;
            }
            boolean ret = true;
            for (ReferenceType c : useValidConstrains) {
                ret &= Utils.isAssignable(tCtx(), c, t);
            }
            return ret;
        }

        private ReferenceType getNextType(ReferenceType current, ReferenceType t) {
            if (current == t) {
                return t;
            }
            Set<ReferenceType> lcas = lca(current, t);
            ReferenceType target = null;
            for (ReferenceType lca : lcas) {
                if (isUseValid(lca)) {
                    target = lca;
                    break;
                }
            }
            if (target != null) {
                // any type in types is use valid
                return target;
            } else {
                if (lcas.isEmpty()) {
                    // normally impossible, but possible for phantom
                    return tCtx().object();
                }
                ReferenceType t1 = lcas.iterator().next();
                if (t1 instanceof ClassType) {
                    return tCtx().object();
                } else if (t1 instanceof ArrayType arrayType) {
                    return typeSystem().getArrayType(tCtx().object(), arrayType.dimensions());
                } else {
                    throw new UnsupportedOperationException();
                }
            }
        }

        public void addNewOutEdge(TypingFlowEdge edge) {
            if (outEdges == null) {
                outEdges = new ArrayList<>();
            }
            outEdges.add(edge);
        }

        public void addNewInEdge(TypingFlowEdge edge) {
            if (inEdges == null) {
                inEdges = new ArrayList<>();
            }
            inEdges.add(edge);
        }

        public boolean addNewUseConstrain(ReferenceType type) {
            if (useValidConstrains == null) {
                useValidConstrains = Sets.newHybridSet();
            }
            return useValidConstrains.add(type);
        }

        public void addInitConstraint(ReferenceType type) {
            if (initConstraints == null) {
                initConstraints = new ArrayList<>();
            }
            initConstraints.add(type);
        }

        public Var var() {
            return var;
        }
    }

    record TypingFlowEdge(
            EdgeKind kind,
            TypingFlowNode source,
            TypingFlowNode target) {
    }

    enum EdgeKind {
        // v1 <- v2
        VAR_VAR,

        // v1[i] <- v2
        VAR_ARRAY,

        // v2 <- v1[i]
        ARRAY_VAR

    }
}
