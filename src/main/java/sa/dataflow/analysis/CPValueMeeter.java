package sa.dataflow.analysis;

import sa.dataflow.analysis.Meeter;
import sa.dataflow.lattice.CPValue;

public class CPValueMeeter implements Meeter<CPValue> {

    @Override
    public CPValue meet(CPValue v1, CPValue v2) {
        if (v1.isUndef() && v2.isConstant()) {
            return v2;
        } else if (v1.isConstant() && v2.isUndef()) {
            return v1;
        } else if (v1.isNAC() || v2.isNAC()) {
            return CPValue.getNAC();
        } else if (v1.equals(v2)) {
            return v1;
        } else {
            return CPValue.getNAC();
        }
    }
}
