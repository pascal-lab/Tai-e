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

package pascal.taie.android.ubcbench;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pascal.taie.android.AndroidBenchTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UBCBenchTest extends AndroidBenchTest {

    private static final String BENCHMARK_HOME_PREFIX = "android-benchmarks/UBCBench/apk/";

    @ParameterizedTest
    @ValueSource(strings = {
            "ViewCasting",
            "ConservativeModel1",
            "ConservativeModel2",
            "ConservativeModel3",
            "HardCodedLocationTest",
            "CallbacksIntentHandling",
            "SetContentView",
            "CallbacksInFragment",
            "ReflectionOverloaded",
            "ReflectionRes",
            "ReflectionDynamic",
            "GetConstructor",
            "ReturnConstructor",
            "EventOrderingTest",
            "ForName",
            "LocationFieldSensitivity",
            "SendTextMessage",
            "SetGetHint",
            "SharedPreference1",
            "SharedPreference2",
            "SharedPreference3",
            "ContextSensitivity",
            "FieldSensitivity",
            "FlowSensitivity",
            "ObjectSensitivity",
            "PathSensitivity"
    })
    void test(String benchmark) {
        run(BENCHMARK_HOME_PREFIX, benchmark);
    }

}
