package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.util.collection.Maps;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Consumer;

abstract class AbstractTransferInferer extends OnFlyHandler {

    protected static final List<TransInferStrategy> strategyList = List.of();

    protected final TaintConfig config;

    protected final Consumer<TaintTransfer> newTransferConsumer;

    protected final SortedSet<TransInferStrategy> enabledStrategies;

    protected boolean processed = false;

    AbstractTransferInferer(HandlerContext context, Consumer<TaintTransfer> newTransferConsumer) {
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
            InfererContext context = new InfererContext(solver, config);
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

                        meetResults(result).forEach(newTransferConsumer);
                    });
        }
    }

}
