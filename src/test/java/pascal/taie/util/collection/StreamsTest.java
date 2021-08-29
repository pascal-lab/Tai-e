/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.util.collection;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class StreamsTest {

    @Test
    public void testConcat() {
        var s1 = Stream.of(1, 2);
        var s2 = Stream.of(3, 4);
        var s3 = Stream.of(5, 6, 7);
        List<Integer> list = new ArrayList<>();
        Streams.concat(s1, s2, s3).forEach(list::add);
        Assert.assertEquals(list, List.of(1, 2, 3, 4, 5, 6, 7));
    }
}
