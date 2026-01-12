package pascal.taie.frontend.java.ir.typing;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.frontend.java.ir.BytecodeBlock;
import pascal.taie.frontend.java.ir.BytecodeCFG;
import pascal.taie.frontend.java.ir.VarManager;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.exp.VarMutator;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
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

    private TypeFlowNode getNode(Var var) {
        if (nodes[var.getIndex()] == null) {
            nodes[var.getIndex()] = new TypeFlowNode(typeSystem, var);
        }
        return nodes[var.getIndex()];
    }

    void addType(Var var, Type type) {
        assert var != null;
        TypeFlowNode node = getNode(var);
        node.addType(type);
    }

    void addVarTypeFlow(Var from, Var to, FlowKind kind) {
        if (from.getType() != null && kind != FlowKind.VAR_ARRAY) {
            // Skip VAR_ARRAY as we can not infer the type of `to` from `from` directly
            Type type = from.getType();
            switch (kind) {
                case VAR_VAR -> addType(to, type);
                case ARRAY_VAR -> TypeUtils.subOneArray(type).ifPresent(t -> addType(to, t));
            }
        } else {
            TypeFlowNode n1 = getNode(from);
            TypeFlowNode n2 = getNode(to);
            TypeFlowEdge edge = new TypeFlowEdge(kind, n1, n2);
            n2.addInEdge(edge);
            n1.addOutEdge(edge);
        }
    }

    void addUseConstraint(Var var, ReferenceType constraint) {
        TypeFlowNode node = getNode(var);
        node.addUseConstraint(constraint);
    }

    void addInitConstraint(Var var, ReferenceType constraint) {
        TypeFlowNode node = getNode(var);
        node.addInitConstraint(constraint);
    }

    void inferTypes() {
        inferUseConstraints();

        Queue<TypeFlow> queue = new LinkedList<>();
        for (TypeFlowNode node : nodes) {
            if (node == null) {
                continue;
            }
            if (node.primitiveType != null) {
                PrimitiveType t = node.primitiveType;
                propogateType(queue, node, t);
            } else if (node.candidateRefTypes != null) {
                node.computeReferenceType();
                propogateType(queue, node, node.referenceType);
            }
        }

        while (!queue.isEmpty()) {
            TypeFlow flow = queue.poll();
            TypeFlowNode node = flow.getTargetNode();
            Optional<Type> optionalType = flow.getTargetType();
            if (optionalType.isEmpty()) {
                continue;
            }
            Type t = optionalType.get();
            if (t instanceof PrimitiveType pType) {
                if (node.addPrimitiveType(pType)) {
                    propogateType(queue, node, pType);
                }
            } else if (t instanceof ReferenceType rType) {
                boolean changed = node.updateReferenceType(rType);
                if (changed) {
                    propogateType(queue, node, node.referenceType);
                }
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private void inferUseConstraints() {
        Queue<ConstraintFlow> queue = new LinkedList<>();
        for (TypeFlowNode node : nodes) {
            if (node == null) {
                continue;
            }
            if (node.useConstraints != null) {
                for (ReferenceType t : node.useConstraints) {
                    propogateConstraints(queue, node, t);
                }
            }
        }

        while (!queue.isEmpty()) {
            ConstraintFlow flow = queue.poll();
            TypeFlowNode node = flow.getTargetNode();
            Optional<Type> optionalType = flow.getTargetConstraintType();
            if (optionalType.isEmpty()) {
                continue;
            }
            Type t = optionalType.get();
            if (t instanceof ReferenceType rType) {
                if (node.addUseConstraint(rType)) {
                    propogateConstraints(queue, node, rType);
                }
            }
        }
    }

    private void propogateType(Queue<TypeFlow> queue, TypeFlowNode node, Type type) {
        if (node.outEdges == null) {
            return;
        }
        for (TypeFlowEdge edge : node.outEdges) {
            if (edge.kind() == FlowKind.VAR_ARRAY) {
                // array is covariant, so we do not infer type from VAR_ARRAY edge
                continue;
            }
            queue.add(new TypeFlow(typeSystem, edge, type));
        }
    }

    private void propogateConstraints(Queue<ConstraintFlow> queue, TypeFlowNode node, ReferenceType type) {
        if (node.inEdges == null) {
            return;
        }
        for (TypeFlowEdge edge : node.inEdges) {
            queue.add(new ConstraintFlow(typeSystem, edge, type));
        }
    }

    boolean applyInferredTypes() {
        boolean needInsertCast = false;
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
                if (!node.satisfyUseConstraints(target)) {
                    needInsertCast = true;
                }
                if (node.initConstraints != null) {
                    for (Type t : node.initConstraints) {
                        if (!typeSystem.isAssignable(t, target)) {
                            needInsertCast = true;
                        }
                    }
                }
                VarMutator.setType(v, target);
            }
        }
        return needInsertCast;
    }

    void initialize(JMethod method, BytecodeCFG cfg, VarManager varManager) {
        addTypesForParams(method, varManager);
        GraphBuilder graphBuilder = new GraphBuilder(typeSystem, this, method);
        for (BytecodeBlock block : cfg) {
            addExceptionTypes(block);
            for (Stmt stmt : block.getStmts()) {
                stmt.accept(graphBuilder);
            }
        }
    }

    private void addExceptionTypes(BytecodeBlock block) {
        if (block.getExceptionHandlerTypes() != null) {
            Var exceptionRef = null;
            for (Stmt stmt : block.getStmts()) {
                if (stmt instanceof Catch catchStmt) {
                    exceptionRef = catchStmt.getExceptionRef();
                    break;
                }
            }
            if (exceptionRef != null) {
                for (ReferenceType type : block.getExceptionHandlerTypes()) {
                    addType(exceptionRef, type);
                }
            }
        }
    }

    private void addTypesForParams(JMethod method, VarManager varManager) {
        if (!method.isStatic()) {
            Var thisVar = varManager.getThisVar();
            addType(thisVar, method.getDeclaringClass().getType());
        }

        for (int i = 0; i < method.getParamCount(); ++i) {
            Var param = varManager.getParams().get(i);
            Type type = method.getParamType(i);
            addType(param, type);
        }
    }
}
