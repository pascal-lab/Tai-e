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

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Stream;

public class CollectionUtilsTest {

    @Test
    public void testReverseStream() {
        List<Integer> list = List.of(78, 23, 111, 666);
        Stream<Integer> stream = list.stream();
        Int i = new Int(list.size() - 1);
        CollectionUtils.reverse(stream).forEach(n -> {
            Assert.assertEquals(n, list.get(i.value));
            --i.value;
        });
    }

    private static class Int {

        private int value;

        public Int(int value) {
            this.value = value;
        }
    }
}
