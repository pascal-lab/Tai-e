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

public class BasicTestFull extends BasicTest {

    /**
     * Tests for handling of more Java features
     */
    @ParameterizedTest
    @MultiStringsSource("StaticField")
    @MultiStringsSource("Array")
    @MultiStringsSource("Cast")
    @MultiStringsSource("Cast2")
    @MultiStringsSource("Null")
    @MultiStringsSource("Primitive")
    @MultiStringsSource({"Primitives", "propagate-types:[reference,int,double]",
            "plugins:[pascal.taie.analysis.pta.plugin.NumberLiteralHandler]"})
    @MultiStringsSource({"PropagateNull", "propagate-types:[reference,null]",
            "plugins:[pascal.taie.analysis.pta.plugin.NullHandler]"})
    @MultiStringsSource({"Strings", "distinguish-string-constants:all"})
    @MultiStringsSource("MultiArray")
    @MultiStringsSource("Clinit")
    @MultiStringsSource("ClassObj")
    @MultiStringsSource("Native")
    @MultiStringsSource({"NativeModel", "distinguish-string-constants:all"})
    @MultiStringsSource({"Annotations", "cs:1-call",
            "distinguish-string-constants:all"})
    void testFull(String mainClass, String... opts) {
        Tests.testPTA(DIR, mainClass, opts);
    }

}
