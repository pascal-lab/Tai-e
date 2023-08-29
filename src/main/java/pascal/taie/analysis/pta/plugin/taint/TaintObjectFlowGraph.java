package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.Views;
import pascal.taie.util.graph.Graph;

import java.util.Collections;
import java.util.Set;

public class TaintObjectFlowGraph implements Graph<TaintNode> {

    private final MultiMap<TaintNode, TaintObjectFlowEdge> inEdges = Maps.newMultiMap(4096);

    private final MultiMap<TaintNode, TaintObjectFlowEdge> outEdges = Maps.newMultiMap(4096);

    private final Set<TaintNode> nodes = Sets.newSet(4096);

    private final TaintNode sourceNode;

    private final Set<TaintNode> sinkNodes;

    public TaintObjectFlowGraph(TaintNode sourceNode, Set<TaintNode> sinkNodes) {
        this.sourceNode = sourceNode;
        this.sinkNodes = Set.copyOf(sinkNodes);
        nodes.add(sourceNode);
        nodes.addAll(sinkNodes);
    }

    public void addEdge(TaintObjectFlowEdge edge) {
        outEdges.put(edge.source(), edge);
        inEdges.put(edge.target(), edge);
        nodes.add(edge.source());
        nodes.add(edge.target());
    }

    public TaintNode getSourceNode() {
        return sourceNode;
    }

    public Set<TaintNode> getSinkNodes() {
        return sinkNodes;
    }

    @Override
    public Set<TaintNode> getPredsOf(TaintNode node) {
        return Views.toMappedSet(getInEdgesOf(node), TaintObjectFlowEdge::source);
    }

    @Override
    public Set<TaintNode> getSuccsOf(TaintNode node) {
        return Views.toMappedSet(getOutEdgesOf(node), TaintObjectFlowEdge::target);
    }

    @Override
    public Set<TaintObjectFlowEdge> getInEdgesOf(TaintNode node) {
        return inEdges.get(node);
    }

    @Override
    public Set<TaintObjectFlowEdge> getOutEdgesOf(TaintNode node) {
        return outEdges.get(node);
    }

    @Override
    public Set<TaintNode> getNodes() {
        return Collections.unmodifiableSet(nodes);
    }
}
