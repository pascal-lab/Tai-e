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

package pascal.taie.util.collection;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ArraySetTest extends AbstractSetTest {

    protected <E> Set<E> newSet() {
        return new ArraySet<>();
    }

    @Test
    void testFixedCapacity() {
        assertThrows(TooManyElementsException.class, () ->
                testAddNElements(new ArraySet<>(4), 5));
    }

    @Test
    void testNonFixedCapacity() {
        testAddNElements(new ArraySet<>(4, false), 5);
    }

    @Test
    void testBoundaryAdd() {
        Set<Integer> s = new ArraySet<>(4);
        s.add(1);
        s.add(2);
        s.add(3);
        s.add(4);
        s.add(1);
    }
}
