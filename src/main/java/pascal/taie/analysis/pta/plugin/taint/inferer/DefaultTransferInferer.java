package pascal.taie.analysis.pta.plugin.taint.inferer;

import pascal.taie.analysis.pta.plugin.taint.HandlerContext;
import pascal.taie.analysis.pta.plugin.taint.TaintTransfer;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.TransInferStrategy;
import pascal.taie.util.AnalysisException;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

public class DefaultTransferInferer extends TransferInferer {
    public DefaultTransferInferer(HandlerContext context, Consumer<TaintTransfer> newTransferConsumer) {
        super(context, newTransferConsumer);
    }

    @Override
    Set<TaintTransfer> getNextInput(TransInferStrategy prevStrategy,
                                    TransInferStrategy nextStrategy,
                                    Set<TaintTransfer> prevOutput) {
        return prevOutput;
    }

    @Override
    Set<TaintTransfer> meetResults(Map<TransInferStrategy, Set<TaintTransfer>> result) {
        TransInferStrategy lastStrategy = enabledStrategies.last();
        return result.get(lastStrategy);
    }

    @Override
    SortedSet<TransInferStrategy> initStrategy() {
        return switch (config.inferenceConfig().confidence()) {
            case DISABLE -> throw new AnalysisException();
            case LOW, MEDIUM, HIGH ->
                    Collections.unmodifiableSortedSet(new TreeSet<>(strategyList.values()));
        };
    }
}
