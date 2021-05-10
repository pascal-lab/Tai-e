package pascal.taie.analysis.bugfinder.dataflow;

public class IsNullValue {

    private static final IsNullValue NULL = new IsNullValue(Kind.NULL);

    private static final IsNullValue CHECKED_NULL = new IsNullValue(Kind.CHECKED_NULL);

    private static final IsNullValue NN = new IsNullValue(Kind.NN);

    private static final IsNullValue CHECKED_NN = new IsNullValue(Kind.CHECKED_NN);

    private static final IsNullValue NO_KABOOM_NN = new IsNullValue(Kind.NO_KABOOM_NN);

    private static final IsNullValue NSP = new IsNullValue(Kind.NSP);

    private static final IsNullValue UNKNOWN = new IsNullValue(Kind.UNKNOWN);

    private static final IsNullValue NCP = new IsNullValue(Kind.NCP);

    private static final IsNullValue UNDEF = new IsNullValue(Kind.UNDEF);

    private final Kind kind;

    private IsNullValue(Kind kind) {
        this.kind = kind;
    }

    public static IsNullValue nullValue() {
        return NULL;
    }

    public static IsNullValue checkedNullValue() {
        return CHECKED_NULL;
    }

    public static IsNullValue nonNullValue() {
        return NN;
    }

    public static IsNullValue checkedNonNullValue() {
        return CHECKED_NN;
    }

    public static IsNullValue noKaboomNonNullValue(){
        return NO_KABOOM_NN;
    }

    public static IsNullValue nullOnSimplePathValue() {
        return NSP;
    }

    public static IsNullValue nonReportingNotNullValue() {
        return UNKNOWN;
    }

    public static IsNullValue nullOnComplexPathValue() {
        return NCP;
    }

    public static IsNullValue undefValue() { return UNDEF; }

    public boolean isDefinitelyNull() {
        return kind == Kind.CHECKED_NULL || kind == Kind.NULL;
    }

    public boolean isNullOnSomePath() {
        return kind == Kind.NSP;
    }

    public boolean isAKaBoom() {
        return kind == Kind.NO_KABOOM_NN;
    }

    public boolean isNullOnComplicatedPath() {
        return kind == Kind.NCP || kind == Kind.UNKNOWN;
    }

    public boolean mightBeNull() {
        return isDefinitelyNull() || isNullOnSomePath();
    }

    public boolean isDefinitelyNotNull() {
        return kind == Kind.NN || kind == Kind.CHECKED_NN || kind == Kind.NO_KABOOM_NN;
    }

    private static final IsNullValue[][] mergeMatrix = {
        // NULL, CHECKED_NULL, NN, CHECKED_NN, NO_KABOOM_NN, NSP, UNKNOWN, NCP, UNDEF
        {NULL}, // NULL
        {NULL, CHECKED_NULL,}, // CHECKED_NULL
        {NSP, NSP, NN}, // NN
        {NSP, NSP, NN, CHECKED_NN,}, // CHECKED_NN
        {NSP, NSP, NN, NN, NO_KABOOM_NN}, // NO_KABOOM_NN
        {NSP, NSP, NSP, NSP, NSP, NSP}, // NSP
        {NSP, NSP, UNKNOWN, UNKNOWN, UNKNOWN, NSP, UNKNOWN,}, // UNKNOWN
        {NSP, NSP, NCP, NCP, NCP, NSP, NCP, NCP,}, // NCP
        {NULL, CHECKED_NULL, NN, CHECKED_NN, NO_KABOOM_NN, NSP, UNKNOWN, NCP, UNDEF}
    };

    public static IsNullValue merge(IsNullValue a, IsNullValue b) {
        int index1 = a.kind.val;
        int index2 = b.kind.val;

        if (index1 < index2) {
            int tmp = index1;
            index1 = index2;
            index2 = tmp;
        }

        return mergeMatrix[index1][index2];
    }

    @Override
    public String toString() {
        return kind.toString();
    }

    private enum Kind {
        NULL(0),
        CHECKED_NULL(1),
        NN(2),
        CHECKED_NN(3),
        NO_KABOOM_NN(4),
        NSP(5),
        UNKNOWN(6),
        NCP(7),
        UNDEF(8);

        public final int val;

        Kind(int v) {
            val = v;
        }
    }
}
