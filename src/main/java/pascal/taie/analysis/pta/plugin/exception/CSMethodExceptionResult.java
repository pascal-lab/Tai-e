package pascal.taie.analysis.pta.plugin.exception;

import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.ir.stmt.Stmt;

import java.util.Collection;
import java.util.Map;

import static java.util.Collections.emptySet;
import static pascal.taie.util.collection.MapUtils.newHybridMap;
import static pascal.taie.util.collection.SetUtils.newHybridSet;


class CSMethodExceptionResult {

    private final Map<Stmt, Collection<CSObj>> explicitExceptions = newHybridMap();

    private final Collection<CSObj> thrownExplicitExceptions = newHybridSet();

    Collection<CSObj> propagateExplicit(Stmt stmt, Collection<CSObj> exceptions) {
        Collection<CSObj> diff = newHybridSet();
        Collection<CSObj> origin = explicitExceptions.computeIfAbsent(
                stmt, k -> newHybridSet());
        exceptions.forEach(exception -> {
            if (origin.add(exception)) {
                diff.add(exception);
            }
        });
        return diff;
    }

    void addUncaughtExceptions(Collection<CSObj> exceptions) {
        thrownExplicitExceptions.addAll(exceptions);
    }

    Collection<CSObj> mayThrow(Stmt stmt) {
        return explicitExceptions.getOrDefault(stmt, emptySet());
    }

    Collection<CSObj> getThrownExplicitExceptions() {
        return this.thrownExplicitExceptions;
    }
}
