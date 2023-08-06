package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

public class IgnoreInnerClass implements TransInferStrategy {

    public static final String ID = "ignore-inner-class";

    @Override
    public boolean shouldIgnore(JMethod method, int index) {
        JClass jClass = method.getDeclaringClass();
        return !jClass.isPublic() && jClass.hasOuterClass();
    }

    @Override
    public int getPriority() {
        return 15;
    }
}
