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

import org.junit.jupiter.params.ParameterizedTest;
import pascal.taie.analysis.Tests;
import pascal.taie.util.MultiStringsSource;

public class ContextSensitivityTestFull extends ContextSensitivityTest {

    @ParameterizedTest
    // More complex tests
    @MultiStringsSource({"RecursiveObj", "cs:2-obj"})
    @MultiStringsSource({"LongObjContext", "cs:2-obj"})
    @MultiStringsSource({"LongCallContext", "cs:2-call"})
    @MultiStringsSource({"StaticSelect", "cs:2-obj"})
    @MultiStringsSource({"TwoCallOnly", "cs:2-call"})
    @MultiStringsSource({"ObjOnly", "cs:1-obj"})
    @MultiStringsSource({"MustUseHeap", "cs:2-call"})
    @MultiStringsSource({"NestedHeap", "cs:2-obj"})
    @MultiStringsSource({"CallOnly", "cs:1-call"})
    @MultiStringsSource({"LinkedQueue", "cs:2-obj"})
    // Tests for handling of non-normal objects
    @MultiStringsSource({"TypeSens", "cs:2-type"})
    @MultiStringsSource({"SpecialHeapContext", "cs:2-obj"})
    void testFull(String mainClass, String... opts) {
        Tests.testPTA(DIR, mainClass, opts);
    }

}
