package pascal.taie.frontend.java.ir.typing;

import java.util.Optional;

import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.language.type.Type;

final class ConstraintFlow {
    private final FrontendTypeSystem typeSystem;
    private final TypeFlowEdge edge;
    private final Type type;

    ConstraintFlow(FrontendTypeSystem typeSystem, TypeFlowEdge edge, Type originalConstraint) {
        this.typeSystem = typeSystem;
        this.edge = edge;
        this.type = originalConstraint;
    }

    TypeFlowNode getTargetNode() {
        return edge.source();
    }

    Optional<Type> getTargetConstraintType() {
        return switch (edge.kind()) {
            case VAR_VAR -> Optional.of(type);
            case VAR_ARRAY -> TypeUtils.subOneArray(type);
            case ARRAY_VAR -> TypeUtils.plusOneArray(type, typeSystem);
        };
    }
}
