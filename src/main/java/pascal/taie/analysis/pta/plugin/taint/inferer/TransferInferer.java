package pascal.taie.analysis.pta.plugin.taint.inferer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.plugin.taint.HandlerContext;
import pascal.taie.analysis.pta.plugin.taint.OnFlyHandler;
import pascal.taie.analysis.pta.plugin.taint.TOFGBuilder;
import pascal.taie.analysis.pta.plugin.taint.TPFGBuilder;
import pascal.taie.analysis.pta.plugin.taint.TaintConfig;
import pascal.taie.analysis.pta.plugin.taint.TaintFlow;
import pascal.taie.analysis.pta.plugin.taint.TaintNode;
import pascal.taie.analysis.pta.plugin.taint.TaintObjectFlowEdge;
import pascal.taie.analysis.pta.plugin.taint.TaintObjectFlowGraph;
import pascal.taie.analysis.pta.plugin.taint.TaintPointerFlowGraph;
import pascal.taie.analysis.pta.plugin.taint.TaintTransfer;
import pascal.taie.analysis.pta.plugin.taint.TransferHandler;
import pascal.taie.analysis.pta.plugin.taint.TransferPoint;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.TransInferStrategy;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.TwoKeyMap;
import pascal.taie.util.graph.MaxFlowMinCutSolver;
import pascal.taie.util.graph.ShortestPath;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class TransferInferer extends OnFlyHandler {
    private static final Logger logger = LogManager.getLogger(TransferInferer.class);

    private static final int BASE = InvokeUtils.BASE;

    private static final int RESULT = InvokeUtils.RESULT;

    protected final CSManager csManager;

    protected final TaintConfig config;
    protected final TransferHandler transferHandler;
    protected final LinkedHashSet<TransInferStrategy> generateStrategies = new LinkedHashSet<>();
    protected final LinkedHashSet<TransInferStrategy> filterStrategies = new LinkedHashSet<>();
    protected final Set<InferredTransfer> addedTransfers = Sets.newLinkedSet();
    private final MultiMap<CSVar, Pair<CSCallSite, Integer>> arg2Callsites = Maps.newMultiMap(4096);
    private final Set<CSVar> taintVars = Sets.newSet();
    private final Set<CSVar> newTaintVars = Sets.newSet();
    private LinkedHashSet<TransInferStrategy> strategies;
    private boolean initialized = false;

    TransferInferer(HandlerContext context, TransferHandler transferHandler) {
        super(context);
        this.csManager = solver.getCSManager();
        this.config = context.config();
        this.transferHandler = transferHandler;
        initStrategy();
        for (TransInferStrategy strategy : getStrategies()) {
            strategy.preGenerate(solver).forEach(this::addNewTransfer);
        }
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
                if (getStrategies().stream().anyMatch(strategy -> strategy.shouldIgnore(csCallSite, index))) {
                    continue;
                }
                Set<InferredTransfer> possibleTransfers = generateStrategies.stream()
                        .map(strategy -> strategy.generate(csCallSite, index))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toUnmodifiableSet());
                if (!possibleTransfers.isEmpty()) {
                    for (TransInferStrategy strategy : filterStrategies) {
                        if (possibleTransfers.isEmpty()) {
                            break;
                        }
                        possibleTransfers = strategy.filter(csCallSite, index, possibleTransfers);
                    }
                }
                newTransfers.addAll(possibleTransfers);
            }
        }

        newTransfers.forEach(this::addNewTransfer);
        newTaintVars.clear();
    }

    @Override
    public void onFinish() {
        getStrategies().forEach(TransInferStrategy::onFinish);
    }

    private LinkedHashSet<TransInferStrategy> getStrategies() {
        if (strategies == null) {
            strategies = new LinkedHashSet<>(generateStrategies);
            strategies.addAll(filterStrategies);
        }
        return strategies;
    }

    private void addNewTransfer(InferredTransfer transfer) {
        if (addedTransfers.add(transfer)) {
            transferHandler.addNewTransfer(transfer);
        }
    }

    public void collectInferredTrans(Set<TaintFlow> taintFlows) {
        File output = new File(World.get().getOptions().getOutputDir(), "transfer-inferer-output.log");
        logger.info("Dumping {}", output.getAbsolutePath());
        try (PrintStream out = new PrintStream(new FileOutputStream(output))) {
            dump(out, taintFlows);
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump to {}", output.getAbsolutePath(), e);
        }
    }

    private void dump(PrintStream out, Set<TaintFlow> taintFlows) {
        out.printf("Added transfers count : %d%n", addedTransfers.size());
        out.printf("Inferred transfers (merge type, ignore transfers with 0 weight) count: %d%n",
                countTransfers(addedTransfers));
        if (taintFlows.isEmpty()) {
            return;
        }
        TwoKeyMap<Var, Var, TaintPath> taintPaths = collectTaintPath(taintFlows);

        dumpTaintRelatedTrans(out, taintPaths);
        for (var entry : taintPaths.entrySet()) {
            out.printf("%n%s -> %s:%n", varToString(entry.key1()), varToString(entry.key2()));
            TaintPath taintPath = entry.value();
            dumpInferredTrans(out, taintPath);
            dumpShortestTaintPath(out, taintPath);
            dumpMinimumCutEdge(out, taintPath);
        }
    }

    // TODO: handle other call source type
    private TwoKeyMap<Var, Var, TaintPath> collectTaintPath(Set<TaintFlow> taintFlows) {
        TaintPointerFlowGraph tpfg = new TPFGBuilder(solver, manager, taintFlows, false, true).build();
        TaintGraphHelper.init(solver, tpfg, transferHandler.getTransfers());
        TOFGBuilder tofgBuilder = new TOFGBuilder(tpfg, solver, manager, config, taintFlows);

        Set<Pointer> sourcesReachSink = tpfg.getSourcePointers().stream()
                .filter(source -> tpfg.getOutDegreeOf(source) > 0)
                .collect(Collectors.toSet());

        TwoKeyMap<Var, Var, TaintPath> taintPaths = Maps.newTwoKeyMap();
        for (Pointer source : sourcesReachSink) {
            assert getTaintSet(source).size() == 1;
            CSObj taintObj = getTaintSet(source).iterator().next();
            TaintObjectFlowGraph tofg = tofgBuilder.build(source, taintObj);
            TaintGraphHelper graphHelper = new TaintGraphHelper(tofg);

            ShortestPath<TaintNode, TaintObjectFlowEdge> shortestPath = new ShortestPath<>(
                    tofg, tofg.getSourceNode(), graphHelper::getWeight, graphHelper::getCost);
            shortestPath.compute();

            for (TaintNode taintNode : tofg.getSinkNodes()) {
                int weight = shortestPath.getDistance(taintNode);
                if (weight != ShortestPath.INVALID_WEIGHT) {
                    List<TaintObjectFlowEdge> path = shortestPath.getPath(taintNode);
                    Var sourceVar = ((CSVar) source).getVar();
                    Var sinkVar = ((CSVar) taintNode.pointer()).getVar();
                    TaintPath oldPath = taintPaths.get(sourceVar, sinkVar);
                    if (oldPath == null
                            || oldPath.weight > weight
                            || (oldPath.weight == weight && oldPath.path.size() > path.size())) {
                        taintPaths.put(sourceVar, sinkVar,
                                new TaintPath(path, weight, tofg, taintNode, graphHelper));
                    }
                }
            }
        }
        return taintPaths;
    }

    private void dumpTaintRelatedTrans(PrintStream out, TwoKeyMap<Var, Var, TaintPath> taintPaths) {
        Set<InferredTransfer> taintTransfers = taintPaths.values().stream()
                .flatMap(taintPath -> taintPath.graphHelper.getAllInferredTransfers().stream())
                .collect(Collectors.toUnmodifiableSet());
        out.printf("%n%d taint related inferred transfers: %n", taintTransfers.size());
        out.printf("%d taint related inferred transfers (merge type, ignore transfers with 0 weight): %n",
                countTransfers(taintTransfers));
        taintTransfers.forEach(tf -> out.printf("%s%n", tf));
    }

    private void dumpInferredTrans(PrintStream out, TaintPath taintPath) {
        out.printf("%nInferred transfers:%n");
        TaintGraphHelper graphHelper = taintPath.graphHelper;
        taintPath.path.stream()
                .flatMap(edge -> graphHelper.getInferredTransfers(edge).stream())
                .forEach(tf -> out.println(transferToString(tf)));
    }

    private void dumpShortestTaintPath(PrintStream out, TaintPath taintPath) {
        out.printf("%nShortest taint path:%n");
        List<TaintObjectFlowEdge> path = taintPath.path;
        TaintGraphHelper graphHelper = taintPath.graphHelper;
        path.forEach(edge -> out.println(edge.pointerFlowEdge()));
        out.printf("%nNeeded transfers:%n");
        path.stream()
                .flatMap(edge -> graphHelper.getTransfers(edge).stream())
                .forEach(tf -> out.println(transferToString(tf)));
    }

    private void dumpMinimumCutEdge(PrintStream out, TaintPath taintPath) {
        out.printf("%nMinimum cut edges:%n");
        TaintGraphHelper helper = taintPath.graphHelper;
        MaxFlowMinCutSolver<TaintNode> minCut = new MaxFlowMinCutSolver<>(taintPath.tofg,
                taintPath.tofg.getSourceNode(),
                taintPath.sinkNode,
                edge -> helper.getCapacity((TaintObjectFlowEdge) edge));
        minCut.compute();
        minCut.getMinCutEdges().stream()
                .map(edge -> helper.getInferredTransfers(((TaintObjectFlowEdge) edge)))
                .flatMap(Collection::stream)
                .distinct()
                .forEach(tf -> out.println(transferToString(tf)));
    }

    private Set<CSObj> getTaintSet(Pointer pointer) {
        return pointer.objects()
                .filter(csObj -> manager.isTaint(csObj.getObject()))
                .collect(Sets::newHybridSet, Set::add, Set::addAll);
    }

    private String transferToString(TaintTransfer transfer) {
        return String.format(" - { method: \"%s\", from: %s, to: %s, type: %s }",
                transfer.getMethod().getSignature(),
                InvokeUtils.toString(transfer.getFrom().index()),
                InvokeUtils.toString(transfer.getTo().index()),
                transfer.getType().getName());
    }

    private String varToString(Var v) {
        return v.getMethod() + "/" + v.getName();
    }

    private long countTransfers(Set<InferredTransfer> transfers) {
        return transfers.stream()
                .filter(tf -> tf.getWeight() > 0)
                .map(tf -> new Entry(tf.getMethod(), tf.getFrom(), tf.getTo()))
                .distinct()
                .count();
    }

    private record Entry(JMethod method, TransferPoint from, TransferPoint to) {

    }

    private record TaintPath(List<TaintObjectFlowEdge> path,
                             int weight,
                             TaintObjectFlowGraph tofg,
                             TaintNode sinkNode,
                             TaintGraphHelper graphHelper) {

    }
}
