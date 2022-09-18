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

package pascal.taie.analysis.graph.callgraph.cha;

import org.junit.Test;
import pascal.taie.analysis.Tests;

public class CHATest {

    protected static void test(String main) {
        Tests.testMain(main, "src/test/resources/cha/", "cg", "algorithm:cha");
    }

    @Test
    public void testStaticCall() {
        test("StaticCall");
    }

    @Test
    public void testVirtualCall() {
        test("VirtualCall");
    }

    @Test
    public void testInterface() {
        test("Interface");
    }

    @Test
    public void testAbstractMethod() {
        test("AbstractMethod");
    }
}
