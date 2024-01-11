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

public class TaintTest {

    static final String DIR = "taint";

    static final String TAINT_CONFIG_PREFIX = "taint-config:src/test/resources/pta/taint/";

    static final String TAINT_CONFIG = TAINT_CONFIG_PREFIX + "taint-config.yml";

    @ParameterizedTest
    @MultiStringsSource({"ArrayTaint", TAINT_CONFIG})
    @MultiStringsSource({"CharArray", TAINT_CONFIG})
    @MultiStringsSource({"FieldTaint", TAINT_CONFIG})
    @MultiStringsSource({"LinkedQueue", TAINT_CONFIG})
    @MultiStringsSource({"CSTaint", "cs:1-obj", TAINT_CONFIG})
    @MultiStringsSource({"TwoObjectTaint", "cs:2-obj", TAINT_CONFIG})
    @MultiStringsSource({"TaintCorner", TAINT_CONFIG})
    @MultiStringsSource({"CycleTaint", TAINT_CONFIG})
    @MultiStringsSource({"ComplexTaint", TAINT_CONFIG})
    @MultiStringsSource({"SimpleTaint", TAINT_CONFIG})
    @MultiStringsSource({"ArgToResult", TAINT_CONFIG})
    @MultiStringsSource({"BaseToResult", TAINT_CONFIG})
    @MultiStringsSource({"StringAppend", TAINT_CONFIG})
    @MultiStringsSource({"Java9StringConcat", TAINT_CONFIG})
    @MultiStringsSource({"OneCallTaint", "cs:1-call", TAINT_CONFIG})
    @MultiStringsSource({"InterTaintTransfer", "cs:2-call", TAINT_CONFIG})
    @MultiStringsSource({"TaintInList", "cs:2-obj", TAINT_CONFIG})
    @MultiStringsSource({"BackPropagation", TAINT_CONFIG})
    @MultiStringsSource({"CSBackPropagation", "cs:1-obj", TAINT_CONFIG})
    @MultiStringsSource({"StaticTaintTransfer",
            TAINT_CONFIG_PREFIX + "taint-config-static-taint-transfer.yml"})
    @MultiStringsSource({"InstanceSourceSink",
            TAINT_CONFIG_PREFIX + "taint-config-instance-source-sink.yml"})
    @MultiStringsSource({"ArrayFieldTransfer",
            TAINT_CONFIG_PREFIX + "taint-config-array-field-transfer.yml"})
    @MultiStringsSource({"TaintParam",
            TAINT_CONFIG_PREFIX + "taint-config-param-source.yml"})
    @MultiStringsSource({"TaintCall",
            TAINT_CONFIG_PREFIX + "taint-config-call-source.yml"})
    @MultiStringsSource({"CallSiteMode",
            TAINT_CONFIG_PREFIX + "taint-config-call-site-model.yml"})
    void test(String mainClass, String... opts) {
        Tests.testPTA(DIR, mainClass, opts);
    }

}
