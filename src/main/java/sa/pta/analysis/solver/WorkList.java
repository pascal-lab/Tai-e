package sa.pta.analysis.solver;

import sa.callgraph.Edge;
import sa.pta.analysis.data.CSCallSite;
import sa.pta.analysis.data.CSMethod;
import sa.pta.analysis.data.Pointer;
import sa.pta.set.PointsToSet;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

class WorkList {

    private Queue<Entry> pointerEntries = new LinkedList<>();

    private Set<Edge<CSCallSite, CSMethod>> callEdges = new LinkedHashSet<>();

    boolean hasPointerEntries() {
        return !pointerEntries.isEmpty();
    }

    void addPointerEntry(Pointer pointer, PointsToSet pointsToSet) {
        addPointerEntry(new Entry(pointer, pointsToSet));
    }

    void addPointerEntry(Entry entry) {
        pointerEntries.add(entry);
    }

    Entry pollPointerEntry() {
        return pointerEntries.poll();
    }

    class Entry {

        final Pointer pointer;

        final PointsToSet pointsToSet;

        public Entry(Pointer pointer, PointsToSet pointsToSet) {
            this.pointer = pointer;
            this.pointsToSet = pointsToSet;
        }
    }

    boolean hasCallEdges() {
        return !callEdges.isEmpty();
    }

    void addCallEdge(Edge<CSCallSite, CSMethod> edge) {
        callEdges.add(edge);
    }

    Edge<CSCallSite, CSMethod> pollCallEdge() {
        Edge<CSCallSite, CSMethod> edge = callEdges.iterator().next();
        callEdges.remove(edge);
        return edge;
    }

    boolean isEmpty() {
        return pointerEntries.isEmpty() && callEdges.isEmpty();
    }
}
