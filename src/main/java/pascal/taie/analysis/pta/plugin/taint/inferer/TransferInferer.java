package pascal.taie.analysis.pta.plugin.taint.inferer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.flowgraph.FlowEdge;
import pascal.taie.analysis.graph.flowgraph.Node;
import pascal.taie.analysis.graph.flowgraph.ObjectFlowGraph;
import pascal.taie.analysis.graph.flowgraph.VarNode;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.plugin.taint.HandlerContext;
import pascal.taie.analysis.pta.plugin.taint.OnFlyHandler;
import pascal.taie.analysis.pta.plugin.taint.TFGBuilder;
import pascal.taie.analysis.pta.plugin.taint.TaintConfig;
import pascal.taie.analysis.pta.plugin.taint.TaintFlow;
import pascal.taie.analysis.pta.plugin.taint.TaintFlowGraph;
import pascal.taie.analysis.pta.plugin.taint.TaintTransfer;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.FilterAlias;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.IgnoreCollection;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.IgnoreException;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.IgnoreInnerClass;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.InitialStrategy;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.NameMatching;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.TransInferStrategy;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassMember;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.Edge;
import pascal.taie.util.graph.Reachability;
import pascal.taie.util.graph.ShortestPath;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class TransferInferer extends OnFlyHandler {

    protected static final Map<String, TransInferStrategy> strategyList;
    private static final Logger logger = LogManager.getLogger(TransferInferer.class);

    static {
        strategyList = Maps.newMap();
        strategyList.put(InitialStrategy.ID, new InitialStrategy());
//        strategyList.put(ObjectFlow.ID, new ObjectFlow());
//        strategyList.put(FilterAlias.ID, new FilterAlias());
        strategyList.put(IgnoreCollection.ID, new IgnoreCollection());
        strategyList.put(IgnoreInnerClass.ID, new IgnoreInnerClass());
        strategyList.put(IgnoreException.ID, new IgnoreException());
        strategyList.put(NameMatching.ID, new NameMatching());
//        strategyList.put(TypeTransfer.ID, new TypeTransfer());
    }

    protected final TaintConfig config;
    protected final Consumer<TaintTransfer> newTransferConsumer;
    protected final SortedSet<TransInferStrategy> enabledStrategies;
    protected final Set<InferredTransfer> addedTransfers = Sets.newSet();

    private final Map<Var, Integer> param2Index = Maps.newMap();

    private final Set<Var> taintParams = Sets.newSet();

    private final Set<Var> newTaintParams = Sets.newSet();

    private boolean initialized = false;

    TransferInferer(HandlerContext context, Consumer<TaintTransfer> newTransferConsumer) {
        super(context);
        this.config = context.config();
        this.newTransferConsumer = newTransferConsumer;
        this.enabledStrategies = initStrategy();
    }

    abstract SortedSet<TransInferStrategy> initStrategy();

    // For the first strategy, prevStrategy is null and prevOutput is an empty set.
    abstract Set<InferredTransfer> getNextInput(TransInferStrategy prevStrategy,
                                                TransInferStrategy nextStrategy,
                                                Set<InferredTransfer> prevOutput);

    abstract Set<InferredTransfer> meetResults(Map<TransInferStrategy, Set<InferredTransfer>> result);

    public Set<InferredTransfer> getInferredTrans() {
        return Collections.unmodifiableSet(addedTransfers);
    }

    @Override
    public void onNewMethod(JMethod method) {
        IR ir = method.getIR();
        for (int i = 0; i < method.getParamCount(); i++) {
            param2Index.put(ir.getParam(i), i);
        }
        if (!method.isStatic()) {
            param2Index.put(ir.getThis(), InvokeUtils.BASE);
        }
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        if (pts.objects().map(CSObj::getObject).anyMatch(manager::isTaint)) {
            Var taintVar = csVar.getVar();
            if (param2Index.containsKey(taintVar) && taintParams.add(taintVar)) {
                newTaintParams.add(taintVar);
            }
        }
    }

    @Override
    public void onBeforeFinish() {
        if (newTaintParams.isEmpty()) {
            return;
        }

        if (!initialized) {
            initialized = true;
            InfererContext context = new InfererContext(solver, manager, config);
            enabledStrategies.forEach(strategy -> strategy.setContext(context));
        }

       Set<InferredTransfer> newTransfers = Sets.newSet();

        for (Var param : newTaintParams) {
            JMethod method = param.getMethod();
            int index = param2Index.get(param);
            if (enabledStrategies.stream().anyMatch(strategy -> strategy.shouldIgnore(method, index))) {
                continue;
            }

            TransInferStrategy prev = null;
            Set<InferredTransfer> prevOutput = Set.of();
            Map<TransInferStrategy, Set<InferredTransfer>> result = Maps.newMap();

            for (TransInferStrategy strategy : enabledStrategies) {
                Set<InferredTransfer> input = getNextInput(prev, strategy, prevOutput);
                Set<InferredTransfer> output = strategy.apply(method, index, input);
                result.put(strategy, output);
                prevOutput = output;
                prev = strategy;
            }

            newTransfers.addAll(meetResults(result));
        }

        newTransfers.forEach(this::addNewTransfer);
        newTaintParams.clear();
    }

    private <T> T getStrategy(Class<T> strategyClass) {
        try {
            Field idField = strategyClass.getField("ID");
            String id = (String) idField.get(null);
            return (T) strategyList.get(id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(String.format("Failed to get analysis ID of %s",
                    strategyClass));
        }
    }

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
        TaintFlowGraph tfg = new TFGBuilder(solver.getResult(), taintFlows, manager, false, true).build();
        TransWeightHandler weightHandler = new TransWeightHandler(solver.getResult(), getInferredTrans());
        Set<Node> sourcesReachSink = tfg.getSourceNodes().stream()
                .filter(source -> tfg.getOutDegreeOf(source) > 0)
                .collect(Collectors.toSet());

        Set<InferredTransfer> taintRelatedTransfers = tfg.getNodes().stream()
                .map(tfg::getOutEdgesOf)
                .flatMap(Collection::stream)
                .map(weightHandler::getInferredTrans)
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet());
        logger.info("Taint related transfers count : {}", taintRelatedTransfers.size());
        logger.info("Taint related transfers (merge type) count : {}", taintRelatedTransfers.stream()
                .map(tf -> new Entry(tf.getMethod(), tf.getFrom().index(), tf.getTo().index()))
                .distinct()
                .count());

        Set<Node> sinkNodes = tfg.getSinkNodes();
        for (Node source : sourcesReachSink) {
            ShortestPath<Node> shortestPath = new ShortestPath<>(tfg, source,
                    edge -> weightHandler.getWeight((FlowEdge) edge), edge -> weightHandler.getCost((FlowEdge) edge));
            shortestPath.compute(ShortestPath.SSSPAlgorithm.DIJKSTRA);
            for (Node sink : sinkNodes) {
                List<Edge<Node>> path = shortestPath.getPath(sink);
                if (!path.isEmpty()) {
                    logger.info("\n{} -> {}:", source, sink);
                    path.stream()
                            .map(edge -> weightHandler.getInferredTrans((FlowEdge) edge))
                            .flatMap(Collection::stream)
                            .forEach(tf -> logger.info(transferToString(tf)));
                }
            }
        }
    }

    private String transferToString(InferredTransfer transfer) {
        return String.format(" - { method: \"%s\", from: %s, to: %s, type: %s }",
                transfer.getMethod().getSignature(),
                InvokeUtils.toString(transfer.getFrom().index()),
                InvokeUtils.toString(transfer.getTo().index()),
                transfer.getType().getName());
    }

    private void addNewTransfer(InferredTransfer transfer) {
        addedTransfers.add(transfer);
        newTransferConsumer.accept(transfer);
    }

    private record Entry(JMethod method, int from, int to) {

    }

}
