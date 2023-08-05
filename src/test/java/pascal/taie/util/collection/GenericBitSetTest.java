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
import pascal.taie.util.SerializationUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GenericBitSetTest {

    private static final Object CONTEXT = new Object();

    private static class StringSet extends GenericBitSet<String> {

        private StringSet() {
            super(false);
        }

        @Override
        protected GenericBitSet<String> newSet() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Object getContext() {
            return CONTEXT;
        }

        @Override
        protected int getIndex(String s) throws IllegalArgumentException {
            return Integer.parseInt(s);
        }

        @Override
        protected String getElement(int index) throws IllegalArgumentException {
            return String.valueOf(index);
        }
    }

    @Test
    void testStream() {
        StringSet ss = new StringSet();
        assertEquals("[]", CollectionUtils.toString(ss));
        ss.add("123");
        assertEquals("[123]", CollectionUtils.toString(ss));
    }

    @Test
    void testRemoveIf() {
        StringSet ss = new StringSet();
        ss.addAll(Set.of("1", "22", "333", "4444", "4446", "4448"));
        ss.removeIf(s -> Integer.parseInt(s) % 2 == 0);
        assertEquals("[1, 333]", CollectionUtils.toString(ss));
    }

    @Test
    void testSerializable() {
        StringSet ss1 = new StringSet();
        ss1.addAll(Set.of("1", "22", "333", "4444", "4446", "4448"));
        StringSet ss2 = SerializationUtils.serializedCopy(ss1);
        assertEquals(ss1, ss2);
        ss1.add("9999");
        ss2.add("9999");
        assertEquals(ss1, ss2);
    }
}
