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

package pascal.taie.analysis.pta.core.cs.context;

import org.junit.Assert;
import org.junit.Test;

public class ContextTest {

    @Test
    public void testLinkedContext() {
        TreeContext.Factory<String> factory = new TreeContext.Factory<>();
        Context a = factory.make("A");
        Context ab1 = factory.append(a, "B", 2);
        Context ab2 = factory.append(a, "B", 3);
        Assert.assertEquals(ab1, ab2);
        Context b1 = factory.make("B");
        Context b2 = factory.append(a, "B", 1);
        Assert.assertEquals(b1, b2);
        Context bc = factory.append(ab1, "C", 2);
        Context abc = factory.append(ab1, "C", 3);
        TreeContext<String> bcd = factory.append(abc, "D", 3);
        Assert.assertEquals(bc, bcd.getParent());
        TreeContext<String> cde = factory.append(bcd, "E", 3);
        Context cd = factory.make("C", "D");
        Assert.assertEquals(cde.getParent(), cd);
    }
}
