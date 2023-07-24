package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.Reachability;

import java.util.Set;

class TypeReachability {

    private final TypeTransferGraph typeGraph;

    private final Set<Type> sinkTypes = Sets.newSet();

    private final Set<Type> typesCanReachSink = Sets.newSet(2048);

    public TypeReachability(TypeTransferGraph typeGraph) {
        this.typeGraph = typeGraph;
    }

    // Return new types that can reach sink
    public Set<Type> addSinkTypes(Set<Type> newSinkTypes) {
        Set<Type> newTypesCanReachSink = Sets.newSet();
        Reachability<Type> reachability = new Reachability<>(typeGraph);
        for (Type type : newSinkTypes) {
            if (sinkTypes.add(type)) {
                newTypesCanReachSink.addAll(reachability.nodesCanReach(type));
            }
        }
        newTypesCanReachSink.removeAll(typesCanReachSink);
        typesCanReachSink.addAll(newTypesCanReachSink);
        return newTypesCanReachSink;
    }

    public Set<Type> addTypeTransfer(Type from, Type to) {
        if(!typeGraph.hasEdge(from, to)) {
            typeGraph.addEdge(from, to);
            Set<Type> newTypesCanReachSink = Sets.newSet();
            if(!typesCanReachSink.contains(from) && typesCanReachSink.contains(to)) {
                newTypesCanReachSink.addAll(new Reachability<>(typeGraph).nodesCanReach(from));
            }
            newTypesCanReachSink.removeAll(typesCanReachSink);
            typesCanReachSink.addAll(newTypesCanReachSink);
            return newTypesCanReachSink;
        }
        return Set.of();
    }

    public boolean canReachSink(Type type) {
        return typesCanReachSink.contains(type);
    }

    public Set<Type> getTypesCanReachSink() {
        return typesCanReachSink;
    }

    public Set<Type> getSinkTypes() {
        return sinkTypes;
    }
}
