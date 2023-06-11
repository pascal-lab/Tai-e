package pascal.taie.analysis.sideeffect;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.language.classes.JMethod;

import java.util.Comparator;

public class SideEffectAnalysis extends ProgramAnalysis<SideEffect> {

    public static final String ID = "side-effect";

    private static final Logger logger = LogManager.getLogger(SideEffectAnalysis.class);

    public SideEffectAnalysis(AnalysisConfig config) {
        super(config);
    }

    @Override
    public SideEffect analyze() {
        PointerAnalysisResult ptaResult = World.get().getResult(PointerAnalysis.ID);
        SideEffect result = new Solver(ptaResult).solve();
        result.getImpureMethods()
                .stream()
                .sorted(Comparator.comparing(JMethod::toString))
                .forEach(m -> logger.info("{} may modify {} objects",
                        m, result.getModifiedObjects(m).size()));
        logger.info("==============================");
        return result;
    }
}

