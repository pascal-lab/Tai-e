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

public class LifecycleTest extends DroidBenchTest {

    static final String TYPE = "Lifecycle";

    /**
     * Tests for Lifecycle Type apk
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "ActivityEventSequence1",
            "ActivityEventSequence2",
            "ActivityEventSequence3",
            "ActivityLifecycle1",
            "ActivityLifecycle2",
            "ActivityLifecycle3",
            "ActivityLifecycle4",
            "ActivitySavedState1",
            "ApplicationLifecycle1",
            "ApplicationLifecycle2",
            "ApplicationLifecycle3",
            "AsynchronousEventOrdering1",
            "BroadcastReceiverLifecycle1",
            "BroadcastReceiverLifecycle2",
            "BroadcastReceiverLifecycle3",
            "EventOrdering1",
            "FragmentLifecycle1",
            "FragmentLifecycle2",
            "ServiceEventSequence1",
            "ServiceEventSequence2",
            "ServiceEventSequence3",
            "ServiceLifecycle1",
            "ServiceLifecycle2",
            "SharedPreferenceChanged1"
    })
    void test(String benchmark) {
        run(TYPE, benchmark);
    }

}
