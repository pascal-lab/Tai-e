package pascal.taie.frontend.java.ir.typing;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.exp.VarMutator;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;

class TypeFlowGraph {
    private final FrontendTypeSystem typeSystem;
    private final TypeFlowNode[] nodes;

    TypeFlowGraph(FrontendTypeSystem typeSystem, int varSize) {
        this.typeSystem = typeSystem;
        this.nodes = new TypeFlowNode[varSize];
    }

    TypeFlowNode getNode(Var var) {
        if (nodes[var.getIndex()] == null) {
            nodes[var.getIndex()] = new TypeFlowNode(typeSystem, var);
        }
        return nodes[var.getIndex()];
    }

    void addConstantEdge(Type t, Var v) {
        assert v != null;
        TypeFlowNode node = getNode(v);
        node.setNewType(t);
    }

    void addVarEdge(Var from, Var to, EdgeKind kind) {
        if (from.getType() != null && kind != EdgeKind.VAR_ARRAY) {
            computeFlowOutType(from.getType(), kind).ifPresent((t) -> {
                addConstantEdge(t, to);
            });
        } else {
            TypeFlowNode n1 = getNode(from);
            TypeFlowNode n2 = getNode(to);
            TypeFlowEdge edge = new TypeFlowEdge(kind, n1, n2);
            n2.addNewInEdge(edge);
            n1.addNewOutEdge(edge);
        }
    }

    private Optional<Type> computeFlowOutType(Type t, EdgeKind kind) {
        return switch (kind) {
            case VAR_VAR -> Optional.of(t);
            case VAR_ARRAY -> TypeInference.plusOneArray(t, typeSystem);
            case ARRAY_VAR -> TypeInference.subOneArray(t);
        };
    }

    public void addUseConstrain(Var v, ReferenceType constrain) {
        TypeFlowNode node = getNode(v);
        node.addNewUseConstrain(constrain);
    }

    public void inferTypes() {
        inferUseConstrains();

        Queue<FlowType> queue = new LinkedList<>();
        for (TypeFlowNode node : nodes) {
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
        for (TypeFlowNode node : nodes) {
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
        TypeFlowNode now = type.edge.source();
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
        if (type.edge.kind() == EdgeKind.VAR_ARRAY) {
            return;
        }
        TypeFlowNode now = type.edge.target();
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
            boolean needSpread = now.onNewReferenceType(type.edge.kind(), rType);
            if (needSpread) {
                flowOutType(queue, now, now.referenceType);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void flowOutType(Queue<FlowType> queue, TypeFlowNode node, PrimitiveType pType) {
        if (node.outEdges == null) {
            return;
        }
        for (TypeFlowEdge e : node.outEdges) {
            queue.add(new FlowType(typeSystem, e, pType));
        }
    }

    private void flowOutType(Queue<FlowType> queue, TypeFlowNode node, ReferenceType type) {
        if (node.outEdges == null) {
            return;
        }
        for (TypeFlowEdge e : node.outEdges) {
            queue.add(new FlowType(typeSystem, e, type));
        }
    }

    private void flowOutConstrains(Queue<FlowType> queue, TypeFlowNode node, ReferenceType type) {
        if (node.inEdges == null) {
            return;
        }
        for (TypeFlowEdge e : node.inEdges) {
            queue.add(new FlowType(typeSystem, e, type));
        }
    }

    boolean setTypes() {
        boolean needCasting = false;
        for (TypeFlowNode node : nodes) {
            if (node == null) {
                continue;
            }
            Var v = node.var();
            if (v.getType() != null) {
                continue;
            }
            if (node.primitiveType != null) {
                VarMutator.setType(v, node.primitiveType);
            } else {
                ReferenceType target = node.referenceType;
                if (target == null) {
                    target = NullType.NULL;
                }
                if (!node.isUseValid(target)) {
                    needCasting = true;
                }
                if (node.initConstraints != null) {
                    for (Type t : node.initConstraints) {
                        if (!typeSystem.isAssignable(t, target)) {
                            needCasting = true;
                        }
                    }
                }
                VarMutator.setType(v, target);
            }
        }
        return needCasting;
    }
}
