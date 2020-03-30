package bamboo.util;

import soot.toolkits.graph.DirectedGraph;

import java.util.Iterator;
import java.util.List;

public class ReversedDirectedGraph<N> implements DirectedGraph<N> {

    private DirectedGraph<N> graph;

    public ReversedDirectedGraph(DirectedGraph<N> graph) {
        this.graph = graph;
    }

    @Override
    public List<N> getHeads() {
        return graph.getTails();
    }

    @Override
    public List<N> getTails() {
        return graph.getHeads();
    }

    @Override
    public List<N> getPredsOf(N s) {
        return graph.getSuccsOf(s);
    }

    @Override
    public List<N> getSuccsOf(N s) {
        return graph.getPredsOf(s);
    }

    @Override
    public int size() {
        return graph.size();
    }

    @Override
    public Iterator<N> iterator() {
        return graph.iterator();
    }
}
