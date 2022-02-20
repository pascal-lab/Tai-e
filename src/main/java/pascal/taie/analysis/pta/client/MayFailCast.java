package pascal.taie.analysis.pta.client;

import pascal.taie.World;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.type.Type;

public class MayFailCast extends Collector {

    public static final String ID = "may-fail-cast";

    public MayFailCast(AnalysisConfig config) {
        super(config);
    }

    @Override
    boolean isRelevant(Stmt stmt) {
        return stmt instanceof Cast;
    }

    @Override
    boolean isWanted(Stmt stmt, PointerAnalysisResult result) {
        Cast cast = (Cast) stmt;
        Type castType = cast.getRValue().getCastType();
        Var from = cast.getRValue().getValue();
        for (Obj obj : result.getPointsToSet(from)) {
            if (!World.get().getTypeManager().isSubtype(
                    castType, obj.getType())) {
                return true;
            }
        }
        return false;
    }

    @Override
    String getDescription() {
        return ID;
    }
}
