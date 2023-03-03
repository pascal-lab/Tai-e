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

public class BasicTestFull extends BasicTest {

    // Tests for handling of more Java features
    @Test
    public void testStaticField() {
        Tests.testPTA(DIR, "StaticField");
    }

    @Test
    public void testArray() {
        Tests.testPTA(DIR, "Array");
    }

    @Test
    public void testCast() {
        Tests.testPTA(DIR, "Cast");
    }

    @Test
    public void testCast2() {
        Tests.testPTA(DIR, "Cast2");
    }

    @Test
    public void testNull() {
        Tests.testPTA(DIR, "Null");
    }

    @Test
    public void testPrimitive() {
        Tests.testPTA(DIR, "Primitive");
    }

    @Test
    public void testPrimitives() {
        Tests.testPTA(DIR, "Primitives",
                "propagate-types:[reference,int,double];" +
                        "plugins:[pascal.taie.analysis.pta.plugin.NumberLiteralHandler]");
    }

    @Test
    public void testStrings() {
        Tests.testPTA(DIR, "Strings", "distinguish-string-constants:all");
    }

    @Test
    public void testMultiArray() {
        Tests.testPTA(DIR, "MultiArray");
    }

    @Test
    public void testClinit() {
        Tests.testPTA(DIR, "Clinit");
    }

    @Test
    public void testClassObj() {
        Tests.testPTA(DIR, "ClassObj");
    }

    @Test
    public void testNative() {
        Tests.testPTA(DIR, "Native");
    }

    @Test
    public void testNativeModel() {
        Tests.testPTA(DIR, "NativeModel");
    }
}
