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

package pascal.taie.analysis.sideeffect;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.Tests;

import java.util.LongSummaryStatistics;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SideEffectTest {

    private static final String CLASS_PATH = "src/test/resources/sideeffect/";

    private static void testSideEffect(String mainClass) {
        Tests.testMain(mainClass, CLASS_PATH, "side-effect",
                "-a", "pta=implicit-entries:false",
                "-a", "cg=algorithm:pta");
    }

    private static SideEffect runSideEffect(String mainClass, boolean onlyApp) {
        Main.main("-cp", CLASS_PATH,
                "-m", mainClass,
                "-a", "side-effect=only-app:" + onlyApp);
        return World.get().getResult(SideEffectAnalysis.ID);
    }

    private LongSummaryStatistics summarize(SideEffect sideEffect) {
        return World.get().getClassHierarchy().allClasses()
                .flatMap(c -> c.getDeclaredMethods().stream())
                .map(sideEffect::getModifiedObjects)
                .mapToLong(Set::size)
                .summaryStatistics();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "StaticStore",
            "SimpleCases",
            "LinkedList",
            "BubbleSort",
            "PureTest",
            "ConstructorTest",
            "PrimitiveTest",
            "Arrays",
            "SideEffects",
            "Globals",
            "Inheritance",
            "InterProc",
            "Recursion",
            "Loops",
            "Null",
            "OOP",
            "Milanova",
            "PolyLoop"
    })
    void test(String mainClass) {
        testSideEffect(mainClass);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "StaticStore",
            "SimpleCases",
            "LinkedList",
            "BubbleSort",
            "PureTest",
            "ConstructorTest",
            "PrimitiveTest",
            "Arrays",
            "SideEffects",
            "Globals",
            "Inheritance",
            "InterProc",
            "Recursion",
            "Loops",
            "Null",
            "OOP",
            "Milanova",
            "PolyLoop"
    })
    void testGlobal(String mainClass) {
        // when only-app is set to false, the result should be no smaller
        SideEffect appResult = runSideEffect(mainClass, true);
        LongSummaryStatistics appSummary = summarize(appResult);

        SideEffect globalResult = runSideEffect(mainClass, false);
        LongSummaryStatistics globalSummary = summarize(globalResult);

        assertTrue(appSummary.getMax() <= globalSummary.getMax());
    }
}
