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

package pascal.taie.util.collection;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

public class GenericBitSetTest {

    private static final Object CONTEXT = new Object();

    private static class StringSet extends GenericBitSet<String> {

        @Override
        public GenericBitSet<String> copy() {
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
    public void testStream() {
        StringSet ss = new StringSet();
        assertEquals("[]", CollectionUtils.toString(ss));
        ss.add("123");
        assertEquals("[123]", CollectionUtils.toString(ss));
    }

    @Test
    public void testRemoveIf() {
        StringSet ss = new StringSet();
        ss.addAll(Set.of("1", "22", "333", "4444", "4446", "4448"));
        ss.removeIf(s -> Integer.parseInt(s) % 2 == 0);
        assertEquals("[1, 333]", CollectionUtils.toString(ss));
    }
}
