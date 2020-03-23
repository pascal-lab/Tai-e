package pascal.pta.analysis.solver;

import pascal.callgraph.AbstractCallGraph;
import pascal.callgraph.Edge;
import pascal.pta.analysis.context.Context;
import pascal.pta.analysis.data.CSCallSite;
import pascal.pta.analysis.data.CSMethod;
import pascal.pta.analysis.data.DataManager;
import pascal.pta.element.CallSite;
import pascal.pta.element.Method;
import pascal.pta.statement.Call;
import pascal.pta.statement.Statement;
import pascal.util.CollectionUtils;

class OnFlyCallGraph extends AbstractCallGraph<CSCallSite, CSMethod> {

    private DataManager dataManager;

    OnFlyCallGraph(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    boolean addEdge(Edge<CSCallSite, CSMethod> edge) {
        return CollectionUtils.addToMapSet(callSiteToEdges, edge.getCallSite(), edge) ||
                CollectionUtils.addToMapSet(calleeToEdges, edge.getCallee(), edge);
    }

    boolean containsEdge(Edge<CSCallSite, CSMethod> edge) {
        return getEdgesOf(edge.getCallSite()).contains(edge);
    }

    @Override
    protected boolean addNewMethod(CSMethod csMethod) {
        if (reachableMethods.add(csMethod)) {
            Method method = csMethod.getMethod();
            Context context = csMethod.getContext();
            for (Statement s : method.getStatements()) {
                if (s instanceof Call) {
                    CallSite callSite = ((Call) s).getCallSite();
                    CSCallSite csCallSite = dataManager
                            .getCSCallSite(context, callSite);
                    callSiteToContainer.put(csCallSite, csMethod);
                    CollectionUtils.addToMapSet(callSitesIn, csMethod, csCallSite);
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
