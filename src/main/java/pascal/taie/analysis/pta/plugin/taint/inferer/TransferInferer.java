package pascal.taie.analysis.pta.plugin.taint.inferer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.plugin.taint.HandlerContext;
import pascal.taie.analysis.pta.plugin.taint.OnFlyHandler;
import pascal.taie.analysis.pta.plugin.taint.TPFGBuilder;
import pascal.taie.analysis.pta.plugin.taint.TaintConfig;
import pascal.taie.analysis.pta.plugin.taint.TaintFlow;
import pascal.taie.analysis.pta.plugin.taint.TaintObjectFlowGraph;
import pascal.taie.analysis.pta.plugin.taint.TaintPointerFlowGraph;
import pascal.taie.analysis.pta.plugin.taint.TaintTransfer;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.TransInferStrategy;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class TransferInferer extends OnFlyHandler {
    private static final Logger logger = LogManager.getLogger(TransferInferer.class);

    private static final int BASE = InvokeUtils.BASE;

    private static final int RESULT = InvokeUtils.RESULT;

    protected final CSManager csManager;

    protected final TaintConfig config;
    protected final Consumer<TaintTransfer> newTransferConsumer;
    protected final LinkedHashSet<TransInferStrategy> generateStrategies = new LinkedHashSet<>();
    protected final LinkedHashSet<TransInferStrategy> filterStrategies = new LinkedHashSet<>();
    protected final Set<InferredTransfer> addedTransfers = Sets.newSet();
    private final MultiMap<CSVar, Pair<CSCallSite, Integer>> arg2Callsites = Maps.newMultiMap(4096);
    private final Set<CSVar> taintVars = Sets.newSet();
    private final Set<CSVar> newTaintVars = Sets.newSet();
    private LinkedHashSet<TransInferStrategy> strategies;
    private boolean initialized = false;

    TransferInferer(HandlerContext context, Consumer<TaintTransfer> newTransferConsumer) {
        super(context);
        this.csManager = solver.getCSManager();
        this.config = context.config();
        this.newTransferConsumer = newTransferConsumer;
        initStrategy();
    }

    abstract void initStrategy();

    public Set<InferredTransfer> getInferredTrans() {
        return Collections.unmodifiableSet(addedTransfers);
    }

    @Override
    public void onNewCallEdge(pascal.taie.analysis.graph.callgraph.Edge<CSCallSite, CSMethod> edge) {
        if (edge.getKind() == CallKind.OTHER) {
            return;
        }
        CSCallSite csCallSite = edge.getCallSite();
        Invoke invoke = csCallSite.getCallSite();
        for (int i = 0; i < invoke.getInvokeExp().getArgCount(); i++) {
            arg2Callsites.put(getCSVar(csCallSite, i), new Pair<>(csCallSite, i));
        }
        if (!invoke.isStatic() && !invoke.isDynamic()) {
            arg2Callsites.put(getCSVar(csCallSite, BASE), new Pair<>(csCallSite, BASE));
        }
        if (invoke.getResult() != null) {
            arg2Callsites.put(getCSVar(csCallSite, RESULT), new Pair<>(csCallSite, RESULT));
        }
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        if (pts.objects().map(CSObj::getObject).anyMatch(manager::isTaint)) {
            if (taintVars.add(csVar)) {
                newTaintVars.add(csVar);
            }
        }
    }

    private CSVar getCSVar(CSCallSite csCallSite, int index) {
        Context context = csCallSite.getContext();
        Invoke callSite = csCallSite.getCallSite();
        return csManager.getCSVar(context, InvokeUtils.getVar(callSite, index));
    }

    @Override
    public void onBeforeFinish() {
        if (newTaintVars.isEmpty()) {
            return;
        }

        if (!initialized) {
            initialized = true;
            TransferGenerator generator = new TransferGenerator(solver);
            InfererContext context = new InfererContext(solver, manager, config, generator);
            getStrategies().forEach(strategy -> strategy.setContext(context));
        }

        Set<InferredTransfer> newTransfers = Sets.newSet();

        for (CSVar csArg : newTaintVars) {
            for (Pair<CSCallSite, Integer> entry : arg2Callsites.get(csArg)) {
                // Currently, we ignore invoke dynamic
                CSCallSite csCallSite = entry.first();
                int index = entry.second();
                if (csCallSite.getCallSite().isDynamic()
                        || index == RESULT) {
                    continue;
                }
                Set<InferredTransfer> possibleTransfers = generateStrategies.stream()
                        .map(strategy -> strategy.generate(csCallSite, index))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toUnmodifiableSet());
                if (!possibleTransfers.isEmpty()) {
                    for (TransInferStrategy strategy : filterStrategies) {
                        possibleTransfers = strategy.filter(csCallSite, index, possibleTransfers);
                    }
                }
                newTransfers.addAll(possibleTransfers);
            }
        }

        newTransfers.forEach(this::addNewTransfer);
        newTaintVars.clear();
    }

    private LinkedHashSet<TransInferStrategy> getStrategies() {
        if (strategies == null) {
            strategies = new LinkedHashSet<>(generateStrategies);
            strategies.addAll(filterStrategies);
        }
        return strategies;
    }

    /**
     *
     */
    public void collectInferredTrans(Set<TaintFlow> taintFlows) {
        logger.info("Total inferred transfers count : {}", addedTransfers.size());
        logger.info("Inferred transfers (merge type) count: {}", addedTransfers.stream()
                .map(tf -> new Entry(tf.getMethod(), tf.getFrom().index(), tf.getTo().index()))
                .distinct()
                .count());
        if (taintFlows.isEmpty()) {
            return;
        }
        logger.info("\nTransfer inferer output:");
        TaintPointerFlowGraph tpfg = new TPFGBuilder(solver, manager, taintFlows, false, true).build();
        TransWeightHandler weightHandler = new TransWeightHandler(solver.getCallGraph(),
                tpfg, csManager, getInferredTrans());

        Set<InferredTransfer> taintRelatedTransfers = tpfg.getNodes().stream()
                .map(tpfg::getOutEdgesOf)
                .flatMap(Collection::stream)
                .map(weightHandler::getInferredTrans)
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet());
        logger.info("Taint related transfers count : {}", taintRelatedTransfers.size());
        logger.info("Taint related transfers (merge type) count : {}", taintRelatedTransfers.stream()
                .map(tf -> new Entry(tf.getMethod(), tf.getFrom().index(), tf.getTo().index()))
                .distinct()
                .count());

        Set<Pointer> sourcesReachSink = tpfg.getSourcePointers().stream()
                .filter(source -> tpfg.getOutDegreeOf(source) > 0)
                .collect(Collectors.toSet());
        Set<Pointer> sinkPointers = tpfg.getSinkPointers();
        for (Pointer source : sourcesReachSink) {
            assert getTaintSet(source).size() == 1;
            CSObj taintObj = getTaintSet(source).iterator().next();
            for (Pointer sink : sinkPointers) {
                TaintObjectFlowGraph tofg = new TaintObjectFlowGraph(tpfg, source, taintObj, sink, solver);
                ShortestTaintPath shortestTaintPath = new ShortestTaintPath(tofg, source,
                        weightHandler::getWeight,
                        weightHandler::getCost,
                        solver);
                shortestTaintPath.compute();
                List<PointerFlowEdge> path = shortestTaintPath.getPath(sink);
                if (!path.isEmpty()) {
                    logger.info("\n{} -> {}:", source, sink);
                    path.stream()
                            .map(weightHandler::getInferredTrans)
                            .flatMap(Collection::stream)
                            .forEach(tf -> logger.info(transferToString(tf)));
                }
            }
        }
    }

    private Set<CSObj> getTaintSet(Pointer pointer) {
        return pointer.objects()
                .filter(csObj -> manager.isTaint(csObj.getObject()))
                .collect(Sets::newHybridSet, Set::add, Set::addAll);
    }

    private String transferToString(InferredTransfer transfer) {
        return String.format(" - { method: \"%s\", from: %s, to: %s, type: %s }",
                transfer.getMethod().getSignature(),
                InvokeUtils.toString(transfer.getFrom().index()),
                InvokeUtils.toString(transfer.getTo().index()),
                transfer.getType().getName());
    }

    private void addNewTransfer(InferredTransfer transfer) {
        if (addedTransfers.add(transfer)) {
            newTransferConsumer.accept(transfer);
        }
    }

    private record Entry(JMethod method, int from, int to) {

    }

}
