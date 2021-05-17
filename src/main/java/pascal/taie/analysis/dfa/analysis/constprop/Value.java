/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.dfa.analysis.constprop;

import pascal.taie.util.AnalysisException;

/**
 * Possible values for constant propagation.
 * A value can be either NAC, or a constant, or UNDEF.
 */
public class Value {

    private static final Value NAC = new Value(Kind.NAC);

    private static final Value UNDEF = new Value(Kind.UNDEF);

    /**
     * Cache frequently used values for saving space.
     */
    private static final Value[] cache = new Value[-(-128) + 127 + 1];

    static {
        for(int i = 0; i < cache.length; i++)
            cache[i] = new Value(Kind.CONSTANT, i - 128);
    }

    private final Kind kind;

    private final int value;

    private Value(Kind kind) {
        this(kind, 0);
    }

    private Value(Kind kind, int value) {
        this.kind = kind;
        this.value = value;
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
    public static Value makeConstant(int value) {
        final int offset = 128;
        if (value >= -128 && value <= 127) { // will cache
            return cache[value + offset];
        }
        return new Value(Kind.CONSTANT, value);
    }

    /**
     * Returns the UNDEF.
     */
    public static Value getUndef() {
        return UNDEF;
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
     * If this value represents a (integer) constant, then returns the integer,
     * otherwise throws an exception.
     */
    public int getConstant() {
        if (!isConstant()) {
            throw new AnalysisException(this + " is not a constant");
        }
        return value;
    }

    Value restrictTo(Value other) {
        if (Kind.isHigher(kind, other.kind)) {
            return other;
        } else if (Kind.isHigher(other.kind, kind)) {
            return this;
        } else if (isConstant()) {
            return value == other.value ? this : UNDEF;
        } else {
            return this;
        }
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

    private enum Kind {
        NAC, // not a constant
        CONSTANT, // an integer constant
        UNDEF, // undefined value
        ;

        /**
         * @return if k1 has higher position than k2 in the lattice.
         */
        private static boolean isHigher(Kind k1, Kind k2) {
            return k1.ordinal() < k2.ordinal();
        }
    }
}
