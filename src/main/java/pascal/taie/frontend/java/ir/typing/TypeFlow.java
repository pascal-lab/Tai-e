package pascal.taie.frontend.java.ir.typing;

import java.util.Optional;

import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.language.type.Type;

/**
 * Represents the forward propagation of the concrete type along a graph edge.
 */
final class TypeFlow {

    private final FrontendTypeSystem typeSystem;

    /**
     * The edge along which the type is propagating.
     */
    private final TypeFlowEdge edge;

    /**
     * The source type being propagated.
     */
    private final Type type;

    TypeFlow(FrontendTypeSystem typeSystem, TypeFlowEdge edge, Type type) {
        this.typeSystem = typeSystem;
        this.edge = edge;
        this.type = type;
    }

    /**
     * Returns the target node for this type propagation.
     */
    TypeFlowNode getTargetNode() {
        return edge.target();
    }

    /**
     * Calculates the type to be applied to the target node.
     */
    Optional<Type> getTargetType() {
        return switch (edge.kind()) {
            case VAR_VAR -> Optional.of(type);
            case VAR_ARRAY -> TypeUtils.increaseDim(type, typeSystem);
            case ARRAY_VAR -> TypeUtils.decreaseDim(type);
        };
    }
}
