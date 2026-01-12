package pascal.taie.frontend.java.ir.typing;

import java.util.Optional;

import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.language.type.Type;

/**
 * Represents the backward propagation of type constraints along a graph edge.
 */
final class ConstraintFlow {

    private final FrontendTypeSystem typeSystem;

    /**
     * The edge along which the constraint is propagating backwards.
     */
    private final TypeFlowEdge edge;

    /**
     * The source constraint being propagated.
     */
    private final Type type;

    ConstraintFlow(FrontendTypeSystem typeSystem, TypeFlowEdge edge, Type originalConstraint) {
        this.typeSystem = typeSystem;
        this.edge = edge;
        this.type = originalConstraint;
    }

    /**
     * Returns the target node for this constraint propagation.
     */
    TypeFlowNode getTargetNode() {
        return edge.source();
    }

    /**
     * Calculates the constraint to be applied to the target node.
     */
    Optional<Type> getTargetConstraintType() {
        return switch (edge.kind()) {
            case VAR_VAR -> Optional.of(type);
            case VAR_ARRAY -> TypeUtils.decreaseDim(type);
            case ARRAY_VAR -> TypeUtils.increaseDim(type, typeSystem);
        };
    }
}
