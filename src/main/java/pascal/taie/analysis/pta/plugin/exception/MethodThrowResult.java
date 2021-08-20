package pascal.taie.analysis.pta.plugin.exception;

import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.MapUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static pascal.taie.util.collection.MapUtils.newHybridMap;
import static pascal.taie.util.collection.SetUtils.newHybridSet;

public class MethodThrowResult {

    private final JMethod method;

    private final Map<Stmt, Set<Obj>> explicitExceptions = newHybridMap();

    private final Collection<Obj> uncaughtExceptions = newHybridSet();

    public MethodThrowResult(JMethod method) {
        this.method = method;
    }

    public Collection<Obj> mayThrowExplicitly(Stmt stmt) {
        return explicitExceptions.getOrDefault(stmt, newHybridSet());
    }

    public Collection<Obj> mayThrowUncaught() {
        return uncaughtExceptions;
    }

    void addCSMethodThrowResult(CSMethodThrowResult csMethodThrowResult) {
        method.getIR().forEach(stmt ->
                csMethodThrowResult.mayThrowExplicitly(stmt)
                        .stream()
                        .map(CSObj::getObject)
                        .forEach(exception ->
                                MapUtils.addToMapSet(explicitExceptions, stmt, exception))
        );
        csMethodThrowResult.mayThrowUncaught()
                .stream()
                .map(CSObj::getObject)
                .forEach(uncaughtExceptions::add);
    }

    @Override
    public String toString() {
        return "MethodThrowResult{" +
                "method=" + method +
                ", explicitExceptions=" + explicitExceptions +
                ", uncaughtExceptions=" + uncaughtExceptions +
                '}';
    }
}
