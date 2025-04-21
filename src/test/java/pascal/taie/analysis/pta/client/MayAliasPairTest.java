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


package pascal.taie.analysis.pta.client;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.ir.exp.Var;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MayAliasPairTest {
    static final String DIR = "basic";

    @ParameterizedTest
    @ValueSource(strings = {
            "New",
            "Assign",
            "StoreLoad",
            "Call",
            "InstanceField",
            "CallParamRet",
            "StaticCall",
            "MergeParam",
    })
    void test(String mainClass) {
        String ptaTestRoot = "src/test/resources/pta";
        String classPath = ptaTestRoot + "/" + DIR;
        List<String> args = List.of(
                // for loading class PTAAssert
                "-cp", ptaTestRoot,
                // for loading main class
                "-cp", classPath, "-m", mainClass,
                "-a", "may-alias-pair"
        );
        Main.main(args.toArray(new String[0]));
        MayAliasPair.MayAliasPairResult resultByAnalysis = World.get().getResult(MayAliasPair.ID);
        long appResultByDefinition = computeByDefinition();
        assertEquals(appResultByDefinition, resultByAnalysis.appAliasPairs());
    }

    private static long computeByDefinition() {
        PointerAnalysisResult ptaResult = World.get().getResult(PointerAnalysis.ID);
        Var[] appVars = ptaResult.getVars().stream()
                .filter(v -> v.getMethod().isApplication())
                .toArray(Var[]::new);
        long aliasPairs = 0;
        for (int i = 0; i < appVars.length; i++) {
            for (int j = i + 1; j < appVars.length; j++) {
                Var v1 = appVars[i], v2 = appVars[j];
                if (ptaResult.mayAlias(v1, v2)) {
                    aliasPairs++;
                }
            }
        }
        return aliasPairs;
    }
}
