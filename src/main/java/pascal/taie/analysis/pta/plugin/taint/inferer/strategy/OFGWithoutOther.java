package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.graph.flowgraph.FlowEdge;
import pascal.taie.analysis.graph.flowgraph.FlowKind;
import pascal.taie.analysis.graph.flowgraph.InstanceFieldNode;
import pascal.taie.analysis.graph.flowgraph.Node;
import pascal.taie.analysis.graph.flowgraph.ObjectFlowGraph;
import pascal.taie.analysis.graph.flowgraph.VarNode;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JField;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.TwoKeyMap;
import pascal.taie.util.collection.Views;
import pascal.taie.util.graph.Graph;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class OFGWithoutOther implements Graph<Node> {

    private final Set<Node> nodes;

    private final Map<Var, VarNode> var2Node = Maps.newMap(4096);

    private final TwoKeyMap<Obj, JField, InstanceFieldNode> iField2Node = Maps.newTwoKeyMap();

    private final MultiMap<Node, FlowEdge> inEdges;

    private final MultiMap<Node, FlowEdge> outEdges;

    public OFGWithoutOther(ObjectFlowGraph ofg) {
        this.nodes = Sets.newSet(ofg.getNumberOfNodes());
        this.inEdges = Maps.newMultiMap(ofg.getNumberOfNodes());
        this.outEdges = Maps.newMultiMap(ofg.getNumberOfNodes());
        for(Node node : ofg) {
            ofg.getOutEdgesOf(node).stream()
                    .filter(edge -> edge.kind() != FlowKind.OTHER)
                    .forEach(edge -> addEdge(node, edge));
        }
    }

    private void addEdge(Node node, FlowEdge outEdge) {
        addNode(node);
        addNode(outEdge.target());
        outEdges.put(node, outEdge);
        inEdges.put(outEdge.target(), outEdge);
    }

    private void addNode(Node node) {
        nodes.add(node);
        if(node instanceof VarNode varNode) {
            var2Node.put(varNode.getVar(), varNode);
        } else if(node instanceof InstanceFieldNode ifNode) {
            iField2Node.put(ifNode.getBase(), ifNode.getField(), ifNode);
        }
    }

    @Override
    public Set<Node> getPredsOf(Node node) {
        return Views.toMappedSet(getInEdgesOf(node), FlowEdge::source);
    }

    @Override
    public Set<FlowEdge> getInEdgesOf(Node node) {
        return inEdges.get(node);
    }

    @Override
    public Set<Node> getSuccsOf(Node node) {
        return Views.toMappedSet(getOutEdgesOf(node), FlowEdge::target);
    }

    @Override
    public Set<FlowEdge> getOutEdgesOf(Node node) {
        return outEdges.get(node);
    }

    @Override
    public Set<Node> getNodes() {
        return Collections.unmodifiableSet(nodes);
    }

    @Nullable
    public VarNode getVarNode(Var var) {
        return var2Node.get(var);
    }

    @Nullable
    public InstanceFieldNode getInstanceFieldNode(Obj base, JField field) {
        return iField2Node.get(base, field);
    }

    public Set<InstanceFieldNode> getInstanceFieldNode(Obj base) {
        if(iField2Node.containsKey(base)) {
            return Views.toMappedSet(iField2Node.get(base).values(), node -> node);
        }
        return Set.of();
    }
}
