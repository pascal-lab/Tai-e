package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.core.cs.element.*;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.taint.TaintManager;
import pascal.taie.analysis.pta.plugin.taint.TaintTransfer;
import pascal.taie.analysis.pta.plugin.taint.inferer.InfererContext;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.analysis.pta.plugin.util.StrategyUtils;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.MultiMap;

import java.util.Set;
import java.util.stream.Collectors;


public class NaturalPropagation implements TransInferStrategy {

    public static String ID = "natural-propagation";

    private Solver solver;

    private TaintManager taintManager;

    private CSManager csManager;

    private MultiMap<JMethod, CSCallSite> method2CSCallSite;

    @Override
    public void setContext(InfererContext context) {
        TransInferStrategy.super.setContext(context);
        solver = context.solver();
        taintManager = context.taintManager();
        csManager = solver.getCSManager();
        method2CSCallSite = StrategyUtils.getMethod2CSCallSites(solver.getCallGraph());
    }

    @Override
    public Set<InferredTransfer> apply(JMethod method, Set<InferredTransfer> transfers) {
        return transfers.stream().filter(tf -> !canNaturalPropagation(method, tf)).collect(Collectors.toUnmodifiableSet());
    }


    boolean hasSameTaint(CSObj from, CSObj to) {
        return taintManager.isTaint(from.getObject()) && taintManager.isTaint(to.getObject()) && from.equals(to);
    }

    boolean hasSameTaint(CSCallSite csCallSite, TaintTransfer transfer) {
        CSVar fromCSVar = StrategyUtils.getCSVar(csManager, csCallSite, transfer.getFrom().index());
        CSVar toCSVar = StrategyUtils.getCSVar(csManager, csCallSite, transfer.getTo().index());

        if (fromCSVar == null || toCSVar == null) {
            return false;
        }

        Set<CSObj> fromPTS = fromCSVar.getObjects();
        Set<CSObj> toPTS = toCSVar.getObjects();

        return fromPTS.stream()
                .anyMatch(fromPT -> toPTS.stream().anyMatch(toPT -> hasSameTaint(fromPT, toPT)));

    }

    boolean canNaturalPropagation(JMethod method, TaintTransfer transfer) {

        Set<CSCallSite> callSites = method2CSCallSite.get(method);

        return callSites.stream().filter(callSite -> hasSameTaint(callSite, transfer)).count() > callSites.size() / 2;
    }


    @Override
    public int getPriority() {
        return 40;
    }

    public int getWeight() {
        return 1;
    }


}
