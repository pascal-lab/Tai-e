package sa.callgraph;

import sa.util.CollectionUtils;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class JimpleCallGraph extends AbstractCallGraph<Unit, SootMethod> {

    public JimpleCallGraph() {
        callSiteToEdges = new HashMap<>();
        calleeToEdges = new HashMap<>();
        containingMethod = new HashMap<>();
        callSitesIn = new HashMap<>();
        entryMethods = new HashSet<>();
        reachableMethods = new HashSet<>();
    }

    public void setEntryMethods(Collection<SootMethod> entryMethods) {
        this.entryMethods.addAll(entryMethods);
        entryMethods.forEach(this::addNewMethod);
    }

    public boolean addEdge(Unit callSite, SootMethod callee, CallKind kind) {
        addNewMethod(callee);
        Edge<Unit, SootMethod> edge = new Edge<>(kind, callSite, callee);
        return CollectionUtils.addToMapSet(callSiteToEdges, callSite, edge) ||
                CollectionUtils.addToMapSet(calleeToEdges, callee, edge);
    }

    /**
     * Adds a new method to this call graph.
     * Returns true if the method was not in this call graph.
     */
    private boolean addNewMethod(SootMethod method) {
        if (reachableMethods.add(method)) {
            if (method.isConcrete()) {
                for (Unit unit : method.retrieveActiveBody().getUnits()) {
                    Stmt stmt = (Stmt) unit;
                    if (stmt.containsInvokeExpr()) {
                        containingMethod.put(stmt, method);
                        CollectionUtils.addToMapSet(callSitesIn, method, stmt);
                    }
                }
            }
            return true;
        }
        return false;
    }
}
