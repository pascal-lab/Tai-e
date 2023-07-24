package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Sets;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

class TypeReachability {

    private final TypeTransferGraph typeGraph;

    private final Set<Type> sinkTypes = Sets.newSet();

    private final Set<Type> typesCanReachSink = Sets.newSet(2048);

    public TypeReachability(TypeTransferGraph typeGraph) {
        this.typeGraph = typeGraph;
    }

    // Return new types that can reach sink
    public Set<Type> addSinkType(Type newSinkType) {
        if (sinkTypes.add(newSinkType)) {
            Set<Type> newTypesCanReachSink = getNewTypesCanReachSink(newSinkType);
            typesCanReachSink.addAll(newTypesCanReachSink);
            return newTypesCanReachSink;
        } else {
            return Set.of();
        }
    }

    public Set<Type> addTypeTransfer(Type from, Type to) {
        if (!typeGraph.hasEdge(from, to)) {
            typeGraph.addEdge(from, to);
            if (!typesCanReachSink.contains(from) && typesCanReachSink.contains(to)) {
                Set<Type> newTypesCanReachSink = getNewTypesCanReachSink(from);
                typesCanReachSink.addAll(newTypesCanReachSink);
                return newTypesCanReachSink;
            } else {
                return Set.of();
            }
        }
        return Set.of();
    }

    // Only need to process predecessors of 'from' which currently cannot reach sink type
    private Set<Type> getNewTypesCanReachSink(Type from) {
        Set<Type> visited = Sets.newSet();
        Deque<Type> stack = new ArrayDeque<>();
        stack.push(from);
        while (!stack.isEmpty()) {
            Type node = stack.pop();
            if (visited.add(node)) {
                typeGraph.getSuccsOf(node)
                        .stream()
                        .filter(type -> !visited.contains(type) && !typesCanReachSink.contains(type))
                        .forEach(stack::push);
            }
        }
        return visited;
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
