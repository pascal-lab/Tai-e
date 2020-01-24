package sa.icfg;

import soot.toolkits.graph.DirectedGraph;

import java.util.Collection;

/**
 * Represents an interprocedural control-flow graph.
 */
public interface ICFG<Method, Node> extends DirectedGraph<Node> {

    Collection<Edge<Node>> getInEdgesOf(Node node);

    Collection<Edge<Node>> getOutEdgesOf(Node node);

    Collection<Method> getEntryMethods();

    Collection<Method> getCalleesOf(Node callSite);

    Collection<Node> getEntriesOf(Method method);

    Collection<Node> getCallersOf(Method method);

    Collection<Node> getExitsOf(Method method);

    Collection<Node> getReturnSitesOf(Node callSite);

    Method getContainingMethodOf(Node node);

    boolean isCallSite(Node node);
}
