package pascal.taie.analysis.pta.client;

import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;

public class PolymorphicCallSite extends Collector {

    public static final String ID = "poly-call";

    public PolymorphicCallSite(AnalysisConfig config) {
        super(config);
    }

    @Override
    boolean isRelevant(Stmt stmt) {
        return stmt instanceof Invoke invoke &&
                (invoke.isVirtual() || invoke.isInterface());
    }

    @Override
    boolean want(Stmt stmt, PointerAnalysisResult result) {
        Invoke invoke = (Invoke) stmt;
        return result.getCallGraph().getCalleesOf(invoke).size() > 1;
    }

    @Override
    String getDescription() {
        return ID;
    }
}
