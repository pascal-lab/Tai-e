package pascal.taie.analysis.pta.plugin.exception;

import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.Collection;

import static pascal.taie.util.collection.Maps.newHybridMap;
import static pascal.taie.util.collection.Sets.newHybridSet;

public class MethodThrowResult {

    private final JMethod method;

    private final MultiMap<Stmt, Obj> explicitExceptions
            = Maps.newMultiMap(newHybridMap());

    private final Collection<Obj> uncaughtExceptions = newHybridSet();

    public MethodThrowResult(JMethod method) {
        this.method = method;
    }

    public Collection<Obj> mayThrowExplicitly(Stmt stmt) {
        return explicitExceptions.get(stmt);
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
                                explicitExceptions.put(stmt, exception))
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
