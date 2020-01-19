package sa.dataflow.lattice;

import sa.util.AnalysisException;
import sa.util.Canonicalizer;

import java.util.Objects;

/**
 * Possible values for constant propagation.
 * If the value is a constant, it can be an integer or a boolean.
 */
public class CPValue {

    private enum Kind {
        NAC, // not a constant
        INT, // an integer
        BOOLEAN, // a boolean
        UNDEF, // undefined value
    }

    private static final Canonicalizer<CPValue> canonicalizer = new Canonicalizer<>();

    private static boolean isCanonicalizing = false;

    private static final CPValue NAC = canonicalize(new CPValue(Kind.NAC));

    private static final CPValue UNDEF = canonicalize(new CPValue(Kind.UNDEF));

    private Kind kind;

    private int intValue;

    private boolean boolValue;

    private int hashCode;

    private CPValue(Kind kind) {
        this(kind, 0, false);
    }

    private CPValue(Kind kind, int intValue, boolean boolValue) {
        this.kind = kind;
        this.intValue = intValue;
        this.boolValue = boolValue;
        this.hashCode = Objects.hash(kind, intValue, boolValue);
    }

    public boolean isNAC() {
        return kind == Kind.NAC;
    }

    public boolean isInt() {
        return kind == Kind.INT;
    }

    public boolean isBool() {
        return kind == Kind.BOOLEAN;
    }

    public boolean isConstant() {
        return isInt() || isBool();
    }

    public boolean isUndef() {
        return kind == Kind.UNDEF;
    }

    public int getInt() {
        if (!isInt()) {
            throw new AnalysisException(this + " is not an integer");
        }
        return intValue;
    }

    public boolean getBool() {
        if (!isBool()) {
            throw new AnalysisException(this + " is not a boolean");
        }
        return boolValue;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (!isCanonicalizing) {
            return this == obj;
        } else if (this == obj) {
            return true;
        } else if (!(obj instanceof CPValue)) {
            return false;
        }
        CPValue other = (CPValue) obj;
        return kind == other.kind
                && intValue == other.intValue
                && boolValue == other.boolValue;
    }

    @Override
    public String toString() {
        switch (kind) {
            case NAC:
                return "NAC";
            case INT:
                return Integer.toString(intValue);
            case BOOLEAN:
                return Boolean.toString(boolValue);
            case UNDEF:
                return "UNDEF";
            default:
                throw new IllegalStateException("Unexpected value: " + kind);
        }
    }

    public static CPValue getNAC() {
        return NAC;
    }

    public static CPValue makeInt(int v) {
        return canonicalize(new CPValue(Kind.INT, v, false));
    }

    public static CPValue makeBool(boolean v) {
        return canonicalize(new CPValue(Kind.BOOLEAN, 0, v));
    }

    public static CPValue getUndef() {
        return UNDEF;
    }

    private static CPValue canonicalize(CPValue v) {
        isCanonicalizing = true;
        CPValue cv = canonicalizer.canonicalize(v);
        isCanonicalizing = false;
        return cv;
    }

}
