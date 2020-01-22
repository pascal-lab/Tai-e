package sa.callgraph;

import java.util.Collection;

public interface CallGraph<CallSite, Method> {

    Collection<Method> calleesOf(CallSite callSite);

    Collection<Edge<CallSite, Method>> edgesOf(CallSite callSite);

    Collection<CallSite> callersOf(Method method);

    Method methodOf(CallSite callSite);

    Collection<Edge<CallSite, Method>> edges();
}
