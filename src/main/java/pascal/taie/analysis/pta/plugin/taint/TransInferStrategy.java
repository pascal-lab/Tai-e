package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.language.classes.JMethod;

import java.util.Set;

interface TransInferStrategy extends Comparable<TransInferStrategy> {

    default void setContext(InfererContext context) {
    }

    /**
     * Apply this strategy on the given transfers related to a given method.
     * @param method All transfers are related to this method.
     * @param transfers Transfers to be filtered.
     * @return Filtered transfers
     */
    Set<TaintTransfer> apply(JMethod method, Set<TaintTransfer> transfers);

    int getPriority();

    @Override
    default int compareTo(TransInferStrategy other) {
        return Integer.compare(getPriority(), other.getPriority());
    }
}
