package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.pta.plugin.taint.inferer.InfererContext;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.language.classes.JMethod;

import java.util.Set;

public interface TransInferStrategy extends Comparable<TransInferStrategy> {

    default void setContext(InfererContext context) {
    }

    /**
     * Apply this strategy on the given transfers related to a given method.
     *
     * @param method    All transfers are related to this method.
     * @param transfers Transfers to be filtered.
     * @return Filtered transfers
     */
    Set<InferredTransfer> apply(JMethod method, Set<InferredTransfer> transfers);

    int getPriority();

    @Override
    default int compareTo(TransInferStrategy other) {
        return Integer.compare(getPriority(), other.getPriority());
    }
}
