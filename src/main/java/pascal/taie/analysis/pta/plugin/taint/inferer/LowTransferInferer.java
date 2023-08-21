package pascal.taie.analysis.pta.plugin.taint.inferer;

import pascal.taie.analysis.pta.plugin.taint.HandlerContext;
import pascal.taie.analysis.pta.plugin.taint.TaintTransfer;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.IgnoreCollection;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.IgnoreException;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.IgnoreInnerClass;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.InitialStrategy;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.MethodNameMatching;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.ScopeFilter;
import pascal.taie.analysis.pta.plugin.taint.inferer.strategy.TypeMatching;

import java.util.function.Consumer;

public class LowTransferInferer extends TransferInferer {
    public LowTransferInferer(HandlerContext context, Consumer<TaintTransfer> newTransferConsumer) {
        super(context, newTransferConsumer);
    }

    @Override
    void initStrategy() {
        generateStrategies.add(new InitialStrategy());
        filterStrategies.add(new ScopeFilter());
        filterStrategies.add(new IgnoreCollection());
        filterStrategies.add(new IgnoreException());
        filterStrategies.add(new IgnoreInnerClass());
        filterStrategies.add(new MethodNameMatching());
        filterStrategies.add(new TypeMatching());
    }
}
