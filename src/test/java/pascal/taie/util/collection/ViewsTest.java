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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ViewsTest {

    @Test
    public void testFilteredCollection() {
        Collection<Integer> view = Views.toFilteredCollection(
                List.of(1, 2, 3, 4, 5, 6, 7, 8, 9), n -> n % 2 == 0);
        IntStream.of(1, 3, 5, 7, 9).forEach(
                n -> assertFalse(view.contains(n)));
        assertEquals(4, view.size());
    }

    @Test
    public void testCombinedSet() {
        Set<Integer> view = Views.toCombinedSet(
                Set.of(1, 3, 5, 7, 9), Set.of(2, 4, 6, 8, 10));
        IntStream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).forEach(
                n -> assertTrue(view.contains(n)));
        assertEquals(10, view.size());
        int total = 0;
        for (int n : view) {
            total += n;
        }
        assertEquals(55, total);
    }
}
