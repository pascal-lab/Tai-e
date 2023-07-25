package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.Graph;

import java.util.Set;

public class TypeTransferGraph implements Graph<Type> {
    private final Set<Type> nodes = Sets.newSet(4096);
    private final MultiMap<Type, Type> successor = Maps.newMultiMap(4096);
    private final MultiMap<Type, Type> predecessor = Maps.newMultiMap(4096);

    public TypeTransferGraph() {
    }

    public void addEdge(Type from, Type to) {
        // Filter out-edges of java.lang.Object for better precision
        if (!from.getName().equals("java.lang.Object")) {
            successor.put(from, to);
            predecessor.put(to, from);
            nodes.add(from);
            nodes.add(to);
        }
    }

    @Override
    public Set<Type> getPredsOf(Type node) {
        return predecessor.get(node);
    }

    @Override
    public Set<Type> getSuccsOf(Type node) {
        return successor.get(node);
    }

    @Override
    public Set<Type> getNodes() {
        return nodes;
    }
}
