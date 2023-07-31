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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ContextTest {

    @Test
    void testTreeContext() {
        testContext(new TrieContext.Factory<>());
    }

    @Test
    void testTreeContext2() {
        TrieContext.Factory<String> factory = new TrieContext.Factory<>();
        Context abc = factory.make("A", "B", "C");
        TrieContext bcd = factory.append(abc, "D", 3);
        Context bc = factory.make("B", "C");
        assertEquals(bc, bcd.getParent());
        TrieContext cde = factory.append(bcd, "E", 3);
        Context cd = factory.make("C", "D");
        assertEquals(cde.getParent(), cd);
    }

    private static void testContext(ContextFactory<String> factory) {
        Context a = factory.make("A");
        Context empty1 = factory.getEmptyContext();
        Context empty2 = factory.makeLastK(a, 0);
        assertEquals(empty1, empty2);

        Context ab1 = factory.append(a, "B", 2);
        Context ab2 = factory.append(a, "B", 3);
        assertEquals(ab1, ab2);

        Context b1 = factory.make("B");
        Context b2 = factory.append(a, "B", 1);
        assertEquals(b1, b2);

        Context pqrstuvw = factory.make("P", "Q", "R", "S", "T", "U", "V", "W");
        Context uvw1 = factory.makeLastK(pqrstuvw, 3);
        Context uvw2 = factory.make("U", "V", "W");
        assertEquals(uvw1, uvw2);
    }
}
