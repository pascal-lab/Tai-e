package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Lists;

import java.util.List;

record TransInferConfig(Confidence confidence,
                        List<JClass> ignoreClasses,
                        List<JMethod> ignoreMethods) {
    public static final TransInferConfig EMPTY = new TransInferConfig(
            Confidence.DISABLE, List.of(), List.of());

    public boolean inferenceEnable() {
        return this.confidence != Confidence.DISABLE;
    }

    public TransInferConfig mergeWith(TransInferConfig other) {
        return new TransInferConfig(
                confidence.compareTo(other.confidence) <= 0 ? confidence : other.confidence,
                Lists.concatDistinct(ignoreClasses, other.ignoreClasses),
                Lists.concatDistinct(ignoreMethods, other.ignoreMethods)
        );
    }

    @Override
    public String toString() {
        return "TransInferConfig{" +
                "confidence=" + confidence +
                ", ignoreClasses=" + ignoreClasses +
                ", ignoreMethods=" + ignoreMethods +
                '}';
    }

    enum Confidence {DISABLE, LOW, MEDIUM, HIGH}
}
