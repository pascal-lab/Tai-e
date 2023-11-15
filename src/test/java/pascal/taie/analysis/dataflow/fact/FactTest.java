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

package pascal.taie.analysis.dataflow.fact;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FactTest {

    @Test
    void testUnionNormal() {
        // Union overlapped sets
        SetFact<String> f1 = newSetFact("x", "y");
        SetFact<String> f2 = newSetFact("y", "z");
        SetFact<String> f3 = f1.unionWith(f2);
        assertEquals(f3.size(), 3);

        // Union two disjoint sets
        f1 = newSetFact("a", "b");
        f2 = newSetFact("c", "d");
        f3 = f1.unionWith(f2);
        assertEquals(f3.size(), 4);

        // Union empty set
        f1 = newSetFact();
        f2 = newSetFact("xxx", "yyy");
        f3 = f1.unionWith(f2);
        assertEquals(f3.size(), 2);
    }

    @Test
    void testIntersectNormal() {
        // Intersect overlapped sets
        SetFact<String> f1 = newSetFact("x", "y");
        SetFact<String> f2 = newSetFact("y", "z");
        SetFact<String> f3 = f1.intersectWith(f2);
        assertEquals(f3.size(), 1);

        // Intersect two disjoint sets
        f1 = newSetFact("a", "b");
        f2 = newSetFact("c", "d");
        f3 = f1.intersectWith(f2);
        assertEquals(f3.size(), 0);
        assertTrue(f3.isEmpty());

        // Intersect empty set
        f1 = newSetFact();
        f2 = newSetFact("xxx", "yyy");
        f3 = f1.intersectWith(f2);
        assertEquals(f3.size(), 0);
        assertTrue(f3.isEmpty());
    }

    @SafeVarargs
    private static <T> SetFact<T> newSetFact(T... args) {
        return new SetFact<>(Arrays.asList(args));
    }
}
