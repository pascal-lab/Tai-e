package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.taint.inferer.InfererContext;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.plugin.util.StrategyUtils;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.Reachability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TypeTransfer implements TransInferStrategy {

    public static String ID = "type-transfer";

    private static final int BASE = InvokeUtils.BASE;

    private static final int RESULT = InvokeUtils.RESULT;
    private final MultiMap<JMethod, Integer> sinkMethod2Index = Maps.newMultiMap();
    private final Set<Type> sinkTypes = Sets.newSet();
    private Solver solver;
    private CSManager csManager;
    private TypeTransferGraph typeTransferGraph;
    private Reachability<Type> typeReachability;

    private void processCallEdge(Edge<CSCallSite, CSMethod> edge) {
        CSCallSite csCallSite = edge.getCallSite();
        JMethod callee = edge.getCallee().getMethod();
        Set<Type> resultTypes = Set.of();
        Set<Type> baseTypes = Set.of();
        List<Set<Type>> argTypes = new ArrayList<>(callee.getParamCount());

        CSVar result = StrategyUtils.getCSVar(csManager, csCallSite, RESULT);
        if (result != null) {
            resultTypes = StrategyUtils.getTypes(solver, result);
        }

        if (!csCallSite.getCallSite().isStatic()) {
            CSVar base = StrategyUtils.getCSVar(csManager, csCallSite, BASE);
            baseTypes = StrategyUtils.getTypes(solver, base);
        }

        for (int i = 0; i < callee.getParamCount(); i++) {
            argTypes.add(StrategyUtils.getTypes(solver,
                    StrategyUtils.getCSVar(csManager, csCallSite, i)));
        }

        Set<Type> allArgsTypes = argTypes.stream().flatMap(Collection::stream).collect(Collectors.toSet());
        addTypeTransfer(allArgsTypes, baseTypes);
        addTypeTransfer(allArgsTypes, resultTypes);
        addTypeTransfer(baseTypes, resultTypes);

        // callee is sink method
        if (sinkMethod2Index.containsKey(callee)) {
            Set<Type> finalBaseTypes = baseTypes;
            Set<Type> finalResultTypes = resultTypes;
            Set<Type> newSinkTypes = sinkMethod2Index.get(callee).stream()
                    .map(index -> switch (index) {
                        case BASE -> finalBaseTypes;
                        case RESULT -> finalResultTypes;
                        default -> argTypes.get(index);
                    })
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
            sinkTypes.addAll(newSinkTypes);
        }
    }

    private void addTypeTransfer(Set<Type> from, Set<Type> to) {
        if (!from.isEmpty() && !to.isEmpty()) {
            for (Type fromType : from) {
                for (Type toType : to) {
                    typeTransferGraph.addEdge(fromType, toType);
                }
            }
        }
    }

    private boolean canReachSink(Type type) {
        return sinkTypes.stream()
                .anyMatch(sinkType -> typeReachability.nodesCanReach(sinkType).contains(type));
    }

    @Override
    public void setContext(InfererContext context) {
        this.solver = context.solver();
        this.csManager = solver.getCSManager();
        this.typeTransferGraph = new TypeTransferGraph();
        context.config().sinks().forEach(sink -> sinkMethod2Index.put(sink.method(), sink.index()));

        ClassHierarchy classHierarchy = solver.getHierarchy();
        classHierarchy.allClasses()
                .forEach(jClass -> {
                    Type type = jClass.getType();
                    Collection<JClass> subClasses = classHierarchy.getAllSubclassesOf(jClass);
                    subClasses.forEach(subClass -> typeTransferGraph.addEdge(subClass.getType(), type));
                });

        solver.getCallGraph().edges().forEach(this::processCallEdge);
        this.typeReachability = new Reachability<>(typeTransferGraph);
    }

    @Override
    public Set<InferredTransfer> apply(JMethod method, Set<InferredTransfer> transfers) {
        return transfers.stream()
                .filter(tf -> canReachSink(tf.getType()))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public int getPriority() {
        return 30;
    }
}
