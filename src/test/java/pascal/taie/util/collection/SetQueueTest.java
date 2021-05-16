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
import java.util.Queue;

public class SetQueueTest {

    @Test
    public void test() {
        Queue<Integer> queue = new SetQueue<>();
        queue.addAll(List.of(1, 2, 1, 1, 2, 3, 4));
        Assert.assertEquals(4, queue.size());
        Assert.assertEquals(1, (int) queue.poll());
        Assert.assertEquals(2, (int) queue.poll());
        Assert.assertEquals(3, (int) queue.poll());
        Assert.assertEquals(4, (int) queue.poll());
    }
}
