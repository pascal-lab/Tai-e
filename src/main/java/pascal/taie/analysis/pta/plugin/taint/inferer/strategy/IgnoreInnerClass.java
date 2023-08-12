package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.type.ClassType;

import java.util.Set;
import java.util.stream.Collectors;

public class IgnoreInnerClass implements TransInferStrategy {

    private boolean isPrivateInnerClass(JClass jClass) {
        return !jClass.isPublic() && jClass.hasOuterClass();
    }

    @Override
    public Set<InferredTransfer> filter(CSCallSite csCallSite, int index, Set<InferredTransfer> transfers) {
        return transfers.stream()
                .filter(tf -> !isPrivateInnerClass(tf.getMethod().getDeclaringClass()))
//                .filter(tf -> !(tf.getType() instanceof ClassType classType
//                        && isPrivateInnerClass(classType.getJClass())))
                .collect(Collectors.toUnmodifiableSet());
    }
}
