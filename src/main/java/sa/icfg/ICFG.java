package sa.icfg;

import soot.toolkits.graph.DirectedGraph;

import java.util.Collection;

/**
 * Represents an interprocedural control-flow graph.
 */
public interface ICFG<Method, Node> extends DirectedGraph<Node> {

    Collection<Method> getEntryMethods();

    Collection<Method> getCalleesOf(Node callSite);

    Collection<Node> getEntriesOf(Method method);

    Collection<Node> getCallersOf(Method method);

    Collection<Node> getExitsOf(Method method);

    Collection<Node> getReturnSitesOf(Node callSite);

    boolean isCallSite(Node node);

    boolean isReturnSite(Node node);

    boolean isFunctionEntry(Node node);

    boolean isFunctionExit(Node node);
}
