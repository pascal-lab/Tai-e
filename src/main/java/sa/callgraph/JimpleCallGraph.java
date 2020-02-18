package sa.callgraph;

import sa.util.CollectionUtils;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

public class JimpleCallGraph extends AbstractCallGraph<Unit, SootMethod> {

    /**
     * Adds a new method to this call graph.
     * Returns true if the method was not in this call graph.
     */
    @Override
    protected boolean addNewMethod(SootMethod method) {
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
