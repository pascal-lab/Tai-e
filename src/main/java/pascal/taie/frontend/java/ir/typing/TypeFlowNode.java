package pascal.taie.frontend.java.ir.typing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.collection.Sets;

/**
 * Represents a node in the type flow graph.
 */
final class TypeFlowNode {

    private final FrontendTypeSystem typeSystem;

    /**
     * The IR var associated with this node.
     */
    private final Var var;

    /**
     * The current inferred primitive type.
     */
    @Nullable
    private PrimitiveType primitiveType;

    /**
     * The current inferred reference type.
     */
    @Nullable
    private ReferenceType referenceType;

    /**
     * The set of candidate reference types.
     * Caculate to {@link #referenceType} when constraint propagation is complete.
     */
    private Set<ReferenceType> candidateRefTypes;

    /**
     * The set of types that this var must be assignable to.
     */
    private Set<ReferenceType> useConstraints;

    /**
     * The list of types required by constructor.
     */
    private List<ReferenceType> initConstraints;

    private List<TypeFlowEdge> inEdges;

    private List<TypeFlowEdge> outEdges;

    TypeFlowNode(FrontendTypeSystem typeSystem, Var var) {
        this.typeSystem = typeSystem;
        this.var = var;
        this.useConstraints = null;
        this.inEdges = null;
        this.outEdges = null;
    }

    /**
     * Returns the current inferred type.
     */
    Type getType() {
        if (primitiveType != null) {
            return primitiveType;
        } else if (referenceType != null) {
            return referenceType;
        } else if (candidateRefTypes != null) {
            for (ReferenceType r : candidateRefTypes) {
                updateReferenceType(r);
            }
            return referenceType;
        } else {
            return null;
        }
    }

    /**
     * Add an inferred type to this variable.
     * Use {@link #addCandidateRefType} because we should NOT calculate LCA until constraint
     * propagation is complete.
     */
    void addType(Type type) {
        if (type instanceof PrimitiveType pType) {
            updatePrimitiveType(pType);
        } else if (type instanceof ReferenceType rType) {
            addCandidateRefType(rType);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Updates the primitive type, returning true if it changed.
     */
    boolean updatePrimitiveType(PrimitiveType type) {
        // TODO: perform numeric promotion
        assert referenceType == null && candidateRefTypes == null;
        assert type != null;
        if (this.primitiveType == null) {
            this.primitiveType = type;
            return true;
        } else {
            if (this.primitiveType == type) {
                return false;
            } else if (TypeSystem.canHoldInt(primitiveType)
                    && TypeSystem.canHoldInt(type)) {
                return false;
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * Adds a candidate reference type.
     */
    private void addCandidateRefType(ReferenceType t) {
        assert primitiveType == null;
        if (candidateRefTypes == null) {
            candidateRefTypes = Sets.newHybridSet();
        }
        candidateRefTypes.add(t);
    }

    /**
     * Updates the inferred type by computing the pruned LCA, returning true if changed.
     */
    boolean updateReferenceType(ReferenceType t) {
        if (referenceType == null) {
            referenceType = t;
            return true;
        } else {
            ReferenceType oldRefType = referenceType;
            referenceType = getSuitableLCA(referenceType, t);
            return oldRefType != referenceType;
        }
    }

    /**
     * Checks if a type satisfies all the use constraints.
     */
    boolean satisfyUseConstraints(ReferenceType t) {
        if (useConstraints == null) {
            return true;
        }
        boolean ret = true;
        for (ReferenceType c : useConstraints) {
            ret &= typeSystem.isAssignable(c, t);
        }
        return ret;
    }

    /**
     * Computes the LCA of two types, pruning candidates that violate constraints.
     */
    private ReferenceType getSuitableLCA(ReferenceType type1, ReferenceType type2) {
        if (type1 == type2) {
            return type2;
        }
        Set<ReferenceType> lcas = typeSystem.lca(type1, type2);
        ReferenceType target = null;
        for (ReferenceType lca : lcas) {
            if (satisfyUseConstraints(lca)) {
                target = lca;
                break;
            }
        }
        if (target != null) {
            // any type in types is use valid
            return target;
        } else {
            if (lcas.isEmpty()) {
                // normally impossible, but possible for phantom
                return typeSystem.objectType();
            }
            // fallback to Object or Object array
            ReferenceType lca = lcas.iterator().next();
            if (lca instanceof ClassType) {
                return typeSystem.objectType();
            } else if (lca instanceof ArrayType arrayType) {
                return typeSystem.getArrayType(typeSystem.objectType(), arrayType.dimensions());
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    // ========================================================================
    // Basic getter and setter methods
    // ========================================================================

    boolean addUseConstraint(ReferenceType constraint) {
        if (useConstraints == null) {
            useConstraints = Sets.newHybridSet();
        }
        return useConstraints.add(constraint);
    }

    Set<ReferenceType> getUseConstraints() {
        if (useConstraints == null) {
            return Set.of();
        }
        return useConstraints;
    }

    void addInitConstraint(ReferenceType constraint) {
        if (initConstraints == null) {
            initConstraints = new ArrayList<>();
        }
        initConstraints.add(constraint);
    }

    List<ReferenceType> getInitConstraints() {
        if (initConstraints == null) {
            return List.of();
        }
        return initConstraints;
    }

    Var var() {
        return var;
    }

    void addInEdge(TypeFlowEdge edge) {
        if (inEdges == null) {
            inEdges = new ArrayList<>();
        }
        inEdges.add(edge);
    }

    List<TypeFlowEdge> getInEdges() {
        if (inEdges == null) {
            return List.of();
        }
        return inEdges;
    }

    void addOutEdge(TypeFlowEdge edge) {
        if (outEdges == null) {
            outEdges = new ArrayList<>();
        }
        outEdges.add(edge);
    }

    List<TypeFlowEdge> getOutEdges() {
        if (outEdges == null) {
            return List.of();
        }
        return outEdges;
    }
}
