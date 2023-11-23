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
import pascal.taie.analysis.pta.plugin.taint.HandlerContext;
import pascal.taie.analysis.pta.plugin.taint.OnFlyHandler;
import pascal.taie.analysis.pta.plugin.taint.TFGInfoCollector;
import pascal.taie.analysis.pta.plugin.taint.TFGInfoCollector.TaintPath;
import pascal.taie.analysis.pta.plugin.taint.TaintConfig;
import pascal.taie.analysis.pta.plugin.taint.TaintObjectFlowEdge;
import pascal.taie.analysis.pta.plugin.taint.TaintTransfer;
import pascal.taie.analysis.pta.plugin.taint.TransferHandler;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.TransInferStrategy;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedHashSet;
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

    public void dump(TFGInfoCollector infoCollector) {
        File output = new File(World.get().getOptions().getOutputDir(), "transfer-inferer-output.log");
        logger.info("Dumping {}", output.getAbsolutePath());
        try (PrintStream out = new PrintStream(new FileOutputStream(output))) {
            for (var entry : infoCollector.getInfoKeySet()) {
                Var sourceVar = entry.first();
                Var sinkVar = entry.second();
                out.printf("%n%s -> %s:%n", varToString(sourceVar), varToString(sinkVar));
                TaintPath taintPath = infoCollector.getShortestTaintPath(sourceVar, sinkVar);
                dumpInferredTrans(out, taintPath);
                dumpShortestTaintPath(out, taintPath);
                dumpMinimumCutEdge(out, taintPath.graphHelper(), infoCollector.getMinimumCutEdges(sourceVar, sinkVar));
            }
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump {}", output.getAbsolutePath(), e);
        }
    }

    private void dumpInferredTrans(PrintStream out, TaintPath taintPath) {
        out.printf("%nInferred transfers:%n");
        TaintGraphHelper graphHelper = taintPath.graphHelper();
        taintPath.path().stream()
                 .flatMap(edge -> graphHelper.getInferredTransfers(edge).stream())
                 .forEach(tf -> out.println(transferToString(tf)));
    }

    private void dumpShortestTaintPath(PrintStream out, TaintPath taintPath) {
        out.printf("%nShortest taint path:%n");
        taintPath.path().forEach(edge -> out.println(edge.pointerFlowEdge()));
    }

    private void dumpMinimumCutEdge(PrintStream out,
                                    TaintGraphHelper helper,
                                    Set<TaintObjectFlowEdge> cutEdges) {
        out.printf("%nMinimum cut edges:%n");
        cutEdges.stream()
                .map(helper::getInferredTransfers)
                .flatMap(Collection::stream)
                .distinct()
                .forEach(tf -> out.println(transferToString(tf)));
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
}
