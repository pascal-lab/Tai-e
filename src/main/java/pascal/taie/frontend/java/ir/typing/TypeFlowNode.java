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
    Set<ReferenceType> types;
    @Nullable
    PrimitiveType primitiveType;

    @Nullable
    ReferenceType referenceType;

    Set<ReferenceType> useValidConstrains;

    List<ReferenceType> initConstraints;

    List<TypeFlowEdge> inEdges;

    List<TypeFlowEdge> outEdges;

    TypeFlowNode(FrontendTypeSystem typeSystem, Var var) {
        this.typeSystem = typeSystem;
        this.var = var;
        useValidConstrains = null;
        inEdges = null;
        outEdges = null;
    }

    public void setNewType(Type t) {
        if (t instanceof PrimitiveType pType) {
            onNewPrimitiveType(pType);
        } else if (t instanceof ReferenceType rType) {
            addReferenceType(rType);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * @param t new incoming primitive type
     * @return if type changes
     */
    public boolean onNewPrimitiveType(PrimitiveType t) {
        // TODO: perform numeric promotion
        assert types == null;
        assert t != null;
        if (this.primitiveType == null) {
            this.primitiveType = t;
            return true;
        } else {
            if (this.primitiveType == t) {
                return false;
            } else if (TypeSystem.canHoldInt(primitiveType)
                    && TypeSystem.canHoldInt(t)) {
                return false;
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    public void addReferenceType(ReferenceType t) {
        assert primitiveType == null;
        if (types == null) {
            types = Sets.newHybridSet();
        }
        types.add(t);
    }

    public boolean onNewReferenceType(EdgeKind kind, ReferenceType t) {
        if (kind == EdgeKind.VAR_ARRAY
                && t instanceof ArrayType arrayType
                && TypeSystem.canHoldInt(arrayType.baseType())) {
            // example for that:
            // 1. a = new int[10]
            // 2. a[1] = 1
            // the right `ByteLiteral(1)` MUST NOT infer `a` to be `byte[]`
            // i.e. we only trust the line 1., not line 2.
            // and there must be something like line 1.,
            // or the classfile will not pass the verification
            return false;
        }

        if (referenceType == null) {
            referenceType = t;
            return true;
        } else {
            ReferenceType temp = referenceType;
            referenceType = getNextType(referenceType, t);
            // TODO: ==?
            return temp != referenceType;
        }
    }

    public void firstResolve() {
        assert types != null;
        for (ReferenceType r : types) {
            onNewReferenceType(null, r);
        }
    }

    boolean isUseValid(ReferenceType t) {
        if (useValidConstrains == null) {
            return true;
        }
        boolean ret = true;
        for (ReferenceType c : useValidConstrains) {
            ret &= typeSystem.isAssignable(c, t);
        }
        return ret;
    }

    private ReferenceType getNextType(ReferenceType current, ReferenceType t) {
        if (current == t) {
            return t;
        }
        Set<ReferenceType> lcas = typeSystem.lca(current, t);
        ReferenceType target = null;
        for (ReferenceType lca : lcas) {
            if (isUseValid(lca)) {
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
            ReferenceType t1 = lcas.iterator().next();
            if (t1 instanceof ClassType) {
                return typeSystem.objectType();
            } else if (t1 instanceof ArrayType arrayType) {
                return typeSystem.getArrayType(typeSystem.objectType(), arrayType.dimensions());
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    public void addNewOutEdge(TypeFlowEdge edge) {
        if (outEdges == null) {
            outEdges = new ArrayList<>();
        }
        outEdges.add(edge);
    }

    public void addNewInEdge(TypeFlowEdge edge) {
        if (inEdges == null) {
            inEdges = new ArrayList<>();
        }
        inEdges.add(edge);
    }

    public boolean addNewUseConstrain(ReferenceType type) {
        if (useValidConstrains == null) {
            useValidConstrains = Sets.newHybridSet();
        }
        return useValidConstrains.add(type);
    }

    public void addInitConstraint(ReferenceType type) {
        if (initConstraints == null) {
            initConstraints = new ArrayList<>();
        }
        initConstraints.add(type);
    }

    public Var var() {
        return var;
    }
}
