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

package pascal.taie.analysis.pta.toolkit.mahjong;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.StmtResult;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.client.MayFailCast;
import pascal.taie.language.classes.ClassMember;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MahjongTest {

    private static final String MAHJONG = "src/test/resources/pta/toolkit/mahjong/";

    @Test
    void testFPG() {
        Main.main("-acp", MAHJONG,
                "-m", "Mahjong",
                "-a", "pta=advanced:mahjong",
                "-a", "may-fail-cast");
        StmtResult<Boolean> mayFailCastResult = World.get().getResult(MayFailCast.ID);
        assertEquals(0, countRelevantStmts(mayFailCastResult));
    }

    @RepeatedTest(100)
    void testMultipleZeroLengthArrays() {
        // This bug appears non-deterministically
        assertDoesNotThrow(() -> Main.main("-acp", MAHJONG,
                "-m", "MultipleZeroLengthArrays",
                "-a", "pta=advanced:mahjong"));
    }

    /**
     * @param mayFailCastResult the result of the may-fail-cast analysis
     * @return the number of casts that may fail in app
     */
    private static long countRelevantStmts(StmtResult<Boolean> mayFailCastResult) {
        PointerAnalysisResult pta = World.get().getResult(PointerAnalysis.ID);
        return pta.getCallGraph().reachableMethods()
                .filter(ClassMember::isApplication)
                .flatMap(x -> x.getIR().stmts())
                .filter(mayFailCastResult::getResult)
                .count();
    }
}
