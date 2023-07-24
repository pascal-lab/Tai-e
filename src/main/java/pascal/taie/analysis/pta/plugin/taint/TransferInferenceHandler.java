package pascal.taie.analysis.pta.plugin.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class TransferInferenceHandler extends OnFlyHandler {

    private static final Logger logger = LogManager.getLogger(TransferInferenceHandler.class);
    public static int ARG = Integer.MIN_VALUE;
    private static final List<Rule> rules = List.of(
            new Rule(method -> method.getName().startsWith("get"), InvokeUtils.BASE, InvokeUtils.RESULT),
            new Rule(method -> method.getName().startsWith("new"), ARG, InvokeUtils.RESULT),
            new Rule(method -> method.getName().startsWith("create"), ARG, InvokeUtils.RESULT)
    );
    private final Set<JMethod> hasTransferMethods = Sets.newSet();

    private final Set<JMethod> methods = Sets.newSet();

    /**
     * Map from a method parameter to its CSVar. A parameter is indicated by a method
     * and an index.
     */
    private final MultiMap<Param, CSVar> param2CSVar = Maps.newMultiMap();

    /**
     * Map from a CSVar to a method parameter. A parameter is indicated by a method
     * and an index.
     */
    private final MultiMap<CSVar, Param> csVar2Param = Maps.newMultiMap();

    /**
     * All CSVar related to sink method
     */
    private final Set<CSVar> sinkCSVars = Sets.newSet();

    private final MultiMap<JMethod, Sink> sinkMethod2Sink = Maps.newMultiMap();

    /**
     * Map from a parameter to its type
     */
    private final MultiMap<Param, Type> param2ClassType = Maps.newMultiMap();

    private final Set<TaintTransfer> inferenceTransfers = Sets.newSet();

    private final Consumer<TaintTransfer> newTransferAction;

    private final CSManager csManager;

    private final TaintConfig taintConfig;

    private final List<JClass> ignoreClasses;

    private final List<JMethod> ignoreMethods;

    private final TypeReachability typeReachability;

    TransferInferenceHandler(HandlerContext context, Consumer<TaintTransfer> newTransferAction) {
        super(context);
        this.csManager = solver.getCSManager();
        this.newTransferAction = newTransferAction;
        this.taintConfig = context.config();
        this.ignoreClasses = taintConfig.inferenceConfig().ignoreClasses();
        this.ignoreMethods = taintConfig.inferenceConfig().ignoreMethods();
        taintConfig.transfers().stream()
                .map(TaintTransfer::method)
                .forEach(hasTransferMethods::add);
        this.typeReachability = new TypeReachability(new TypeTransferGraph(solver.getHierarchy()));
        taintConfig.sinks().forEach(sink -> sinkMethod2Sink.put(sink.method(), sink));
    }

    private List<TaintTransfer> getTransfers(JMethod method, int from, int to) {
        if ((from == InvokeUtils.BASE || to == InvokeUtils.BASE) && method.isStatic()) {
            return List.of();
        }
        Set<Type> fromTypes = getParamClassType(method, from);
        Set<Type> toTypes = getParamClassType(method, to).stream()
                .filter(typeReachability::canReachSink)
                .collect(Collectors.toSet());
        if (fromTypes.isEmpty() || toTypes.isEmpty()) {
            return List.of();
        }

        TransferPoint fromPoint = new TransferPoint(TransferPoint.Kind.VAR, from, null);
        TransferPoint toPoint = new TransferPoint(TransferPoint.Kind.VAR, to, null);
        return toTypes.stream()
                .map(toType -> new TaintTransfer(method, fromPoint, toPoint, toType))
                .toList();
    }

    private List<TaintTransfer> getTransfers(JMethod method, Rule rule) {
        if (rule.from == ARG && rule.to == ARG) {
            // Currently, we do not support this pattern since it is imprecise
            return List.of();
        }

        List<TaintTransfer> transfers = new ArrayList<>();
        if (rule.from == ARG) {
            for (int i = 0; i < method.getParamCount(); i++) {
                transfers.addAll(getTransfers(method, i, rule.to));
            }
        } else if (rule.to == ARG) {
            for (int i = 0; i < method.getParamCount(); i++) {
                transfers.addAll(getTransfers(method, rule.from, i));
            }
        } else {
            transfers.addAll(getTransfers(method, rule.from, rule.to));
        }
        return transfers;
    }

    private List<TaintTransfer> getTransfers(JMethod method) {
        List<TaintTransfer> transfers = new ArrayList<>();
        transfers.addAll(getArgToBaseTransfers(method));
        transfers.addAll(getBaseToResultTransfers(method));
        transfers.addAll(getArgToResultTransfers(method));
        return transfers;
    }

    private List<TaintTransfer> getArgToBaseTransfers(JMethod method) {
        if (method.isStatic()) {
            return List.of();
        }
        List<TaintTransfer> transfers = new ArrayList<>();
        for (int i = 0; i < method.getParamCount(); i++) {
            transfers.addAll(getTransfers(method, i, InvokeUtils.BASE));
        }
        return transfers;
    }

    private List<TaintTransfer> getBaseToResultTransfers(JMethod method) {
        if (method.isStatic()) {
            return List.of();
        }
        return getTransfers(method, InvokeUtils.BASE, InvokeUtils.RESULT);
    }

    private List<TaintTransfer> getArgToResultTransfers(JMethod method) {
        List<TaintTransfer> transfers = new ArrayList<>();
        for (int i = 0; i < method.getParamCount(); i++) {
            transfers.addAll(getTransfers(method, i, InvokeUtils.RESULT));
        }
        return transfers;
    }

    private void addTransfer(TaintTransfer transfer) {
        if (inferenceTransfers.add(transfer)) {
            newTransferAction.accept(transfer);
        }
    }

    private void processMethod(JMethod method) {
        if (hasTransferMethods.contains(method)
                || ignoreMethods.contains(method)
                || ignoreClasses.contains(method.getDeclaringClass())) {
            return;
        }
        if (typeReachability.getTypesCanReachSink().isEmpty()) {
            return;
        }
        List<Rule> matchedRules = rules.stream().filter(rule -> rule.predicate().test(method)).toList();
        // If any rule is matched
        if (!matchedRules.isEmpty()) {
            for (Rule rule : matchedRules) {
                getTransfers(method, rule).forEach(this::addTransfer);
            }
        } else {
            getTransfers(method).forEach(this::addTransfer);
        }
    }

    private Set<Type> getParamClassType(JMethod method, int index) {
        if(index == ARG) {  // Merge all types in args
            Set<Type> result = Sets.newSet();
            for(int i = 0; i < method.getParamCount(); i++) {
                result.addAll(param2ClassType.get(new Param(method, i)));
            }
            return result;
        } else {
            return param2ClassType.get(new Param(method, index));
        }
    }

    // Return new types that can reach sink
    private Set<Type> processNewParamTypes(Param param, Set<Type> type) {
        JMethod method = param.method;
        int index = param.index;
        Set<Type> existParamTypes = getParamClassType(param.method, param.index);
        type = type.stream().filter(Predicate.not(existParamTypes::contains)).collect(Collectors.toSet());
        Set<Type> newTypesCanReachSink = Sets.newSet();
        switch (index) {
            case InvokeUtils.BASE -> {
                assert !method.isStatic();
                // base-to-result
                newTypesCanReachSink.addAll(
                        addTypeTransfers(type, getParamClassType(method, InvokeUtils.RESULT)));
                // arg-to-base
                newTypesCanReachSink.addAll(addTypeTransfers(getParamClassType(method, ARG), type));
            }
            case InvokeUtils.RESULT -> {
                // arg-to-result
                newTypesCanReachSink.addAll(addTypeTransfers(getParamClassType(method, ARG), type));
                if(!method.isStatic()) {
                    // base-to-result
                    newTypesCanReachSink.addAll(
                            addTypeTransfers(getParamClassType(method, InvokeUtils.BASE), type));
                }
            }
            default -> {
                newTypesCanReachSink.addAll(
                        addTypeTransfers(type, getParamClassType(method, InvokeUtils.RESULT)));
                if(!method.isStatic()) {
                    newTypesCanReachSink.addAll(
                            addTypeTransfers(type, getParamClassType(method, InvokeUtils.BASE)));
                }
            }
        }
        if (newTypesCanReachSink.isEmpty()) {
            return Set.of();
        }
        return newTypesCanReachSink;
    }

    private Set<Type> addTypeTransfers(Set<Type> fromTypes, Set<Type> toTypes) {
        Set<Type> result = Sets.newSet();
        for(Type from : fromTypes) {
            for(Type to : toTypes) {
                result.addAll(typeReachability.addTypeTransfer(from, to));
            }
        }
        return result;
    }

    @Nullable
    private CSVar getCSVar(CSCallSite csCallSite, int index) {
        Invoke callSite = csCallSite.getCallSite();
        Var var = InvokeUtils.getVar(callSite, index);
        if (var == null) {
            return null;
        }
        return csManager.getCSVar(csCallSite.getContext(), var);
    }

    private void processNewCSVarOfParam(Param param, CSVar csVar) {
        if (csVar != null) {
            param2CSVar.put(param, csVar);
            csVar2Param.put(csVar, param);
        }
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        CSCallSite csCallSite = edge.getCallSite();
        Context context = csCallSite.getContext();
        Invoke callSite = csCallSite.getCallSite();
        JMethod callee = edge.getCallee().getMethod();
        for (int i = 0; i < callee.getParamCount(); i++) {
            processNewCSVarOfParam(new Param(callee, i), getCSVar(csCallSite, i));
        }
        if (!callSite.isStatic()) {
            processNewCSVarOfParam(new Param(callee, InvokeUtils.BASE),
                    getCSVar(csCallSite, InvokeUtils.BASE));
        }
        processNewCSVarOfParam(new Param(callee, InvokeUtils.RESULT),
                getCSVar(csCallSite, InvokeUtils.RESULT));

        sinkMethod2Sink.get(callee)
                .forEach(sink -> {
                    Var sinkVar = InvokeUtils.getVar(callSite, sink.index());
                    CSVar csSinkVar = csManager.getCSVar(context, sinkVar);
                    sinkCSVars.add(csSinkVar);
                    // TODO: update sink types
                });
    }


    @Override
    public void onNewMethod(JMethod method) {
        methods.add(method);
        processMethod(method);
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        Set<Param> params = csVar2Param.get(csVar);
        if(params.isEmpty()) {
            return;
        }
        Set<Type> newTypes = pts.objects()
                .map(CSObj::getObject)
                .map(Obj::getType)
                .filter(type -> type instanceof ClassType)
                .collect(Collectors.toSet());

        params.forEach(param -> param2ClassType.putAll(param, newTypes));

        Set<Type> newTypesCanReachSink = Sets.newSet();
        if (sinkCSVars.contains(csVar)) {
            newTypes.forEach(newSinkType ->
                    newTypesCanReachSink.addAll(typeReachability.addSinkType(newSinkType)));
        }

        if (typeReachability.getTypesCanReachSink().isEmpty()) {
            return;
        }

        params.stream()
                .map(param -> processNewParamTypes(param, newTypes))
                .flatMap(Collection::stream)
                .forEach(newTypesCanReachSink::add);

        if (!newTypesCanReachSink.isEmpty()) {
            for (JMethod method : methods) {
                Set<Type> resultTypes = getParamClassType(method, InvokeUtils.RESULT);
                if (Sets.haveOverlap(resultTypes, newTypesCanReachSink)) {
                    processMethod(method);
                } else if (!method.isStatic()) {
                    Set<Type> baseTypes = getParamClassType(method, InvokeUtils.BASE);
                    if (Sets.haveOverlap(baseTypes, newTypesCanReachSink)) {
                        processMethod(method);
                    }
                }
            }
        }
    }

    @Override
    public void onFinish() {
        logger.info("Inference transfer count: {}", inferenceTransfers.size());
    }

    private record Rule(Predicate<JMethod> predicate, int from, int to) {
    }

    private record Param(JMethod method, int index) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Param param = (Param) o;
            return index == param.index && Objects.equals(method, param.method);
        }

        @Override
        public int hashCode() {
            return Objects.hash(method, index);
        }
    }
}
