package pascal.dataflow.analysis.constprop;

import pascal.util.AnalysisException;

import java.util.Objects;

/**
 * Possible values for constant propagation.
 * If the value is a constant, it can be an integer or a boolean.
 */
public class Value {

    private enum Kind {
        NAC, // not a constant
        CONSTANT, // an integer constant
        UNDEF, // undefined value
    }

    private static final Value NAC = new Value(Kind.NAC);

    private static final Value UNDEF = new Value(Kind.UNDEF);

    private Kind kind;

    private int value;

    private Value(Kind kind) {
        this(kind, 0);
    }

    private Value(Kind kind, int value) {
        this.kind = kind;
        this.value = value;
    }

    /**
     * Returns if this value is NAC.
     */
    public boolean isNAC() {
        return kind == Kind.NAC;
    }

    /**
     * Returns if this value represents a constant.
     */
    public boolean isConstant() {
        return kind == Kind.CONSTANT;
    }

    /**
     * Returns if this value is UNDEF.
     */
    public boolean isUndef() {
        return kind == Kind.UNDEF;
    }

    /**
     * If this value represents an integer constant, then returns the integer,
     * otherwise throws an exception.
     */
    public int getConstant() {
        if (!isConstant()) {
            throw new AnalysisException(this + " is not a constant");
        }
        return value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof Value)) {
            return false;
        }
        Value other = (Value) obj;
        return kind == other.kind
                && value == other.value;
    }

    @Override
    public String toString() {
        switch (kind) {
            case NAC:
                return "NAC";
            case CONSTANT:
                return Integer.toString(value);
            case UNDEF:
                return "UNDEF";
            default:
                throw new IllegalStateException("Unexpected value: " + kind);
        }
    }

    /**
     * Returns the NAC.
     */
    public static Value getNAC() {
        return NAC;
    }

    /**
     * Makes a constant value.
     */
    public static Value makeConstant(int v) {
        return new Value(Kind.CONSTANT, v);
    }

    /**
     * Returns the UNDEF.
     */
    public static Value getUndef() {
        return UNDEF;
    }
}
