package sa.icfg;

import sa.callgraph.CallGraph;

import java.util.Collection;

public abstract class AbstractICFG<Method, Node> implements ICFG<Method, Node> {

    protected CallGraph<Node, Method> callGraph;

    @Override
    public Collection<Method> getEntryMethods() {
        return callGraph.getEntryMethods();
    }

    @Override
    public Collection<Method> getCalleesOf(Node callSite) {
        return callGraph.getCallees(callSite);
    }

    @Override
    public Collection<Node> getCallersOf(Method method) {
        return callGraph.getCallers(method);
    }
}
