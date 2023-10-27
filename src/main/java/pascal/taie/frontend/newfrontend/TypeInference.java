package pascal.taie.frontend.newfrontend;

import pascal.taie.ir.exp.ArrayLengthExp;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
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
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import static pascal.taie.frontend.newfrontend.Utils.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

public class TypeInference {

    final AsmIRBuilder builder;

    final TypingFlowGraph graph;

    public TypeInference(AsmIRBuilder builder) {
        this.builder = builder;
        graph = new TypingFlowGraph();
    }

    static public Set<ReferenceType> lca(ReferenceType r1, ReferenceType r2) {
        return Utils.lca(r1, r2);
    }

    static public Optional<Type> plusOneArray(Type t) {
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

    static public Optional<Type> subOneArray(Type t) {
        if (t instanceof ArrayType at) {
            return Optional.of(at.elementType());
        } else {
            return Optional.empty();
        }
    }

    public void build() {

        addThisParam();
        addExceptionRef();

        for (Stmt stmt : builder.getAllStmts()) {
            stmt.accept(new StmtVisitor<Void>() {
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
                        graph.addConstantEdge(PrimitiveType.INT, stmt.getLValue());
                    } else {
                        graph.addVarEdge(stmt.getRValue().getOperand(), stmt.getLValue(), EdgeKind.VAR_VAR);
                    }
                    return null;
                }

                @Override
                public Void visit(InstanceOf stmt) {
                    graph.addConstantEdge(PrimitiveType.BOOLEAN, stmt.getLValue());
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
                    if (lValue != null) {
                        graph.addConstantEdge(stmt.getRValue().getType(), lValue);
                    }

                    if (stmt.getRValue() instanceof InvokeInstanceExp invokeInstanceExp) {
                        if (! stmt.getMethodRef().getName().equals(MethodNames.INIT)) {
                            graph.addUseConstrain(invokeInstanceExp.getBase(),
                                    invokeInstanceExp.getMethodRef().getDeclaringClass().getType());
                        }
                    }

                    List<Type> paraTypes = stmt.getRValue().getMethodRef().getParameterTypes();
                    List<Var> args = stmt.getRValue().getArgs();
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
                        graph.addUseConstrain(stmt.getValue(), r);
                    }
                    return StmtVisitor.super.visit(stmt);
                }
            });
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
                k.setType(v.primitiveType);
            } else {
                if (v.referenceType != null) {
                    k.setType(v.referenceType);
                } else {
                    // TODO: add warning here
                    k.setType(NullType.NULL);
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

    private void addExceptionRef() {
        for (ExceptionEntry entry : builder.getExceptionEntries()) {
            graph.addConstantEdge(entry.catchType(), entry.handler().getExceptionRef());
        }
    }

    static class TypingFlowGraph {
        Map<Var, TypingFlowNode> nodes = Maps.newHybridMap();

        public void addConstantEdge(Type t, Var v) {
            assert v != null;
            TypingFlowNode node = nodes.computeIfAbsent(v, TypingFlowNode::new);
            node.setNewType(t);
        }

        public void addVarEdge(Var from, Var to, EdgeKind kind) {
            if (from.getType() != null) {
                addConstantEdge(from.getType(), to);
            } else {
                TypingFlowNode n1 = nodes.computeIfAbsent(from, TypingFlowNode::new);
                TypingFlowNode n2 = nodes.computeIfAbsent(to, TypingFlowNode::new);
                TypingFlowEdge edge = new TypingFlowEdge(kind, n1, n2);
                n2.addNewInEdge(edge);
                n1.addNewOutEdge(edge);
            }
        }

        public void addUseConstrain(Var v, ReferenceType constrain) {
            TypingFlowNode node = nodes.computeIfAbsent(v, TypingFlowNode::new);
            node.addNewUseConstrain(constrain);
        }

        public void inferTypes() {
            inferUseConstrains();

            Queue<FlowType> queue = new LinkedList<>();
            for (TypingFlowNode node : nodes.values()) {
                if (node.primitiveType != null) {
                    PrimitiveType t = node.primitiveType;
                    queue.addAll(flowOutType(node, t));
                } else if (node.types != null) {
                    node.firstResolve();
                    queue.addAll(flowOutType(node, node.referenceType));
                }
            }

            while (!queue.isEmpty()) {
                FlowType now = queue.poll();
                queue.addAll(spreadingFlowType(now));
            }
        }

        public void inferUseConstrains() {
            Queue<FlowType> queue = new LinkedList<>();
            for (TypingFlowNode node : nodes.values()) {
                if (node.useValidConstrains != null) {
                    for (ReferenceType t : node.useValidConstrains) {
                        queue.addAll(flowOutConstrains(node, t));
                    }
                }
            }

            while (!queue.isEmpty()) {
                FlowType now = queue.poll();
                queue.addAll(spreadingUseConstrains(now));
            }
        }

        private List<FlowType> spreadingUseConstrains(FlowType type) {
            TypingFlowNode now = type.edge.source;
            Optional<Type> optionalType = type.getSourceType();
            if (optionalType.isEmpty()) {
                return List.of();
            }
            Type t = optionalType.get();
            if (t instanceof ReferenceType t1) {
                if (now.useValidConstrains.add(t1)) {
                    return flowOutConstrains(now, t1);
                }
            }
            return List.of();
        }

        private List<FlowType> spreadingFlowType(FlowType type) {
            TypingFlowNode now = type.edge.target;
            Optional<Type> optionalType = type.getTargetType();
            if (optionalType.isEmpty()) {
                return List.of();
            }
            Type t = optionalType.get();
            if (t instanceof PrimitiveType pType) {
                if (now.onNewPrimitiveType(pType)) {
                    return flowOutType(now, pType);
                }
            } else if (t instanceof ReferenceType rType) {
                boolean needSpread = now.onNewReferenceType(type.edge.kind, rType);
                if (needSpread) {
                    return flowOutType(now, now.referenceType);
                }
            } else {
                throw new UnsupportedOperationException();
            }

            return List.of();
        }

        private List<FlowType> flowOutType(TypingFlowNode node, PrimitiveType pType) {
            return node.outEdges.stream()
                    .map(e -> new FlowType(e, pType))
                    .toList();
        }

        private List<FlowType> flowOutType(TypingFlowNode node, ReferenceType type) {
            return node.outEdges
                    .stream()
                    .map(e -> new FlowType(e, type))
                    .toList();
        }

        private List<FlowType> flowOutConstrains(TypingFlowNode node, ReferenceType type) {
            return node.inEdges
                    .stream()
                    .map(e -> new FlowType(e, type))
                    .toList();
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
