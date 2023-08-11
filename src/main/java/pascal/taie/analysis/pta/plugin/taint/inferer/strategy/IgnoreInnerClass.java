package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

public class IgnoreInnerClass implements TransInferStrategy {

    @Override
    public boolean shouldIgnore(JMethod method, int index) {
        JClass jClass = method.getDeclaringClass();
        return !jClass.isPublic() && jClass.hasOuterClass();
    }
}
