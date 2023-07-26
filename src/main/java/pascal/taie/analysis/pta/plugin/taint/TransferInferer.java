package pascal.taie.analysis.pta.plugin.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Consumer;

abstract class TransferInferer extends OnFlyHandler {

    private static final Logger logger = LogManager.getLogger(TransferInferer.class);

    protected static final List<TransInferStrategy> strategyList = List.of(
            new InitialStrategy(),
            new NameMatchingStrategy(),
            new TypeTransferStrategy()
    );

    protected final TaintConfig config;

    protected final Consumer<TaintTransfer> newTransferConsumer;

    protected final SortedSet<TransInferStrategy> enabledStrategies;

    protected boolean processed = false;

    protected Set<TaintTransfer> newTransfers = Sets.newSet();

    TransferInferer(HandlerContext context, Consumer<TaintTransfer> newTransferConsumer) {
        super(context);
        this.config = context.config();
        this.newTransferConsumer = newTransferConsumer;
        this.enabledStrategies = initStrategy();
    }

    abstract SortedSet<TransInferStrategy> initStrategy();

    // For the first strategy, prevStrategy is null and prevOutput is an empty set.
    abstract Set<TaintTransfer> getNextInput(TransInferStrategy prevStrategy,
                                             TransInferStrategy nextStrategy,
                                             Set<TaintTransfer> prevOutput);

    abstract Set<TaintTransfer> meetResults(Map<TransInferStrategy, Set<TaintTransfer>> result);

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
                        Set<TaintTransfer> prevOutput = Set.of();
                        Map<TransInferStrategy, Set<TaintTransfer>> result = Maps.newMap();

                        for(TransInferStrategy strategy : enabledStrategies) {
                            Set<TaintTransfer> input = getNextInput(prev, strategy, prevOutput);
                            Set<TaintTransfer> output = strategy.apply(method, input);
                            result.put(strategy, output);
                            prevOutput = output;
                            prev = strategy;
                        }

                        meetResults(result).forEach(this::addNewTransfer);
                    });
            logger.info("Total inferred transfers count :{}", newTransfers.size());
        }
    }

    private void addNewTransfer(TaintTransfer transfer) {
        newTransfers.add(transfer);
        newTransferConsumer.accept(transfer);
    }

}
