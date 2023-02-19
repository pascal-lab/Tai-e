/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.graph.cfg;

class SwitchCaseEdge<N> extends CFGEdge<N> {

    private final int caseValue;

    SwitchCaseEdge(N source, N target, int caseValue) {
        super(Kind.SWITCH_CASE, source, target);
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
        return super.toString() + " [" + caseValue + "]";
    }
}
