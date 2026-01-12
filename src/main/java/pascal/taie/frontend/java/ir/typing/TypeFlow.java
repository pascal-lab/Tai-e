package pascal.taie.frontend.java.ir.typing;

import java.util.Optional;

import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.language.type.Type;

final class TypeFlow {
    private final FrontendTypeSystem typeSystem;
    final TypeFlowEdge edge;
    private final Type type;

    TypeFlow(FrontendTypeSystem typeSystem, TypeFlowEdge edge, Type type) {
        this.typeSystem = typeSystem;
        this.edge = edge;
        this.type = type;
    }

    Optional<Type> getTargetType() {
        return switch (edge.kind()) {
            case VAR_VAR -> Optional.of(type);
            case VAR_ARRAY -> TypeUtils.plusOneArray(type, typeSystem);
            case ARRAY_VAR -> TypeUtils.subOneArray(type);
        };
    }

    Optional<Type> getSourceType() {
        return switch (edge.kind()) {
            case VAR_VAR -> Optional.of(type);
            case VAR_ARRAY -> TypeUtils.subOneArray(type);
            case ARRAY_VAR -> TypeUtils.plusOneArray(type, typeSystem);
        };
    }
}
