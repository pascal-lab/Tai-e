package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.taint.TaintConfig;
import pascal.taie.analysis.pta.plugin.taint.inferer.InfererContext;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.util.collection.Sets;

import java.util.Set;
import java.util.stream.Collectors;

public class ProcessString implements TransInferStrategy {

    private static final String STRING_CONFIG = "src/main/resources/string-transfers.yml";

    private Set<JClass> stringClasses;

    @Override
    public Set<InferredTransfer> preGenerate(Solver solver) {
        TaintConfig taintConfig = TaintConfig.loadConfig(STRING_CONFIG,
                solver.getHierarchy(),
                solver.getTypeSystem());
        return taintConfig.transfers().stream()
                .map(tf -> new InferredTransfer(tf.getMethod(), tf.getFrom(), tf.getTo(), tf.getType(), 0))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void setContext(InfererContext context) {
        Solver solver = context.solver();
        ClassHierarchy classHierarchy = solver.getHierarchy();
        stringClasses = Sets.newHybridSet();
        stringClasses.add(classHierarchy.getJREClass(ClassNames.STRING));
        stringClasses.add(classHierarchy.getJREClass(ClassNames.STRING_BUILDER));
        stringClasses.add(classHierarchy.getJREClass(ClassNames.STRING_BUFFER));
    }

    @Override
    public boolean shouldIgnore(CSCallSite csCallSite, int index) {
        return stringClasses.contains(csCallSite.getCallSite().getMethodRef().getDeclaringClass());
    }

    @Override
    public Set<InferredTransfer> filter(CSCallSite csCallSite, int index, Set<InferredTransfer> transfers) {
        return transfers.stream()
                .filter(tf -> !stringClasses.contains(tf.getMethod().getDeclaringClass()))
                .collect(Collectors.toUnmodifiableSet());
    }
}
