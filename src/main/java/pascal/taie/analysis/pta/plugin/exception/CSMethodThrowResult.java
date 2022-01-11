package pascal.taie.analysis.pta.plugin.exception;

import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class CSMethodThrowResult {

    private final MultiMap<Stmt, CSObj> explicitExceptions = Maps.newMultiMap();

    private final Set<CSObj> uncaughtExceptions = Sets.newHybridSet();

    Set<CSObj> propagate(Stmt stmt, Collection<CSObj> exceptions) {
        Set<CSObj> diff = Sets.newHybridSet();
        exceptions.forEach(exception -> {
            if (explicitExceptions.put(stmt, exception)) {
                diff.add(exception);
            }
        });
        return diff;
    }

    void addUncaughtExceptions(Collection<CSObj> exceptions) {
        uncaughtExceptions.addAll(exceptions);
    }

    Set<CSObj> mayThrowExplicitly(Stmt stmt) {
        return explicitExceptions.get(stmt);
    }

    Set<CSObj> mayThrowUncaught() {
        return Collections.unmodifiableSet(uncaughtExceptions);
    }
}
