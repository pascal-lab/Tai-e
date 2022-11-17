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

package pascal.taie.analysis.pta;

import org.junit.Test;
import pascal.taie.analysis.Tests;

public class LambdaTest {

    private static final String DIR = "lambda";

    private static final String ARG = "handle-invokedynamic:true";

    @Test
    public void testArgs() {
        Tests.testPTA(DIR, "Args", ARG);
    }

    @Test
    public void testConstructor() {
        Tests.testPTA(DIR, "LambdaConstructor", ARG);
    }

    @Test
    public void testInstanceMethod() {
        Tests.testPTA(DIR, "LambdaInstanceMethod", ARG);
    }

    @Test
    public void testStaticMethod() {
        Tests.testPTA(DIR, "LambdaStaticMethod", ARG);
    }

    @Test
    public void testImpreciseLambdas() {
        Tests.testPTA(DIR, "ImpreciseLambdas", ARG);
    }

    @Test
    public void testDispatchBugDueToLackOfSubclassCheck() {
        Tests.testPTA(DIR, "DispatchBugDueToLackOfSubclassCheck", ARG);
    }

    @Test
    public void testNativeModelWithLambda() {
        Tests.testPTA(DIR, "NativeModelWithLambda", ARG);
    }
}
