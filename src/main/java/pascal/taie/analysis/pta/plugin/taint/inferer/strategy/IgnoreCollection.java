package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.plugin.taint.inferer.InfererContext;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Sets;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class IgnoreCollection implements TransInferStrategy {

    private static final Set<String> COLLECTION_CLASS = Set.of(
            "java.util.Collection", "java.util.Map", "java.util.Dictionary");

    private ClassHierarchy hierarchy;

    private Set<JClass> collectionClasses;

    private Set<JClass> getCollectionClasses() {
        Set<JClass> collectionClasses = Sets.newSet();
        COLLECTION_CLASS.stream()
                .map(hierarchy::getJREClass)
                .map(hierarchy::getAllSubclassesOf)
                .flatMap(Collection::stream)
                .forEach(collectionClasses::add);
        Set<JClass> allCollectionClasses = Sets.newSet(collectionClasses);
        collectionClasses.forEach(c ->
                allCollectionClasses.addAll(getAllInnerClassesOf(c)));
        return allCollectionClasses;
    }

    private Set<JClass> getAllInnerClassesOf(JClass jclass) {
        Set<JClass> innerClasses = Sets.newHybridSet();
        hierarchy.getDirectInnerClassesOf(jclass).forEach(inner -> {
            innerClasses.add(inner);
            innerClasses.addAll(getAllInnerClassesOf(inner));
        });
        return innerClasses;
    }

    @Override
    public void setContext(InfererContext context) {
        hierarchy = context.solver().getHierarchy();
        collectionClasses = getCollectionClasses();
    }

    @Override
    public Set<InferredTransfer> filter(CSCallSite csCallSite, int index, Set<InferredTransfer> transfers) {
        return transfers.stream()
                .filter(tf -> !collectionClasses.contains(tf.getMethod().getDeclaringClass())
                        && !(tf.getType() instanceof ClassType classType
                        && collectionClasses.contains(classType.getJClass())))
                .collect(Collectors.toUnmodifiableSet());
    }
}
