package sa.dataflow.lattice;

/**
 * Possible values for constant propagation.
 * A constant value can be an integer or a boolean.
 */
public class CPValue {

    enum Kind {
        NAC, // not a constant
        INT, // an integer
        BOOLEAN, // a boolean
        UNDEF, // undefined value
    }

    private Kind kind;

    private int intValue;

    private boolean boolValue;

}
