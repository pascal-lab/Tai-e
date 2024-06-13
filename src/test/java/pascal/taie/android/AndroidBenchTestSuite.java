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

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import pascal.taie.android.droidbench.AliasingTest;
import pascal.taie.android.droidbench.AndroidSpecificTest;
import pascal.taie.android.droidbench.ArraysAndListsTest;
import pascal.taie.android.droidbench.CallbackTest;
import pascal.taie.android.droidbench.DroidBenchTestSuite;
import pascal.taie.android.droidbench.EmulatorDetectionTest;
import pascal.taie.android.droidbench.FieldAndObjectSensitivityTest;
import pascal.taie.android.droidbench.GeneralJavaTest;
import pascal.taie.android.droidbench.InterAppCommunicationTest;
import pascal.taie.android.droidbench.InterComponentCommunicationTest;
import pascal.taie.android.droidbench.LifecycleTest;
import pascal.taie.android.droidbench.ReflectionTest;
import pascal.taie.android.droidbench.ThreadingTest;
import pascal.taie.android.droidbench.UnreachableCodeTest;
import pascal.taie.android.iccbench.ICCBenchTest;
import pascal.taie.android.ubcbench.UBCBenchTest;

@Suite
@SelectClasses({
        DroidBenchTestSuite.class,
        ICCBenchTest.class,
        UBCBenchTest.class
})
public class AndroidBenchTestSuite {
}
