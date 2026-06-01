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

    private static final String BENCHMARK_HOME_PREFIX = "android-benchmarks/suite/real-world";

    @ParameterizedTest
    @ValueSource(strings = {
            "UBCBench-20",
            "UBCBench-21",
            "UBCBench-22",
            "UBCBench-23",
            "UBCBench-24",
            "UBCBench-25",
            "TaintBench-backflash",
            "TaintBench-beita_com_beita_contact",
            "TaintBench-cajino_baidu",
            "TaintBench-chat_hook",
            "TaintBench-chulia",
            "TaintBench-death_ring_materialflow",
            "TaintBench-dsencrypt_samp",
            "TaintBench-exprespam",
            "TaintBench-fakeappstore",
            "TaintBench-fakebank_android_samp",
            "TaintBench-fakedaum",
            "TaintBench-fakemart",
            "TaintBench-fakeplay",
            "TaintBench-faketaobao",
            "TaintBench-godwon_samp",
            "TaintBench-hummingbad_android_samp",
            "TaintBench-jollyserv",
            "TaintBench-overlay_android_samp",
            "TaintBench-overlaylocker2_android_samp",
            "TaintBench-phospy",
            "TaintBench-proxy_samp",
            "TaintBench-remote_control_smack",
            "TaintBench-repane",
            "TaintBench-roidsec",
            "TaintBench-samsapo",
            "TaintBench-save_me",
            "TaintBench-scipiex",
            "TaintBench-slocker_android_samp",
            "TaintBench-sms_google",
            "TaintBench-sms_send_locker_qqmagic",
            "TaintBench-smssend_packageInstaller",
            "TaintBench-smssilience_fake_vertu",
            "TaintBench-smsstealer_kysn_assassincreed_android_samp",
            "TaintBench-stels_flashplayer_android_update",
            "TaintBench-tetus",
            "TaintBench-the_interview_movieshow",
            "TaintBench-threatjapan_uracto",
            "TaintBench-vibleaker_android_samp",
            "TaintBench-xbot_android_samp"
    })
    void test(String benchmark) {
        run(BENCHMARK_HOME_PREFIX, benchmark, true);
    }
}
