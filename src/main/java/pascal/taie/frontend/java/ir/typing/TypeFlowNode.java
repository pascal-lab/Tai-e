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

final class TypeFlowNode {
    private final FrontendTypeSystem typeSystem;
    private final Var var;
    @Nullable
    Set<ReferenceType> candidateRefTypes;
    @Nullable
    PrimitiveType primitiveType;

    @Nullable
    ReferenceType referenceType;

    Set<ReferenceType> useConstraints;

    List<ReferenceType> initConstraints;

    List<TypeFlowEdge> inEdges;

    List<TypeFlowEdge> outEdges;

    TypeFlowNode(FrontendTypeSystem typeSystem, Var var) {
        this.typeSystem = typeSystem;
        this.var = var;
        this.useConstraints = null;
        this.inEdges = null;
        this.outEdges = null;
    }

    void addType(Type type) {
        if (type instanceof PrimitiveType pType) {
            addPrimitiveType(pType);
        } else if (type instanceof ReferenceType rType) {
            addReferenceType(rType);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * @return if type changes
     */
    boolean addPrimitiveType(PrimitiveType type) {
        // TODO: perform numeric promotion
        assert candidateRefTypes == null;
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

    private void addReferenceType(ReferenceType t) {
        assert primitiveType == null;
        if (candidateRefTypes == null) {
            candidateRefTypes = Sets.newHybridSet();
        }
        candidateRefTypes.add(t);
    }

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

    void computeReferenceType() {
        assert candidateRefTypes != null;
        for (ReferenceType r : candidateRefTypes) {
            updateReferenceType(r);
        }
    }

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

    private ReferenceType getSuitableLCA(ReferenceType current, ReferenceType t) {
        if (current == t) {
            return t;
        }
        Set<ReferenceType> lcas = typeSystem.lca(current, t);
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

    void addOutEdge(TypeFlowEdge edge) {
        if (outEdges == null) {
            outEdges = new ArrayList<>();
        }
        outEdges.add(edge);
    }

    void addInEdge(TypeFlowEdge edge) {
        if (inEdges == null) {
            inEdges = new ArrayList<>();
        }
        inEdges.add(edge);
    }

    boolean addUseConstraint(ReferenceType type) {
        if (useConstraints == null) {
            useConstraints = Sets.newHybridSet();
        }
        return useConstraints.add(type);
    }

    void addInitConstraint(ReferenceType type) {
        if (initConstraints == null) {
            initConstraints = new ArrayList<>();
        }
        initConstraints.add(type);
    }

    Var var() {
        return var;
    }
}
