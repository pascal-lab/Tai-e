package pascal.taie.analysis.pta.plugin.Exception;

import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;

import java.util.Collection;
import java.util.Map;

import static pascal.taie.util.collection.MapUtils.newHybridMap;
import static pascal.taie.util.collection.SetUtils.newHybridSet;


public class JMethodExceptionResult {
    private final Map<Stmt, Collection<Obj>> explicitExceptions = newHybridMap();

    private final Collection<Obj> thrownExplicitExceptions = newHybridSet();

    private final JMethod jMethod;

    JMethodExceptionResult(JMethod jMethod,
                           CSMethodExceptionResult csMethodExceptionResult) {
        this.jMethod = jMethod;
        jMethod.getIR().getStmts().forEach(stmt -> {
            Collection<CSObj> csExceptions =
                    csMethodExceptionResult.mayThrow(stmt);
            Collection<Obj> exceptions = newHybridSet();
            csExceptions.forEach(csException -> {
                exceptions.add(csException.getObject());
            });
        });
        Collection<CSObj> methodCSExceptions =
                csMethodExceptionResult.getThrownExplicitExceptions();
        methodCSExceptions.forEach(csException -> {
            thrownExplicitExceptions.add(csException.getObject());
        });
    }

    public JMethodExceptionResult(JMethod jMethod) {
        this.jMethod = jMethod;
    }

    void addExplicit(Stmt stmt, Collection<Obj> exceptions) {
        Collection<Obj> originExceptions =
                explicitExceptions.getOrDefault(stmt, newHybridSet());
        originExceptions.addAll(exceptions);
        explicitExceptions.put(stmt, originExceptions);
    }

    void addUncaughtExceptions(Collection<Obj> exceptions) {
        thrownExplicitExceptions.addAll(exceptions);
    }

    public Collection<Obj> mayThrow(Stmt stmt) {
        return explicitExceptions.getOrDefault(stmt, newHybridSet());
    }

    public Collection<Obj> getThrownExplicitExceptions() {
        return this.thrownExplicitExceptions;
    }

    void addCSMethodExceptionResult(CSMethodExceptionResult csMethodExceptionResult) {
        jMethod.getIR().getStmts().forEach(stmt -> {
            Collection<CSObj> csExceptions = csMethodExceptionResult.mayThrow(stmt);
            if (csExceptions.size() > 0) {
                Collection<Obj> exceptions = explicitExceptions.
                        computeIfAbsent(stmt, key -> newHybridSet());
                csExceptions.forEach(csException -> {
                    exceptions.add(csException.getObject());
                });
            }
        });
        Collection<CSObj> methodCSExceptions =
                csMethodExceptionResult.getThrownExplicitExceptions();
        methodCSExceptions.forEach(csException -> {
            thrownExplicitExceptions.add(csException.getObject());
        });
    }

    @Override
    public String toString() {
        return "JMethodExceptionResult{" +
                "jMethod=" + jMethod +
                ", explicitExceptions=" + explicitExceptions +
                ", thrownExplicitExceptions=" + thrownExplicitExceptions +
                '}';
    }
}
