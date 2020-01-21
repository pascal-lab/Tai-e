package sa.dataflow.cp;

import sa.util.AnalysisException;
import sa.util.Canonicalizer;

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

    public static Value getNAC() {
        return NAC;
    }

    public static Value makeInt(int v) {
        return new Value(Kind.INT, v, false);
    }

    public static Value makeBool(boolean v) {
        return v ? TRUE : FALSE;
    }

    public static Value getUndef() {
        return UNDEF;
    }

}
