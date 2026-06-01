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

package pascal.taie.android;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResultImpl;
import pascal.taie.analysis.pta.plugin.taint.TaintAnalysis;
import pascal.taie.analysis.pta.plugin.taint.TaintFlow;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AndroidBenchTest {

    private static final Logger logger = LogManager.getLogger(AndroidBenchTest.class);

    private static final String BENCHMARK_INFO = "benchmark-info.yml";

    protected static final String TAINT_CONFIG_MICRO = "android-benchmarks/suite/taint-config.yml";

    protected static final String TAINT_CONFIG_REAL = "android-benchmarks/taint-config-real.yml";

    public void run(String benchmarkPrefix, String benchmark, boolean isRealWorld) {
        System.out.println("\nAnalyzing " + benchmark);
        Map<String, AndroidBenchmarkInfo> benchmarkInfos = AndroidBenchmarkInfo.load(benchmarkPrefix, BENCHMARK_INFO);
        AndroidBenchmarkInfo info = benchmarkInfos.get(benchmark);
        Main.main(composeArgs(benchmarkPrefix, info, isRealWorld));
        PointerAnalysisResultImpl result = World.get().getResult(PointerAnalysis.ID);
        Set<TaintFlow> res = result.getResult(TaintAnalysis.class.getName());
        if (!isRealWorld) {
            if (info.groundTruth() != null) {
                logger.info(
                        "[Benchmark Note] {}: current expected = {}, ground truth = {}, actual = {}.",
                        info.id(),
                        info.expected(),
                        info.groundTruth(),
                        res.size()
                );
            }

            assertEquals(
                    info.expected(),
                    res.size(),
                    () -> {
                        String note = info.groundTruth() == null
                                ? ""
                                : String.format(
                                " Known failing case: current expected value is a temporary override. " +
                                        "Ground truth is %d. Re-validate after analysis logic changes.",
                                info.groundTruth()
                        );

                        return String.format(
                                "Benchmark %s failed. Expected: %d, actual: %d.%s",
                                info.id(),
                                info.expected(),
                                res.size(),
                                note
                        );
                    }
            );
        }
    }

    private String[] composeArgs(String benchmarkHomePrefix, AndroidBenchmarkInfo info, boolean isRealWorld) {
        List<String> args = new ArrayList<>();
        Collections.addAll(args,
                "-cp", new File(benchmarkHomePrefix, info.apk()).getPath(),
                "-am");
        Map<String, String> ptaArgs = Map.of(
                "taint-config", isRealWorld ? TAINT_CONFIG_REAL : TAINT_CONFIG_MICRO
        );

        Map<String, String> cgArgs = Map.of(
                "algorithm", "pta"
        );

        Collections.addAll(args,
                "-a", "pta=" + ptaArgs.entrySet()
                        .stream()
                        .map(e -> e.getKey() + ":" + e.getValue())
                        .collect(Collectors.joining(";")),
                "-a", "cg=" + cgArgs.entrySet()
                        .stream()
                        .map(e -> e.getKey() + ":" + e.getValue())
                        .collect(Collectors.joining(";"))
        );
        return args.toArray(new String[0]);
    }

}
