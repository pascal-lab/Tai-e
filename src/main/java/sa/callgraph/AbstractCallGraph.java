package sa.callgraph;

import sa.util.CollectionView;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class AbstractCallGraph<CallSite, Method>
        implements CallGraph<CallSite, Method> {

    protected Map<CallSite, Set<Edge<CallSite, Method>>> callSiteToEdges;
    protected Map<Method, Set<Edge<CallSite, Method>>> calleeToEdges;
    protected Map<CallSite, Method> containingMethod;
    protected Map<Method, Set<CallSite>> callSitesIn;
    protected Set<Method> entryMethods;
    protected Set<Method> reachableMethods;

    @Override
    public Collection<Method> getCallees(CallSite callSite) {
        Set<Edge<CallSite, Method>> edges = callSiteToEdges.get(callSite);
        if (edges != null) {
            return CollectionView.of(edges, Edge::getCallee);
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public Collection<CallSite> getCallers(Method callee) {
        Set<Edge<CallSite, Method>> edges = calleeToEdges.get(callee);
        if (edges != null) {
            return CollectionView.of(edges, Edge::getCallSite);
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public Method getContainingMethod(CallSite callSite) {
        return containingMethod.get(callSite);
    }

    @Override
    public Collection<CallSite> getCallSitesIn(Method method) {
        return callSitesIn.getOrDefault(method, Collections.emptySet());
    }

    @Override
    public Collection<Edge<CallSite, Method>> getEdgesOf(CallSite callSite) {
        return callSiteToEdges.getOrDefault(callSite, Collections.emptySet());
    }

    @Override
    public Iterator<Edge<CallSite, Method>> getAllEdges() {
        return callSiteToEdges.values()
                .stream()
                .flatMap(Set::stream)
                .iterator();
    }

    @Override
    public Collection<Method> getEntryMethods() {
        return entryMethods;
    }

    @Override
    public Collection<Method> getReachableMethods() {
        return reachableMethods;
    }

    @Override
    public boolean contains(Method method) {
        return reachableMethods.contains(method);
    }

    @Override
    public Iterator<Edge<CallSite, Method>> iterator() {
        return getAllEdges();
    }
}
