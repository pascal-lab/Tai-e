package pascal.taie.analysis.pta.plugin.exception;

import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.Collections;
import java.util.Set;

import static pascal.taie.util.collection.Maps.newHybridMap;
import static pascal.taie.util.collection.Sets.newHybridSet;

public class MethodThrowResult {

    private final JMethod method;

    private final MultiMap<Stmt, Obj> explicitExceptions
            = Maps.newMultiMap(newHybridMap());

    private final Set<Obj> uncaughtExceptions = newHybridSet();

    public MethodThrowResult(JMethod method) {
        this.method = method;
    }

    public Set<Obj> mayThrowExplicitly(Stmt stmt) {
        return explicitExceptions.get(stmt);
    }

    public Set<Obj> mayThrowUncaught() {
        return Collections.unmodifiableSet(uncaughtExceptions);
    }

    void addCSMethodThrowResult(CSMethodThrowResult csMethodThrowResult) {
        for (Stmt stmt : method.getIR()) {
            csMethodThrowResult.mayThrowExplicitly(stmt)
                    .objects()
                    .map(CSObj::getObject)
                    .forEach(exception ->
                            explicitExceptions.put(stmt, exception));
        }
        csMethodThrowResult.mayThrowUncaught()
                .objects()
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
