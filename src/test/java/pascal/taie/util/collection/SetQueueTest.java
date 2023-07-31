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

import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class SetQueueTest {

    @Test
    void test() {
        Queue<Integer> queue = new SetQueue<>();
        queue.addAll(List.of(1, 2, 1, 1, 2, 3, 4));
        assertEquals(4, queue.size());
        assertEquals(1, (int) queue.poll());
        assertEquals(2, (int) queue.poll());
        assertEquals(3, (int) queue.poll());
        assertEquals(4, (int) queue.poll());
    }

    @Test
    void testSerializable() {
        Queue<Integer> queue1 = new SetQueue<>();
        queue1.addAll(List.of(1, 2, 1, 1, 2, 3, 4));
        Queue<Integer> queue2 = SerializationUtils.serializedCopy(queue1);
        assertEquals(queue1.size(), queue2.size());
        while (!queue1.isEmpty()) {
            assertEquals(queue1.poll(), queue2.poll());
        }
    }
}
