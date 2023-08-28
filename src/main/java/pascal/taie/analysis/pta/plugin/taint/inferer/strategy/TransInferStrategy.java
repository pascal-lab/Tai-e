package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.taint.inferer.InfererContext;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;

import java.util.Collections;
import java.util.Set;

public interface TransInferStrategy {

    default Set<InferredTransfer> preGenerate(Solver solver) {
        return Set.of();
    }

    default void setContext(InfererContext context) {
    }

    default boolean shouldIgnore(CSCallSite csCallSite, int index) {
        return false;
    }

    default Set<InferredTransfer> generate(CSCallSite csCallSite, int index) {
        return Set.of();
    }

    /**
     * Apply this strategy on the given transfers related to a given method parameter.
     *
     * @param csCallSite    All transfers are related to this csCallSite.
     * @param index         All transfers are from this index.
     * @param transfers     Transfers to be filtered.
     * @return New Transfers
     */
    default Set<InferredTransfer> filter(CSCallSite csCallSite, int index, Set<InferredTransfer> transfers) {
        return Collections.unmodifiableSet(transfers);
    }

    default void onFinish() {

    }
}
