package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.plugin.taint.inferer.InfererContext;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.analysis.pta.plugin.taint.inferer.TransferGenerator;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.util.collection.Sets;

import java.util.Set;

public class InitialStrategy implements TransInferStrategy {

    private static final int BASE = InvokeUtils.BASE;
    private static final int RESULT = InvokeUtils.RESULT;

    private TransferGenerator generator;

    @Override
    public void setContext(InfererContext context) {
        generator = context.generator();
    }

    @Override
    public Set<InferredTransfer> generate(CSCallSite csCallSite, int index) {
        Invoke callSite = csCallSite.getCallSite();
        if(index == RESULT) {
            return Set.of();
        } else if(index == BASE) {
            assert !callSite.isStatic();
            // base-to-result
            if(callSite.getResult() != null) {
                return generator.getTransfers(csCallSite, BASE, RESULT);
            }
            return Set.of();
        } else {
            Set<InferredTransfer> result = Sets.newSet();
            if (!callSite.isStatic()) {
                // arg-to-base
                result.addAll(generator.getTransfers(csCallSite, index, BASE));
            }
            // arg-to-result
            if(callSite.getResult() != null) {
                result.addAll(generator.getTransfers(csCallSite, index, RESULT));
            }
            return result;
        }
    }
}
