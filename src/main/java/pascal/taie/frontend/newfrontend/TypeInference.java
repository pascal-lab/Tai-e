package pascal.taie.frontend.newfrontend;

import pascal.taie.frontend.newfrontend.ssa.PhiStmt;
import pascal.taie.ir.exp.ArrayLengthExp;
import pascal.taie.ir.exp.ExpModifier;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.RValue;
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
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import static pascal.taie.frontend.newfrontend.Utils.*;
import static pascal.taie.language.type.BooleanType.BOOLEAN;
import static pascal.taie.language.type.IntType.INT;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

// TODO: moving to newfrontend.typing package
public class TypeInference {

    final AsmIRBuilder builder;

    final TypingFlowGraph graph;

    public TypeInference(AsmIRBuilder builder) {
        this.builder = builder;
        graph = new TypingFlowGraph();
    }

    public static Set<ReferenceType> lca(ReferenceType r1, ReferenceType r2) {
        return Utils.lca(r1, r2);
    }

    public static Optional<Type> plusOneArray(Type t) {
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
        return Optional.of(BuildContext.get().getTypeSystem().getArrayType(baseType, dim));
    }

    public static Optional<Type> subOneArray(Type t) {
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
            graph.addConstantEdge(stmt.getRValue().getType(), stmt.getLValue());
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
            return StmtVisitor.super.visit(stmt);
        }

        @Override
        public Void visit(Binary stmt) {
            graph.addVarEdge(stmt.getRValue().getOperand1(), stmt.getLValue(), EdgeKind.VAR_VAR);
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
                if (!stmt.getMethodRef().getName().equals(MethodNames.INIT)) {
                    graph.addUseConstrain(invokeInstanceExp.getBase(),
                            invokeInstanceExp.getMethodRef().getDeclaringClass().getType());
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
            return StmtVisitor.super.visit(stmt);
        }

        @Override
        public Void visit(PhiStmt stmt) {
            Var lValue = stmt.getLValue();
            for (RValue v : stmt.getRValue().getUses()) {
                graph.addVarEdge((Var) v, lValue, EdgeKind.VAR_VAR);
            }
            return StmtVisitor.super.visit(stmt);
        }
    }

    public void build() {

        addThisParam();
        ConstraintVisitor visitor = new ConstraintVisitor();
        for (BytecodeBlock block : builder.blockSortedList) {
            if (block.getExceptionHandlerType() != null) {
                for (Stmt stmt : block.getStmts()) {
                    if (stmt instanceof Catch catchStmt) {
                        graph.addConstantEdge(block.getExceptionHandlerType(),
                                catchStmt.getExceptionRef());
                        break;
                    }
                }
            }
            for (Stmt stmt : block.getStmts()) {
                stmt.accept(visitor);
            }
        }

        graph.inferTypes();
        setTypes(graph);
        CastingInsert insert = new CastingInsert(builder);
        insert.build();
    }

    private void setTypes(TypingFlowGraph graph) {
        graph.nodes.forEach((k, v) -> {
            if (k.getType() != null) {
                return;
            }
            if (v.primitiveType != null) {
                ExpModifier.setType(k, v.primitiveType);
            } else {
                if (v.referenceType != null) {
                    ExpModifier.setType(k, v.referenceType);
                } else {
                    // TODO: add warning here
                    ExpModifier.setType(k, NullType.NULL);
                }
            }
        });
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
        Map<Var, TypingFlowNode> nodes = Maps.newHybridMap();

        public void addConstantEdge(Type t, Var v) {
            assert v != null;
            if (v.getType() != null) {
                return;
            } else if (builder.varSSAInfo.isSSAVar(v)) {
                ExpModifier.setType(v, t);
                return;
            }
            TypingFlowNode node = nodes.computeIfAbsent(v, TypingFlowNode::new);
            node.setNewType(t);
        }

        public void addVarEdge(Var from, Var to, EdgeKind kind) {
            if (to.getType() != null && from.getType() != null) {
                return;
            }
            if (from.getType() != null) {
                computeFlowOutType(from.getType(), kind).ifPresent((t) -> {
                    addConstantEdge(t, to);
                });
            } else {
                TypingFlowNode n1 = nodes.computeIfAbsent(from, TypingFlowNode::new);
                TypingFlowNode n2 = nodes.computeIfAbsent(to, TypingFlowNode::new);
                TypingFlowEdge edge = new TypingFlowEdge(kind, n1, n2);
                n2.addNewInEdge(edge);
                n1.addNewOutEdge(edge);
            }
        }

        public void addUseConstrain(Var v, ReferenceType constrain) {
            if (v.getType() != null) {
                return;
            }
            TypingFlowNode node = nodes.computeIfAbsent(v, TypingFlowNode::new);
            node.addNewUseConstrain(constrain);
        }

        public void inferTypes() {
            inferUseConstrains();

            Queue<FlowType> queue = new LinkedList<>();
            for (TypingFlowNode node : nodes.values()) {
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
            for (TypingFlowNode node : nodes.values()) {
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
                if (now.useValidConstrains.add(t1)) {
                    flowOutConstrains(queue, now, t1);
                }
            }
        }

        private void spreadingFlowType(Queue<FlowType> queue, FlowType type) {
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
            for (TypingFlowEdge e : node.outEdges) {
                queue.add(new FlowType(e, pType));
            }
        }

        private void flowOutType(Queue<FlowType> queue, TypingFlowNode node, ReferenceType type) {
            for (TypingFlowEdge e : node.outEdges) {
                queue.add(new FlowType(e, type));
            }
        }

        private void flowOutConstrains(Queue<FlowType> queue, TypingFlowNode node, ReferenceType type) {
            for (TypingFlowEdge e : node.inEdges) {
                queue.add(new FlowType(e, type));
            }
        }
    }

    record FlowType(TypingFlowEdge edge, Type type) {
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
    }

    private static Optional<Type> computeFlowOutType(Type t, EdgeKind kind) {
        return switch (kind) {
            case VAR_VAR -> Optional.of(t);
            case VAR_ARRAY -> plusOneArray(t);
            case ARRAY_VAR -> subOneArray(t);
        };
    }

    static final class TypingFlowNode {
        private final Var var;
        @Nullable
        private Set<ReferenceType> types;
        @Nullable
        private PrimitiveType primitiveType;

        @Nullable
        private ReferenceType referenceType;

        private Set<ReferenceType> useValidConstrains;

        private List<TypingFlowEdge> inEdges;

        private List<TypingFlowEdge> outEdges;

        TypingFlowNode(Var var) {
            this.var = var;
            useValidConstrains = Sets.newHybridSet();
            inEdges = List.of();
            outEdges = List.of();
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
            if (isPrimitiveArrayType(t) && kind == EdgeKind.VAR_ARRAY) {
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
            return useValidConstrains.stream()
                    .allMatch(c -> Utils.isAssignable(c, t));
        }

        private ReferenceType getNextType(ReferenceType current, ReferenceType t) {
            if (current == t) {
                return t;
            }
            Set<ReferenceType> newType = lca(current, t);
            List<ReferenceType> types = newType.stream().filter(this::isUseValid).toList();
            if (!types.isEmpty()) {
                // any type in types is use valid
                return types.get(0);
            } else {
                if (newType.isEmpty()) {
                    // normally impossible, but possible for phantom
                    return Utils.getObject();
                }
                ReferenceType t1 = newType.iterator().next();
                if (t1 instanceof ClassType) {
                    return Utils.getObject();
                } else if (t1 instanceof ArrayType arrayType) {
                    return BuildContext.get().getTypeSystem()
                            .getArrayType(Utils.getObject(), arrayType.dimensions());
                } else {
                    throw new UnsupportedOperationException();
                }
            }
        }

        public void addNewOutEdge(TypingFlowEdge edge) {
            if (outEdges.isEmpty()) {
                outEdges = new ArrayList<>();
            }
            outEdges.add(edge);
        }

        public void addNewInEdge(TypingFlowEdge edge) {
            if (inEdges.isEmpty()) {
                inEdges = new ArrayList<>();
            }
            inEdges.add(edge);
        }

        public void addNewUseConstrain(ReferenceType type) {
            if (useValidConstrains.isEmpty()) {
                useValidConstrains = new HashSet<>();
            }
            useValidConstrains.add(type);
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
