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

/**
 * Represents the graph structure used for the pruning-based type inference algorithm.
 */
final class TypeFlowGraph {

    private final FrontendTypeSystem typeSystem;

    /**
     * The array of nodes in the graph, indexed by Var.getIndex().
     */
    private final TypeFlowNode[] nodes;

    TypeFlowGraph(FrontendTypeSystem typeSystem, int varSize) {
        this.typeSystem = typeSystem;
        this.nodes = new TypeFlowNode[varSize];
    }

    // ========================================================================
    // 1. Build Phase
    // ========================================================================

    /**
     * Initializes the graph by building the edges, types and constraints from the method.
     */
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

    /**
     * Adds types for 'this' and parameters.
     */
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

    /**
     * Adds types for exception variables in catch blocks.
     */
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

    /**
     * Add a possible type for a var.
     */
    void addType(Var var, Type type) {
        assert var != null;
        TypeFlowNode node = getNode(var);
        node.addType(type);
    }

    /**
     * Add an edge or a type to flow type from `from` to `to`.
     */
    void addVarTypeFlow(Var from, Var to, FlowKind kind) {
        if (from.getType() != null && kind != FlowKind.VAR_ARRAY) {
            // Skip VAR_ARRAY as we can not infer the type of `to` from `from` directly
            Type type = from.getType();
            switch (kind) {
                case VAR_VAR -> addType(to, type);
                case ARRAY_VAR -> TypeUtils.decreaseDim(type).ifPresent(t -> addType(to, t));
            }
        } else {
            TypeFlowNode n1 = getNode(from);
            TypeFlowNode n2 = getNode(to);
            TypeFlowEdge edge = new TypeFlowEdge(kind, n1, n2);
            n2.addInEdge(edge);
            n1.addOutEdge(edge);
        }
    }

    /**
     * Adds a usage constraint to a var.
     */
    void addUseConstraint(Var var, ReferenceType constraint) {
        TypeFlowNode node = getNode(var);
        node.addUseConstraint(constraint);
    }

    /**
     * Adds an initialization constraint (e.g., constructor call) to a var.
     */
    void addInitConstraint(Var var, ReferenceType constraint) {
        TypeFlowNode node = getNode(var);
        node.addInitConstraint(constraint);
    }

    // ========================================================================
    // 2. Type Inference Phase
    // ========================================================================

    void inferTypes() {
        propagateUseConstraints();
        propagateTypes();
    }

    /**
     * Back-propagates usage constraints.
     */
    private void propagateUseConstraints() {
        Queue<ConstraintFlow> queue = new LinkedList<>();
        for (TypeFlowNode node : nodes) {
            if (node == null) {
                continue;
            }
            for (ReferenceType t : node.getUseConstraints()) {
                propagateConstraints(queue, node, t);
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
                    propagateConstraints(queue, node, rType);
                }
            }
        }
    }

    private void propagateConstraints(Queue<ConstraintFlow> queue, TypeFlowNode node, ReferenceType type) {
        for (TypeFlowEdge edge : node.getInEdges()) {
            queue.add(new ConstraintFlow(typeSystem, edge, type));
        }
    }


    /**
     * Forward-propagates concrete types (pruned by constraints) until a fixed point is reached.
     */
    private void propagateTypes() {
        Queue<TypeFlow> queue = new LinkedList<>();
        for (TypeFlowNode node : nodes) {
            if (node == null) {
                continue;
            }
            if (node.getType() != null) {
                propagateType(queue, node, node.getType());
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
                if (node.updatePrimitiveType(pType)) {
                    // TODO: use node's type?
                    propagateType(queue, node, pType);
                }
            } else if (t instanceof ReferenceType rType) {
                boolean changed = node.updateReferenceType(rType);
                if (changed) {
                    propagateType(queue, node, node.getType());
                }
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private void propagateType(Queue<TypeFlow> queue, TypeFlowNode node, Type type) {
        for (TypeFlowEdge edge : node.getOutEdges()) {
            if (edge.kind() == FlowKind.VAR_ARRAY) {
                // array is covariant, so we do not infer type from VAR_ARRAY edge
                continue;
            }
            queue.add(new TypeFlow(typeSystem, edge, type));
        }
    }

    /**
     * Applies the inferred types to the IR vars and checks if casting is needed.
     */
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
            if (node.getType() instanceof PrimitiveType) {
                VarMutator.setType(v, node.getType());
            } else {
                ReferenceType target = (ReferenceType) node.getType();
                if (target == null) {
                    target = NullType.NULL;
                }
                if (!node.satisfyUseConstraints(target)) {
                    needInsertCast = true;
                }
                for (Type t : node.getInitConstraints()) {
                    if (!typeSystem.isAssignable(t, target)) {
                        needInsertCast = true;
                    }
                }
                VarMutator.setType(v, target);
            }
        }
        return needInsertCast;
    }

    // ========================================================================
    // 3. Helpers
    // ========================================================================

    private TypeFlowNode getNode(Var var) {
        if (nodes[var.getIndex()] == null) {
            nodes[var.getIndex()] = new TypeFlowNode(typeSystem, var);
        }
        return nodes[var.getIndex()];
    }
}
