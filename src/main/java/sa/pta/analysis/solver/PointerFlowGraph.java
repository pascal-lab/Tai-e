package sa.pta.analysis.solver;

import sa.pta.analysis.data.Pointer;
import sa.pta.element.Type;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PointerFlowGraph {

    private Set<Pointer> pointers = new HashSet<>();

    private Map<Pointer, Set<PointerFlowEdge>> edges = new HashMap<>();

    private Map<Pointer, Set<Pointer>> successors = new HashMap<>();

    public void addNewPointer(Pointer pointer) {
        pointers.add(pointer);
    }

    public boolean addEdge(Pointer from, Pointer to, PointerFlowEdge.Kind kind) {
        return addEdge(from, to, kind, null);
    }

    public boolean addEdge(Pointer from, Pointer to, PointerFlowEdge.Kind kind, Type type) {
        addNewPointer(from);
        addNewPointer(to);
        if (successors.computeIfAbsent(from, k -> new HashSet<>()).add(to)) {
            edges.computeIfAbsent(from, k -> new HashSet<>())
                    .add(new PointerFlowEdge(kind, from, to, type));
            return true;
        } else {
            return false;
        }
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
