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

package pascal.taie.android.droidbench;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class GeneralJavaTest extends AndroidBenchmarkTest {

    static final String TYPE = "GeneralJava";

    /**
     * Tests for GeneralJava Type apk
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "Clone1",
            "Loop1",
            "Serialization1",
            "Loop2",
            "Exceptions1",
            "Exceptions2",
            "Exceptions3",
            "Exceptions4",
            "Exceptions5",
            "Exceptions6",
            "Exceptions7",
            "FactoryMethods1",
            "SourceCodeSpecific1",
            "StartProcessWithSecret1",
            "StaticInitialization1",
            "StaticInitialization2",
            "StaticInitialization3",
            "StringFormatter1",
            "StringPatternMatching1",
            "StringToCharArray1",
            "StringToOutputStream1",
            "UnreachableCode",
            "VirtualDispatch1",
            "VirtualDispatch2",
            "VirtualDispatch3",
            "VirtualDispatch4"
    })
    void test(String benchmark) {
        run(TYPE, benchmark);
    }

}
