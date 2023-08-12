package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.plugin.taint.inferer.InfererContext;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Sets;

import java.util.Set;
import java.util.stream.Collectors;

public class IgnoreException implements TransInferStrategy {

    private Set<JClass> throwableSubClasses;

    @Override
    public void setContext(InfererContext context) {
        ClassHierarchy hierarchy = context.solver().getHierarchy();
        JClass throwableClass = hierarchy.getJREClass("java.lang.Throwable");
        throwableSubClasses = Sets.newSet(hierarchy.getAllSubclassesOf(throwableClass));
    }

    @Override
    public Set<InferredTransfer> filter(CSCallSite csCallSite, int index, Set<InferredTransfer> transfers) {
        return transfers.stream()
                .filter(tf -> !throwableSubClasses.contains(tf.getMethod().getDeclaringClass())
                        && !(tf.getType() instanceof ClassType classType
                        && throwableSubClasses.contains(classType.getJClass())))
                .collect(Collectors.toUnmodifiableSet());
    }
}
