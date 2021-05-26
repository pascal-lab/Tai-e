package pascal.taie.analysis.pta.plugin.Exception;

import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.ir.stmt.Stmt;

import java.util.Collection;
import java.util.Map;

import static java.util.Collections.emptySet;
import static pascal.taie.util.collection.MapUtils.newHybridMap;
import static pascal.taie.util.collection.SetUtils.newHybridSet;


public class CSMethodExceptionResult {
    private final Map<Stmt, Collection<CSObj>> explicitExceptions = newHybridMap();

    private final Collection<CSObj> thrownExplicitExceptions = newHybridSet();

    void addExplicit(Stmt stmt, Collection<CSObj> exceptions) {
        Collection<CSObj> originExceptions=explicitExceptions.getOrDefault(stmt,newHybridSet());
        originExceptions.addAll(exceptions);
        explicitExceptions.put(stmt, originExceptions);
    }

    void addUncaughtExceptions(Collection<CSObj> exceptions) {
        thrownExplicitExceptions.addAll(exceptions);
    }

    public Collection<CSObj> mayThrow(Stmt stmt) {
        return explicitExceptions.getOrDefault(stmt, emptySet());
    }

    public Collection<CSObj> getThrownExplicitExceptions() {
        return this.thrownExplicitExceptions;
    }

    public Collection<CSObj> getDifferentExceptions(Stmt stmt,
                                                    Collection<CSObj> exceptions) {
        Collection<CSObj> diff = newHybridSet();
        Collection<CSObj> origin = explicitExceptions.getOrDefault(stmt, emptySet());
        exceptions.forEach(exception -> {
            if (!origin.contains(exception)) {
                diff.add(exception);
            }
        });
        return diff;
    }
}
