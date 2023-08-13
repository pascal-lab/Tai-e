package pascal.taie.analysis.pta.plugin.taint.inferer;

import pascal.taie.analysis.pta.plugin.taint.HandlerContext;
import pascal.taie.analysis.pta.plugin.taint.TaintTransfer;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.IgnoreCollection;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.IgnoreException;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.IgnoreInnerClass;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.NameMatching;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.ScopeFilter;

import java.util.function.Consumer;

public class HighTransferInferer extends TransferInferer {
    public HighTransferInferer(HandlerContext context, Consumer<TaintTransfer> newTransferConsumer) {
        super(context, newTransferConsumer);
    }

    @Override
    void initStrategy() {
        generateStrategies.add(new NameMatching());
        filterStrategies.add(new IgnoreCollection());
        filterStrategies.add(new IgnoreException());
        filterStrategies.add(new IgnoreInnerClass());
        filterStrategies.add(new ScopeFilter());
    }
}
