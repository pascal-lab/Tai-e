package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Lists;

import java.util.List;

record TransferInferenceConfig(Confidence confidence,
                               List<JClass> ignoreClasses,
                               List<JMethod> ignoreMethods) {
    enum Confidence { DISABLE, LOW, MEDIUM, HIGH }

    public static final TransferInferenceConfig EMPTY = new TransferInferenceConfig(
            Confidence.DISABLE, List.of(), List.of());

    public boolean inferenceEnable() {
        return this.confidence != Confidence.DISABLE;
    }

    public TransferInferenceConfig mergeWith(TransferInferenceConfig other) {
        return new TransferInferenceConfig(
                confidence.compareTo(other.confidence) <= 0 ? confidence : other.confidence,
                Lists.concatDistinct(ignoreClasses, other.ignoreClasses),
                Lists.concatDistinct(ignoreMethods, other.ignoreMethods)
        );
    }

    @Override
    public String toString() {
        return "TransferInferenceConfig{" +
                "confidence=" + confidence +
                ", ignoreClasses=" + ignoreClasses +
                ", ignoreMethods=" + ignoreMethods +
                '}';
    }
}
