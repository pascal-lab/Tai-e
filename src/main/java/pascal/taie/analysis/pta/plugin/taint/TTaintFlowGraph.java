package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.graph.flowgraph.Node;
import pascal.taie.analysis.graph.flowgraph.NodeManager;
import pascal.taie.analysis.pta.core.cs.element.*;
import pascal.taie.util.Indexer;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.Views;
import pascal.taie.util.graph.Graph;

import java.util.*;
import java.util.stream.Collectors;

public class TTaintFlowGraph extends NodeManager
        implements Graph<Node>, Indexer<Node> {

    private final Set<Node> sourceNodes = null;

    private final Set<Node> sinkNodes = null;

    private final Map<Node, List<Set<TaintFlowEdge>>> inEdges = Maps.newHybridMap();

    private final Map<Node, List<Set<TaintFlowEdge>>> outEdges = Maps.newHybridMap();

    private final Set<TaintFlowEdge> edges = Sets.newHybridSet();

    public TTaintFlowGraph(TaintPointerFlowGraph tpfg){
//        this.sourceNodes = tpfg.getSourcePointers().stream()
//                .map(this::toNode)
//                .collect(Collectors.toSet());
//        this.sinkNodes = tpfg.getSinkPointers().stream()
//                .map(this::toNode)
//                .collect(Collectors.toSet());
        Map<Node, List<Pointer>> mergeNodes = Maps.newHybridMap();
        tpfg.getNodes().forEach(node -> {
//            inEdges.computeIfAbsent(toNode(node), k -> new ArrayList<>())
//                    .add(tpfg.getInEdgesOf(node).stream()
//                            .map(edge->new TaintFlowEdge(edge.kind(), toNode(edge.source()), toNode(edge.target())))
//                            .collect(Collectors.toSet()));
//            outEdges.computeIfAbsent(toNode(node), k -> new ArrayList<>())
//                    .add(tpfg.getOutEdgesOf(node).stream()
//                            .map(edge->new TaintFlowEdge(edge.kind(), toNode(edge.source()), toNode(edge.target())))
//                            .collect(Collectors.toSet()));
            mergeNodes.computeIfAbsent(toNode(node), k -> new ArrayList<>()).add(node);
        });
        tpfg.getNodes().forEach(node -> edges.addAll(tpfg.getInEdgesOf(node).stream()
                .map(edge -> new TaintFlowEdge(edge.kind(), toNode(edge.source()), toNode(edge.target()),
                        mergeNodes.get(toNode(node)).indexOf(edge.source()), mergeNodes.get(toNode(node)).indexOf(edge.target())))
                .collect(Collectors.toSet())));
    }

    private Node toNode(Pointer pointer) {
        if (pointer instanceof CSVar csVar) {
            return getOrCreateVarNode(csVar.getVar());
        } else if (pointer instanceof InstanceField iField) {
            return getOrCreateInstanceFieldNode(
                    iField.getBase().getObject(), iField.getField());
        } else if (pointer instanceof ArrayIndex arrayIndex) {
            return getOrCreateArrayIndexNode(
                    arrayIndex.getArray().getObject());
        } else {
            return getOrCreateStaticFieldNode(
                    ((StaticField) pointer).getField());
        }
    }

    public Set<Node> getSourceNodes(){
        return sourceNodes;
    }

    public Set<Node> getSinkNodes(){
        return sinkNodes;
    }

    public Map<Node, List<Set<TaintFlowEdge>>> getInEdges(){
        return inEdges;
    }

    public Map<Node, List<Set<TaintFlowEdge>>> getOutEdges(){
        return outEdges;
    }

    public Set<TaintFlowEdge> getEdges() {
        return edges;
    }

    @Override
    public Set<Node> getPredsOf(Node node) {
        return Views.toMappedSet(getInEdgesOf(node), TaintFlowEdge::source);
    }

    @Override
    public Set<Node> getSuccsOf(Node node) {
        return Views.toMappedSet(getOutEdgesOf(node), TaintFlowEdge::target);
    }

    @Override
    public Set<TaintFlowEdge> getInEdgesOf(Node node) {
        return inEdges.get(node).stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<TaintFlowEdge> getOutEdgesOf(Node node) {
        return outEdges.get(node).stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }
}
