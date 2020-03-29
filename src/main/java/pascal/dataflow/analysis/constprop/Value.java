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
        INT, // an integer constant
        BOOLEAN, // a boolean constant
        UNDEF, // undefined value
    }

    private static final Value NAC = new Value(Kind.NAC);

    private static final Value UNDEF = new Value(Kind.UNDEF);

    private static final Value TRUE = new Value(Kind.BOOLEAN, 0, true);

    private static final Value FALSE = new Value(Kind.BOOLEAN, 0, false);

    private Kind kind;

    private int intValue;

    private boolean boolValue;

    private int hashCode;

    private Value(Kind kind) {
        this(kind, 0, false);
    }

    private Value(Kind kind, int intValue, boolean boolValue) {
        this.kind = kind;
        this.intValue = intValue;
        this.boolValue = boolValue;
        this.hashCode = Objects.hash(kind, intValue, boolValue);
    }

    /**
     * Returns if this value is NAC.
     */
    public boolean isNAC() {
        return kind == Kind.NAC;
    }

    /**
     * Returns if this value represents an integer.
     */
    public boolean isInt() {
        return kind == Kind.INT;
    }

    /**
     * Returns if this value represents a boolean.
     */
    public boolean isBool() {
        return kind == Kind.BOOLEAN;
    }

    /**
     * Returns if this value represents a constant.
     */
    public boolean isConstant() {
        return isInt() || isBool();
    }

    /**
     * Returns if this value is UNDEF.
     */
    public boolean isUndef() {
        return kind == Kind.UNDEF;
    }

    /**
     * If this value represents an integer, then returns the integer,
     * otherwise throws an exception.
     */
    public int getInt() {
        if (!isInt()) {
            throw new AnalysisException(this + " is not an integer");
        }
        return intValue;
    }

    /**
     * If this value represents an boolean, then returns the boolean,
     * otherwise throws an exception.
     */
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
        if (this == obj) {
            return true;
        } else if (!(obj instanceof Value)) {
            return false;
        }
        Value other = (Value) obj;
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

    /**
     * Returns the NAC.
     */
    public static Value getNAC() {
        return NAC;
    }

    /**
     * Makes a integer value.
     */
    public static Value makeInt(int v) {
        return new Value(Kind.INT, v, false);
    }

    /**
     * Makes a boolean value.
     */
    public static Value makeBool(boolean v) {
        return v ? TRUE : FALSE;
    }

    /**
     * Returns the UNDEF.
     */
    public static Value getUndef() {
        return UNDEF;
    }

}
