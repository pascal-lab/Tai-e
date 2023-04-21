package pascal.taie.frontend.newfrontend;

import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Binary;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Copy;
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
import pascal.taie.util.collection.Sets;

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

    AsmIRBuilder builder;

    public TypeInference(AsmIRBuilder builder) {
        this.builder = builder;
    }

    static public ReferenceType lca(ReferenceType r1, ReferenceType r2) {
        return null;
    }


    static public boolean isSubType(ReferenceType s, ReferenceType t) {
        return false;
    }

    static public ArrayType plusOneArray(Type t) {
        Type baseType;
        int dim;
        if (t instanceof ArrayType at) {
            baseType = at.baseType();
            dim = at.dimensions() + 1;
        } else {
            baseType = t;
            dim = 1;
        }
        return BuildContext.get().getTypeSystem().getArrayType(baseType, dim);
    }

    static public Optional<Type> subOneArray(Type t) {
        if (t instanceof ArrayType at) {
            return Optional.of(at.elementType());
        } else {
            return Optional.empty();
        }
    }

    public void build() {
        TypingFlowGraph graph = new TypingFlowGraph();

        for (Stmt stmt : builder.stmts) {
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
                    graph.addVarEdge(stmt.getLValue(), stmt.getRValue(), EdgeKind.VAR_VAR);
                    return null;
                }

                @Override
                public Void visit(LoadArray stmt) {
                    graph.addVarEdge(stmt.getRValue().getBase(), stmt.getLValue(), EdgeKind.VAR_ARRAY);
                    return null;
                }

                @Override
                public Void visit(StoreArray stmt) {
                    graph.addVarEdge(stmt.getRValue(), stmt.getLValue().getBase(), EdgeKind.ARRAY_VAR);
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
                    return StmtVisitor.super.visit(stmt);
                }

                @Override
                public Void visit(Binary stmt) {
                    graph.addVarEdge(stmt.getRValue().getOperand1(), stmt.getLValue(), EdgeKind.VAR_VAR);
                    return null;
                }

                @Override
                public Void visit(Unary stmt) {
                    graph.addVarEdge(stmt.getRValue().getOperand(), stmt.getLValue(), EdgeKind.VAR_VAR);
                    return null;
                }

                @Override
                public Void visit(Cast stmt) {
                    graph.addConstantEdge(stmt.getRValue().getType(), stmt.getLValue());
                    return null;
                }

                @Override
                public Void visit(Invoke stmt) {
                    graph.addConstantEdge(stmt.getRValue().getType(), stmt.getLValue());

                    if (stmt.getRValue() instanceof InvokeInstanceExp invokeInstanceExp) {
                        graph.addUseConstrain(invokeInstanceExp.getBase(),
                                invokeInstanceExp.getMethodRef().getDeclaringClass().getType());
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
            });
        }

        graph.inferTypes();
    }

    static class TypingFlowGraph {
        Map<Var, TypingFlowNode> nodes;

        public void addConstantEdge(Type t, Var v) {
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
            Queue<FlowType> queue = new LinkedList<>();
            for (TypingFlowNode node : nodes.values()) {
                if (node.primitiveType != null) {
                    PrimitiveType t = node.primitiveType;
                    queue.addAll(flowOutType(node, t));
                } else {
                    assert node.types != null;
                    node.applyUseConstrains();
                    queue.addAll(flowOutType(node, node.types));
                }
            }

            while (!queue.isEmpty()) {
                FlowType now = queue.poll();
                queue.addAll(spreadingFlowType(now));
            }
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
                Set<ReferenceType> delta = now.onNewReferenceType(rType, true);
                if (delta.size() != 0) {
                    return flowOutType(now, delta);
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

        private List<FlowType> flowOutType(TypingFlowNode node, Set<ReferenceType> rType) {
            return node.outEdges
                    .stream()
                    .flatMap(e -> rType.stream().map(t -> new FlowType(e, t)))
                    .toList();
        }
    }

    record FlowType(TypingFlowEdge edge, Type type) {
        Optional<Type> getTargetType() {
            return switch (edge.kind) {
                case VAR_VAR -> Optional.of(type);
                case VAR_ARRAY -> Optional.of(plusOneArray(type));
                case ARRAY_VAR -> subOneArray(type);
            };
        }
    }

    static final class TypingFlowNode {
        private final Var var;
        @Nullable
        private Set<ReferenceType> types;
        @Nullable
        private PrimitiveType primitiveType;
        private Set<ReferenceType> useValidConstrains;

        private List<TypingFlowEdge> inEdges;

        private List<TypingFlowEdge> outEdges;

        TypingFlowNode(Var var) {
            this.var = var;
            useValidConstrains = Set.of();
            inEdges = List.of();
            outEdges = List.of();
        }

        public void setNewType(Type t) {
            if (t instanceof PrimitiveType pType) {
                onNewPrimitiveType(pType);
            } else if (t instanceof ReferenceType rType) {
                onNewReferenceType(rType, false);
            } else {
                throw new UnsupportedOperationException();
            }
        }

        public boolean onNewPrimitiveType(PrimitiveType t) {
            assert types == null;
            if (this.primitiveType == null) {
                this.primitiveType = t;
                return true;
            } else {
                assert this.primitiveType == t;
                return false;
            }
        }

        public Set<ReferenceType> onNewReferenceType(ReferenceType t, boolean confirmUseValid) {
            assert primitiveType == null;
            if (types == null) {
                types = new HashSet<>();
                types.add(t);
                return Set.of(t);
            } else {
                Set<ReferenceType> res = Sets.newHybridSet();
                for (ReferenceType type: types) {
                    ReferenceType a = lca(t, type);
                    if (! confirmUseValid || useValid(a)) {
                        if (types.add(a)) {
                            res.add(a);
                        }
                    }
                }
                return res;
            }
        }

        public void addNewOutEdge(TypingFlowEdge edge) {
            if (outEdges.size() == 0) {
                outEdges = new ArrayList<>();
            }
            outEdges.add(edge);
        }

        public void addNewInEdge(TypingFlowEdge edge) {
            if (inEdges.size() == 0) {
                inEdges = new ArrayList<>();
            }
            inEdges.add(edge);
        }

        public void addNewUseConstrain(ReferenceType type) {
            if (useValidConstrains.size() == 0) {
                useValidConstrains = new HashSet<>();
            }
            useValidConstrains.add(type);
        }

        public void applyUseConstrains() {
            if (useValidConstrains.size() != 0) {
                assert types != null;
                types.removeIf((s) -> ! useValid(s));
            }
        }

        public boolean useValid(ReferenceType type) {
            return useValidConstrains
                    .stream()
                    .allMatch(t -> isSubType(type, t));
        }

        public List<TypingFlowEdge> getInEdges() {
            return inEdges;
        }

        /**
         * set type for var, this node is completed and removed from algorithm
         */
        public void complete() {
            assert types == null;
            // TODO: set var type
        }

        public Var var() {
            return var;
        }

        @Nullable
        public Set<ReferenceType> types() {
            return types;
        }

        @Nullable
        public PrimitiveType primitiveType() {
            return primitiveType;
        }

        public Set<ReferenceType> useValidConstrains() {
            return useValidConstrains;
        }

        public List<TypingFlowEdge> inEdges() {
            return inEdges;
        }

        public List<TypingFlowEdge> outEdges() {
            return outEdges;
        }
    }

    record TypingFlowEdge(
            EdgeKind kind,
            TypingFlowNode source,
            TypingFlowNode target) {
    }

    enum EdgeKind {
        VAR_VAR,
        VAR_ARRAY,
        ARRAY_VAR;

        NodeKind getTarget() {
            return switch (this) {
                case VAR_VAR, ARRAY_VAR -> NodeKind.VAR;
                case VAR_ARRAY -> NodeKind.ArrayAccess;
            };
        }

        NodeKind getSource() {
            return switch (this) {
                case VAR_VAR, VAR_ARRAY -> NodeKind.VAR;
                case ARRAY_VAR -> NodeKind.ArrayAccess;
            };
        }
    }

    enum NodeKind {
        VAR,
        ArrayAccess
    }
}
