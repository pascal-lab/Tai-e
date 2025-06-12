package pascal.taie.analysis.pta.toolkit.mahjong;

import org.junit.jupiter.api.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.StmtResult;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.client.MayFailCast;
import pascal.taie.language.classes.ClassMember;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MahjongTest {

    private static final String MISC = "src/test/resources/pta/misc/";

    @Test
    void testFPG() {
        Main.main("-acp", MISC,
                "-m", "Mahjong",
                "-a", "pta=advanced:mahjong",
                "-a", "may-fail-cast");
        StmtResult<Boolean> mayFailCastResult = World.get().getResult(MayFailCast.ID);
        assertEquals(0, countRelevantStmts(mayFailCastResult));
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
