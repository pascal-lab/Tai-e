package pascal.taie.analysis.pta.plugin.taint.inferer;

import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Lists;

import java.util.List;

public record TransInferConfig(Confidence confidence,
                               Scope scope,
                               List<String> appPackages,
                               List<JClass> ignoreClasses,
                               List<JMethod> ignoreMethods,
                               List<Type> ignoreTypes) {
    public static final TransInferConfig EMPTY = new TransInferConfig(
            Confidence.DISABLE, Scope.APP, List.of(), List.of(), List.of(), List.of());

    public boolean inferenceEnable() {
        return this.confidence != Confidence.DISABLE;
    }

    public TransInferConfig mergeWith(TransInferConfig other) {
        return new TransInferConfig(
                confidence.compareTo(other.confidence) <= 0 ? confidence : other.confidence,
                scope.compareTo(other.scope) <= 0 ? scope : other.scope,
                Lists.concatDistinct(appPackages, other.appPackages),
                Lists.concatDistinct(ignoreClasses, other.ignoreClasses),
                Lists.concatDistinct(ignoreMethods, other.ignoreMethods),
                Lists.concatDistinct(ignoreTypes, other.ignoreTypes)
        );
    }

    @Override
    public String toString() {
        return "TransInferConfig{" +
                "confidence=" + confidence +
                ", scope=" + scope +
                ", appPackages=" + appPackages +
                ", ignoreClasses=" + ignoreClasses +
                ", ignoreMethods=" + ignoreMethods +
                ", ignoreTypes=" + ignoreTypes +
                '}';
    }

    public enum Confidence {DISABLE, LOW, MEDIUM, HIGH}

    public enum Scope {APP, APP_LIB, ALL}
}
