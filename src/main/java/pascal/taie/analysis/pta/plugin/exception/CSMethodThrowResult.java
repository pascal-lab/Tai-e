package pascal.taie.analysis.pta.plugin.exception;

import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.EnhancedSet;
import pascal.taie.util.collection.Maps;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class CSMethodThrowResult {

    private final Supplier<EnhancedSet<CSObj>> setFactory;

    private final Map<Stmt, EnhancedSet<CSObj>> explicitExceptions;

    private final EnhancedSet<CSObj> uncaughtExceptions;

    CSMethodThrowResult(Supplier<EnhancedSet<CSObj>> setFactory) {
        this.setFactory = setFactory;
        explicitExceptions = Maps.newHybridMap();
        uncaughtExceptions = setFactory.get();
    }

    Set<CSObj> propagate(Stmt stmt, Set<CSObj> exceptions) {
        return explicitExceptions.computeIfAbsent(
                stmt, unused -> setFactory.get())
                .addAllDiff(exceptions);
    }

    void addUncaughtExceptions(Set<CSObj> exceptions) {
        uncaughtExceptions.addAll(exceptions);
    }

    Set<CSObj> mayThrowExplicitly(Stmt stmt) {
        Set<CSObj> result = explicitExceptions.get(stmt);
        return result != null ? result : Set.of();
    }

    Set<CSObj> mayThrowUncaught() {
        return Collections.unmodifiableSet(uncaughtExceptions);
    }
}
