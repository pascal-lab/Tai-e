package sa.pta.analysis.solver;

import sa.pta.analysis.data.Pointer;
import sa.pta.element.Type;
import sa.pta.set.PointsToSetFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class PointerFlowGraph {

    private PointsToSetFactory setFactory;

    private Set<Pointer> pointers = new HashSet<>();

    private Map<Pointer, Set<PointerFlowEdge>> edges = new HashMap<>();

    private Map<Pointer, Set<Pointer>> successors = new HashMap<>();

    PointerFlowGraph(PointsToSetFactory setFactory) {
        this.setFactory = setFactory;
    }
    
    public void addNewPointer(Pointer pointer) {
        if (pointers.add(pointer)) {
            pointer.setPointsToSet(setFactory.makePointsToSet());
        }
    }
    
    public boolean addEdge(Pointer from, Pointer to, PointerFlowEdge.Kind kind) {
        addNewPointer(from);
        addNewPointer(to);
        if (successors.computeIfAbsent(from, k -> new HashSet<>()).add(to)) {
            edges.computeIfAbsent(from, k -> new HashSet<>())
                    .add(new PointerFlowEdge(kind, from, to));
            return true;
        } else {
            return false;
        }
    }

    public boolean addCastEdge(Pointer from, Pointer to, Type type) {
        addNewPointer(from);
        addNewPointer(to);
        successors.computeIfAbsent(from, k-> new HashSet<>()).add(to);
        return edges.computeIfAbsent(from, k -> new HashSet<>())
                .add(new PointerFlowEdge(PointerFlowEdge.Kind.CAST, from, to, type));
    }

    public Set<PointerFlowEdge> getOutEdgesOf(Pointer pointer) {
        return edges.getOrDefault(pointer, Collections.emptySet());
    }

    public Set<Pointer> getPointers() {
        return pointers;
    }

    public Iterator<PointerFlowEdge> getEdges() {
        return edges.values()
                .stream()
                .flatMap(Set::stream)
                .iterator();
    }
}
