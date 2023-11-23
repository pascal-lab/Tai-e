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

import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResultImpl;
import pascal.taie.analysis.pta.plugin.taint.TaintAnalysis;
import pascal.taie.analysis.pta.plugin.taint.TaintFlow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DroidBenchTest {

    private static final String BENCHMARK_HOME_PREFIX = "android-benchmarks/DroidBench/apk/";

    private static final String BENCHMARK_INFO = "benchmark-info.yml";

    private static final String TAINT_CONFIG = BENCHMARK_HOME_PREFIX + "taint-config.yml";

    public void run(String type, String benchmark) {
        System.out.println("\nAnalyzing " + benchmark);
        Map<String, AndroidBenchmarkInfo> benchmarkInfos = AndroidBenchmarkInfo.load(BENCHMARK_HOME_PREFIX + type, BENCHMARK_INFO);
        AndroidBenchmarkInfo info = benchmarkInfos.get(benchmark);
        Main.main(composeArgs(info));
        PointerAnalysisResultImpl result = World.get().getResult(PointerAnalysis.ID);
        Set<TaintFlow> res = result.getResult(TaintAnalysis.class.getName());
        assertEquals(info.expected(), res.size());
    }

    private String[] composeArgs(AndroidBenchmarkInfo info) {
        List<String> args = new ArrayList<>();
        Collections.addAll(args,
                "-pp",
                "-cp", BENCHMARK_HOME_PREFIX + info.apk(),
                "-am");
        Map<String, String> ptaArgs = Map.of(
                "distinguish-string-constants", "null",
                "merge-string-objects", "true",
                "only-app", "true",
                "taint-config", TAINT_CONFIG,
                "propagate-types", "[reference,int,double,char]");
        Collections.addAll(args,
                "-a", "pta=" + ptaArgs.entrySet()
                        .stream()
                        .map(e -> e.getKey() + ":" + e.getValue())
                        .collect(Collectors.joining(";")),
                "-a", "may-fail-cast",
                "-a", "poly-call");
        return args.toArray(new String[0]);
    }
}
