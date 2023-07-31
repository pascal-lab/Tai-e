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

package pascal.taie.analysis.dataflow.analysis.constprop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class MeetValueTest {

    private final Value i1 = Value.makeConstant(1);
    private final Value i0 = Value.makeConstant(0);
    private final Value NAC = Value.getNAC();
    private final Value undef = Value.getUndef();
    private final ConstantPropagation.Analysis cp =
            new ConstantPropagation.Analysis(null, true);

    @Test
    void testMeet() {
        assertEquals(cp.meetValue(undef, undef), undef);
        assertEquals(cp.meetValue(undef, i0), i0);
        assertEquals(cp.meetValue(undef, NAC), NAC);
        assertEquals(cp.meetValue(NAC, NAC), NAC);
        assertEquals(cp.meetValue(NAC, i0), NAC);
        assertEquals(cp.meetValue(NAC, undef), NAC);
        assertEquals(cp.meetValue(i0, i0), i0);
        assertEquals(cp.meetValue(i0, i1), NAC);
        assertEquals(cp.meetValue(i0, undef), i0);
        assertEquals(cp.meetValue(i0, NAC), NAC);
    }
}
