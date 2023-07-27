package pascal.taie.analysis.pta.plugin.taint.inferer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.plugin.taint.HandlerContext;
import pascal.taie.analysis.pta.plugin.taint.OnFlyHandler;
import pascal.taie.analysis.pta.plugin.taint.TaintConfig;
import pascal.taie.analysis.pta.plugin.taint.TaintTransfer;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.IgnoreCollection;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.IgnoreInnerClass;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.InitialStrategy;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.NameMatching;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.TransInferStrategy;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.TypeTransfer;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Consumer;

public abstract class TransferInferer extends OnFlyHandler {

    private static final Logger logger = LogManager.getLogger(TransferInferer.class);

    protected static final Map<String, TransInferStrategy> strategyList;

    protected final TaintConfig config;

    protected final Consumer<TaintTransfer> newTransferConsumer;

    protected final SortedSet<TransInferStrategy> enabledStrategies;

    protected boolean processed = false;

    protected Set<InferredTransfer> newTransfers = Sets.newSet();

    static {
        strategyList = Maps.newMap();
        strategyList.put(InitialStrategy.ID, new InitialStrategy());
        strategyList.put(IgnoreCollection.ID, new IgnoreCollection());
        strategyList.put(IgnoreInnerClass.ID, new IgnoreInnerClass());
        strategyList.put(NameMatching.ID, new NameMatching());
        strategyList.put(TypeTransfer.ID, new TypeTransfer());
    }

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

    public Set<InferredTransfer> getInferredTransfer() {
        return Collections.unmodifiableSet(newTransfers);
    }

    @Override
    public void onBeforeFinish() {
        if(!processed) {
            processed = true;
            InfererContext context = new InfererContext(solver, manager, config);
            enabledStrategies.forEach(strategy -> strategy.setContext(context));

            solver.getCallGraph().reachableMethods()
                    .map(CSMethod::getMethod)
                    .distinct()
                    .forEach(method -> {
                        TransInferStrategy prev = null;
                        Set<InferredTransfer> prevOutput = Set.of();
                        Map<TransInferStrategy, Set<InferredTransfer>> result = Maps.newMap();

                        for(TransInferStrategy strategy : enabledStrategies) {
                            Set<InferredTransfer> input = getNextInput(prev, strategy, prevOutput);
                            Set<InferredTransfer> output = strategy.apply(method, input);
                            result.put(strategy, output);
                            prevOutput = output;
                            prev = strategy;
                        }

                        meetResults(result).forEach(this::addNewTransfer);
                    });
            logger.info("Total inferred transfers count :{}", newTransfers.size());
        }
    }

    private void addNewTransfer(InferredTransfer transfer) {
        newTransfers.add(transfer);
        newTransferConsumer.accept(transfer);
    }

}
