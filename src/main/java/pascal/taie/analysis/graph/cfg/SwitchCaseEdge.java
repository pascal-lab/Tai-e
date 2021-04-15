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

package pascal.taie.analysis.graph.cfg;

class SwitchCaseEdge<N> extends Edge<N> {

    private final int caseValue;

    SwitchCaseEdge(Kind kind, N source, N target, int caseValue) {
        super(kind, source, target);
        this.caseValue = caseValue;
    }

    @Override
    public int getCaseValue() {
        return caseValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        SwitchCaseEdge<?> that = (SwitchCaseEdge<?>) o;
        return caseValue == that.caseValue;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + caseValue;
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + " with case " + caseValue;
    }
}
