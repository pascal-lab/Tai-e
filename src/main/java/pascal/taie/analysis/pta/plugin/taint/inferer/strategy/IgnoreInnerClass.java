package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.language.classes.JClass;

import java.util.Set;
import java.util.stream.Collectors;

public class IgnoreInnerClass implements TransInferStrategy {

    @Override
    public Set<InferredTransfer> filter(CSCallSite csCallSite, int index, Set<InferredTransfer> transfers) {
        return transfers.stream()
                .filter(tf -> {
                    JClass jClass = tf.getMethod().getDeclaringClass();
                    return jClass.isPublic() || !jClass.hasOuterClass();
                })
                .collect(Collectors.toUnmodifiableSet());
    }
}
