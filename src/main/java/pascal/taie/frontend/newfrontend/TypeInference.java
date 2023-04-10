package pascal.taie.frontend.newfrontend;

import pascal.taie.ir.exp.Var;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TypeInference {

    AsmIRBuilder builder;

    public TypeInference(AsmIRBuilder builder) {
        this.builder = builder;
    }

    static public ReferenceType lca(ReferenceType r1, ReferenceType r2) {
        return null;
    }

    public void build() {
    }

    static class TypingFlowGraph {
        Map<Var, TypingFlowNode> nodes;

        public void addConstantEdge(Var v, EdgeKind kind, Type t) {
            TypingFlowNode node = nodes.computeIfAbsent(v, (var) -> new TypingFlowNode(kind.getTarget(), v));
            node.setNewType(t);
        }

        public void addVarEdge(Var v1, Var v2, EdgeKind kind) {
            TypingFlowNode n1 = nodes.computeIfAbsent(v1, (var) -> new TypingFlowNode(kind.getSource(), var));
            TypingFlowNode n2 = nodes.computeIfAbsent(v2, (var) -> new TypingFlowNode(kind.getTarget(), var));
            TypingFlowEdge edge = new TypingFlowEdge(kind, n1, n2);
            n1.addNewInEdge(edge);
            n2.addNewOutEdge(edge);
        }
    }

    static final class TypingFlowNode {
        private final NodeKind kind;
        private final Var var;
        @Nullable
        private Set<ReferenceType> types;
        @Nullable
        private PrimitiveType primitiveType;
        @Nullable
        private Set<ReferenceType> useValidConstrains;

        private List<TypingFlowEdge> inEdges;

        private List<TypingFlowEdge> outEdges;

        TypingFlowNode(NodeKind kind, Var var) {
            this.kind = kind;
            this.var = var;
            inEdges = List.of();
            outEdges = List.of();
        }

        public void setNewType(Type t) {
            if (t instanceof PrimitiveType pType) {
                assert types == null;
                if (primitiveType == null) {
                    primitiveType = pType;
                } else {
                    assert pType == primitiveType;
                }
            } else {
                assert primitiveType == null;
                ReferenceType rType = (ReferenceType) t;
                if (types == null) {
                    types = new HashSet<>();
                    types.add(rType);
                } else {
                    for (ReferenceType type: types) {
                        types.add(lca(rType, type));
                    }
                }
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

        public NodeKind kind() {
            return kind;
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

        @Nullable
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
