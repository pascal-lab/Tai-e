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

public class InterComponentCommunicationTest extends DroidBenchTest {

    static final String CATEGORY = "InterComponentCommunication";

    /**
     * Tests for InterComponentCommunication CATEGORY apk
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "ActivityCommunication1",
            "ActivityCommunication2",
            "ActivityCommunication3",
            "ActivityCommunication4",
            "ActivityCommunication5",
            "ActivityCommunication6",
            "ActivityCommunication7",
            "ActivityCommunication8",
            "BroadcastTaintAndLeak1",
            "ComponentNotInManifest1",
            "EventOrdering1",
            "ServiceCommunication1",
            "SharedPreferences1",
            "Singletons1",
            "UnresolvableIntent1",
    })
    void test(String benchmark) {
        run(CATEGORY, benchmark);
    }

}
