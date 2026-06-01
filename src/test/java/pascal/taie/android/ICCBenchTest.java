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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ICCBenchTest extends AndroidBenchTest {

    private static final String BENCHMARK_HOME_PREFIX = "android-benchmarks/suite/ICC-Bench/apks/";

    @ParameterizedTest
    @ValueSource(strings = {
            "rpc_localservice",
            "rpc_messengerservice",
            "rpc_returnsensitive",
            "rpc_remoteservice",
            "icc_rpc_comprehensive",
            "icc_dynregister1",
            "icc_dynregister2",
            "icc_explicit1",
            "icc_implicit_action",
            "icc_implicit_category",
            "icc_implicit_data1",
            "icc_implicit_data2",
            "icc_implicit_mix1",
            "icc_implicit_mix2",
            "icc_explicit_nosrc_nosink",
            "icc_explicit_nosrc_sink",
            "icc_explicit_src_nosink",
            "icc_explicit_src_sink",
            "icc_implicit_nosrc_nosink",
            "icc_implicit_nosrc_sink",
            "icc_implicit_src_nosink",
            "icc_implicit_src_sink",
            "icc_intentservice",
            "icc_stateful",
    })
    void test(String benchmark) {
        run(BENCHMARK_HOME_PREFIX, benchmark, false);
    }
}
