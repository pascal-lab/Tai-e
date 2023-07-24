package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.Graph;

import java.util.List;
import java.util.Set;

public class TypeTransferGraph implements Graph<Type> {

    private static final List<Class<? extends Type>> concernedType = List.of(ClassType.class);
    private final Set<Type> nodes = Sets.newSet(4096);
    private final MultiMap<Type, Type> successor = Maps.newMultiMap(4096);
    private final MultiMap<Type, Type> predecessor = Maps.newMultiMap(4096);

    public TypeTransferGraph(ClassHierarchy classHierarchy) {
        classHierarchy.allClasses().forEach(
                jClass -> classHierarchy.getAllSubclassesOf(jClass)
                        .forEach(subClass -> addEdge(subClass.getType(), jClass.getType())));
    }

    public void addEdge(Type from, Type to) {
        // Filter out-edges of java.lang.Object for better precision
        if (concernedType.contains(from.getClass())
                && concernedType.contains(to.getClass())
                && from.getName().equals("java.lang.Object")) {
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
