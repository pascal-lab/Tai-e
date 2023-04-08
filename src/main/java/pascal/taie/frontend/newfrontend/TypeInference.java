package pascal.taie.frontend.newfrontend;

import pascal.taie.ir.exp.Var;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;

import javax.annotation.Nullable;
import java.util.List;

public class TypeInference {

    AsmIRBuilder builder;

    public TypeInference(AsmIRBuilder builder) {
        this.builder = builder;
    }

    public ReferenceType lca(ReferenceType r1, ReferenceType r2) {
        return null;
    }

    public void build() {
    }

    record TypingFlowNode(
            Var var,
            @Nullable List<ReferenceType> types,
            @Nullable PrimitiveType primitiveType,
            @Nullable List<ReferenceType> useValidConstrains,
            List<TypingFlowEdges> inEdges,
            List<TypingFlowEdges> outEdges) {
    }

    record TypingFlowEdges(
            Kind kind,
            TypingFlowNode target) {
    }

    enum Kind {
        VAR_VAR,
        VAR_ARRAY,
        ARRAY_VAR
    }
}
