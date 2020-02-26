package sa.callgraph;

import sa.util.CollectionUtils;
import sa.util.CollectionView;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public abstract class AbstractCallGraph<CallSite, Method>
        implements CallGraph<CallSite, Method> {

    protected Map<CallSite, Set<Edge<CallSite, Method>>> callSiteToEdges;
    protected Map<Method, Set<Edge<CallSite, Method>>> calleeToEdges;
    protected Map<CallSite, Method> callSiteToContainer;
    protected Map<Method, Set<CallSite>> callSitesIn;
    protected Set<Method> entryMethods;
    protected Set<Method> reachableMethods;

    protected AbstractCallGraph() {
        callSiteToEdges = new HashMap<>();
        calleeToEdges = new HashMap<>();
        callSiteToContainer = new HashMap<>();
        callSitesIn = new HashMap<>();
        entryMethods = new HashSet<>();
        reachableMethods = new HashSet<>();
    }

    public void addEntryMethod(Method entryMethod) {
        entryMethods.add(entryMethod);
        addNewMethod(entryMethod);
    }

    public boolean addEdge(CallSite callSite, Method callee, CallKind kind) {
        addNewMethod(callee);
        Edge<CallSite, Method> edge = new Edge<>(kind, callSite, callee);
        return CollectionUtils.addToMapSet(callSiteToEdges, callSite, edge) ||
                CollectionUtils.addToMapSet(calleeToEdges, callee, edge);
    }

    /**
     * Adds a new method to this call graph.
     * Returns true if the method was not in this call graph.
     */
    protected abstract boolean addNewMethod(Method method);

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
    public Method getContainerMethodOf(CallSite callSite) {
        return callSiteToContainer.get(callSite);
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
    public Stream<Edge<CallSite, Method>> getAllEdges() {
        return callSiteToEdges.values()
                .stream()
                .flatMap(Set::stream);
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
        return getAllEdges().iterator();
    }
}
