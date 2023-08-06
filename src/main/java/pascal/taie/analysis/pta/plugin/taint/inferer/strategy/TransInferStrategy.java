package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.pta.plugin.taint.inferer.InfererContext;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.language.classes.JMethod;

import java.util.Collections;
import java.util.Set;

public interface TransInferStrategy extends Comparable<TransInferStrategy> {

    default void setContext(InfererContext context) {
    }

    default boolean shouldIgnore(JMethod method, int index) {
        return false;
    }

    /**
     * Apply this strategy on the given transfers related to a given method parameter.
     *
     * @param method    All transfers are related to this method.
     * @param index     All transfers are from this index.
     * @param transfers Transfers to be filtered.
     * @return New Transfers
     */
    default Set<InferredTransfer> apply(JMethod method, int index, Set<InferredTransfer> transfers) {
        return Collections.unmodifiableSet(transfers);
    }

    int getPriority();

    @Override
    default int compareTo(TransInferStrategy other) {
        return Integer.compare(getPriority(), other.getPriority());
    }
}
