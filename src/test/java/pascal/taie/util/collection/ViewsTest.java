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

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

public class ViewsTest {

    @Test
    public void testFilteredCollection() {
        Collection<Integer> view = Views.toFilteredCollection(
                List.of(1, 2, 3, 4, 5, 6, 7, 8, 9), n -> n % 2 == 0);
        IntStream.of(1, 3, 5, 7, 9).forEach(
                n -> Assert.assertFalse(view.contains(n)));
        Assert.assertEquals(4, view.size());
    }
}
