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

package pascal.taie.analysis.deadcode;

import org.junit.Test;

public class DeadCodeTestFull extends DeadCodeTest {

    @Test
    public void testControlFlowUnreachable2() {
        testDCD("ControlFlowUnreachable2");
    }

    @Test
    public void testUnreachableIfBranch2() {
        testDCD("UnreachableIfBranch2");
    }

    @Test
    public void testUnreachableSwitchBranch2() {
        testDCD("UnreachableSwitchBranch2");
    }

    @Test
    public void testDeadAssignment2() {
        testDCD("DeadAssignment2");
    }

    @Test
    public void testLiveAssignments() {
        testDCD("LiveAssignments");
    }

    @Test
    public void testMixedDeadCode() {
        testDCD("MixedDeadCode");
    }

    @Test
    public void testNotDead() {
        testDCD("NotDead");
    }

    @Test
    public void testCorner() {
        testDCD("Corner");
    }

    @Test
    public void testAllReachableIfBranch() {
        testDCD("AllReachableIfBranch");
    }

    @Test
    public void testForLoops() {
        testDCD("ForLoops");
    }

    @Test
    public void testArrayField() {
        testDCD("ArrayField");
    }
}
