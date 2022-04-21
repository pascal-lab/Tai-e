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

import org.junit.Assert;
import org.junit.Test;
import pascal.taie.util.AnalysisException;

public class ValueTest {

    @Test
    public void testInt() {
        Value v1 = Value.makeConstant(10);
        Assert.assertTrue(v1.isConstant());
        Assert.assertFalse(v1.isNAC() || v1.isUndef());
        Assert.assertEquals(v1.getConstant(), 10);
        Value v2 = Value.makeConstant(1);
        Value v3 = Value.makeConstant(10);
        Assert.assertNotEquals(v1, v2);
        Assert.assertEquals(v1, v3);
    }

    @Test(expected = AnalysisException.class)
    public void testGetIntOnNAC() {
        Value.getNAC().getConstant();
    }

    @Test(expected = AnalysisException.class)
    public void testGetIntOnUndef() {
        Value.getUndef().getConstant();
    }
}
