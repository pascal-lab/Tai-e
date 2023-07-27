package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.Collections;
import java.util.Set;

public class IgnoreInnerClass implements TransInferStrategy {

    public static final String ID = "ignore-inner-class";

    @Override
    public Set<InferredTransfer> apply(JMethod method, Set<InferredTransfer> transfers) {
        JClass jClass = method.getDeclaringClass();
        if(!jClass.isPublic() && jClass.hasOuterClass()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(transfers);
    }

    @Override
    public int getPriority() {
        return 15;
    }
}
