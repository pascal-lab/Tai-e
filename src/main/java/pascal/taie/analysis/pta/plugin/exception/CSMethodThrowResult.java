package pascal.taie.analysis.pta.plugin.exception;

import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.Maps;

import java.util.Map;
import java.util.function.Supplier;

public class CSMethodThrowResult {

    private final Supplier<PointsToSet> setFactory;

    private final Map<Stmt, PointsToSet> explicitExceptions = Maps.newHybridMap();

    private final PointsToSet uncaughtExceptions;

    CSMethodThrowResult(Supplier<PointsToSet> setFactory) {
        this.setFactory = setFactory;
        uncaughtExceptions = setFactory.get();
    }

    PointsToSet propagate(Stmt stmt, PointsToSet exceptions) {
        return explicitExceptions.computeIfAbsent(
                stmt, unused -> setFactory.get())
                .addAllDiff(exceptions);
    }

    void addUncaughtExceptions(PointsToSet exceptions) {
        uncaughtExceptions.addAll(exceptions);
    }

    PointsToSet mayThrowExplicitly(Stmt stmt) {
        return explicitExceptions.getOrDefault(stmt, PointsToSet.emptySet());
    }

    PointsToSet mayThrowUncaught() {
        return uncaughtExceptions;
    }
}
