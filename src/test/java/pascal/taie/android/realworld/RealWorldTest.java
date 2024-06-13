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

package pascal.taie.android.realworld;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pascal.taie.android.AndroidBenchTest;

public class RealWorldTest extends AndroidBenchTest {

    private static final String BENCHMARK_HOME_PREFIX = "android-benchmarks/real-world";

    @ParameterizedTest
    @ValueSource(strings = {
            "UBCBench-20",
            "UBCBench-21",
            "UBCBench-22",
            "UBCBench-23",
            "UBCBench-24",
            "UBCBench-25",
            "TaintBench-beita_com_beita_contact",
            "TaintBench-cajino_baidu",
            "TaintBench-death_ring_materialflow",
            "TaintBench-smssilience_fake_vertu",
            "TaintBench-threatjapan_uracto",
            "TaintBench-xbot_android_samp"
    })
    void test(String benchmark) {
        run(BENCHMARK_HOME_PREFIX, benchmark, true);
    }
}
