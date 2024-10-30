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
import org.junit.jupiter.params.provider.ValueSource;
import pascal.taie.Main;
import pascal.taie.analysis.Tests;
import pascal.taie.util.MultiStringsSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TaintTest {

    private static final String DIR = "taint";

    private static final String TAINT_CONFIG_PREFIX = "taint-config:src/test/resources/pta/taint/";

    private static final String TAINT_CONFIG = TAINT_CONFIG_PREFIX + "taint-config.yml";

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
        testInNonInteractiveMode(mainClass, opts);
        testInInteractiveMode(mainClass, opts);
    }

    private void testInNonInteractiveMode(String mainClass, String... opts) {
        Tests.testPTA(DIR, mainClass, opts);
    }

    private void testInInteractiveMode(String mainClass, String... opts) {
        InputStream originalSystemIn = System.in;
        try {
            String simulatedInput = "r\ne\n";
            System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));
            String[] newOpts = new String[opts.length + 1];
            System.arraycopy(opts, 0, newOpts, 0, opts.length);
            newOpts[opts.length] = "taint-interactive-mode:true";
            Tests.testPTA(DIR, mainClass, newOpts);
        } finally {
            System.setIn(originalSystemIn);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SimpleTaint",
            "TaintCall",
            "TaintParam",
    })
    void testTaintConfigProvider(String mainClass) {
        List<String> ptaArgs = new ArrayList<>();
        Collections.addAll(ptaArgs,
                "implicit-entries:false",
                "only-app:true",
                "distinguish-string-constants:all",
                "expected-file:src/test/resources/pta/taint/" + mainClass + "-pta-expected.txt",
                "taint-config-providers:[pascal.taie.analysis.pta.MockTaintConfigProvider]"
        );
        Main.main(
                "-pp",
                "-cp", "src/test/resources/pta/taint",
                "-m", mainClass,
                "-a", "pta=" + String.join(";", ptaArgs)
        );
    }
}
