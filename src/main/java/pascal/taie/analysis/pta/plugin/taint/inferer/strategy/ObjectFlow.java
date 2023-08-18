package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.element.InstanceField;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.taint.TaintManager;
import pascal.taie.analysis.pta.plugin.taint.inferer.InfererContext;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.analysis.pta.plugin.taint.inferer.TransferGenerator;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.plugin.util.StrategyUtils;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.TwoKeyMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ObjectFlow implements TransInferStrategy {

    private TwoKeyMap<CSCallSite, Integer, List<Integer>> possibleIndex;

    private CSManager csManager;

    private TaintManager taintManager;

    private TransferGenerator generator;

    @Override
    public void setContext(InfererContext context) {
        Solver solver = context.solver();
        generator = context.generator();
        csManager = solver.getCSManager();
        taintManager = context.taintManager();
        possibleIndex = Maps.newTwoKeyMap();
    }

    @Override
    public Set<InferredTransfer> generate(CSCallSite csCallSite, int index) {
        return getPossibleToIndex(csCallSite, index).stream()
                .map(toIndex -> generator.getTransfers(csCallSite, index, toIndex))
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<InferredTransfer> filter(CSCallSite csCallSite, int index, Set<InferredTransfer> transfers) {
        if(transfers.isEmpty()) {
            return Set.of();
        }
        List<Integer> possibleToIndex = getPossibleToIndex(csCallSite, index);
        if(possibleToIndex.isEmpty()) {
            return Set.of();
        }
        return transfers.stream()
                .filter(tf -> possibleToIndex.contains(tf.getTo().index()))
                .collect(Collectors.toUnmodifiableSet());
    }

    private List<Integer> getPossibleToIndex(CSCallSite csCallSite, int index) {
        List<Integer> possibleToIndex = possibleIndex.get(csCallSite, index);
        if(possibleToIndex == null) {
            possibleToIndex = new ArrayList<>();
            CSVar fromVar = StrategyUtils.getCSVar(csManager, csCallSite, index);
            assert fromVar != null;
            Set<InstanceField> fromFields = fromVar.objects()
                    .map(csManager::getInstanceFieldsOf)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
            List<Integer> toVarIndex = new ArrayList<>();

            Var returnVar = csCallSite.getCallSite().getResult();
            if(returnVar != null && returnVar.getType() instanceof ReferenceType) {
                toVarIndex.add(InvokeUtils.RESULT);
            }
            if(index != InvokeUtils.BASE && !csCallSite.getCallSite().isStatic()) {
                toVarIndex.add(InvokeUtils.BASE);
            }

            for(int toIndex : toVarIndex) {
                CSVar toVar = StrategyUtils.getCSVar(csManager, csCallSite, toIndex);
                assert toVar != null;
                Set<InstanceField> toFields = toVar.objects()
                        .map(csManager::getInstanceFieldsOf)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());
                if(varCanReach(fromVar, toFields) || fieldsCanReach(fromFields, toVar)) {
                    possibleToIndex.add(toIndex);
                }
            }
            possibleIndex.put(csCallSite, index, possibleToIndex);
        }
        return possibleToIndex;
    }

    // Field -> Var/Field
    private boolean fieldsCanReach(Set<InstanceField> fromFields, CSVar toVar) {
        Set<Pointer> targets = Sets.newSet();
        targets.add(toVar);
        toVar.objects()
                .map(csManager::getInstanceFieldsOf)
                .forEach(targets::addAll);
        return anyReach(fromFields, targets);
    }

    // Var -> Field
    private boolean varCanReach(CSVar fromVar, Set<InstanceField> toFields) {
        return anyReach(Set.of(fromVar), toFields);
    }

    private boolean anyReach(Set<? extends Pointer> from, Set<? extends Pointer> to) {
        if(from.isEmpty() || to.isEmpty()) {
            return false;
        }
        Set<Pointer> visited = Sets.newSet();
        Deque<Pointer> stack = new ArrayDeque<>(from);

        while(!stack.isEmpty()) {
            Pointer entry = stack.pop();
            if(visited.add(entry)) {
                if(to.contains(entry)) {
                    return true;
                }
                entry.getOutEdges().stream()
                        .map(PointerFlowEdge::target)
                        .filter(Predicate.not(visited::contains))
                        .forEach(stack::push);
            }
        }
        return false;
    }

    private boolean hasTaint(Pointer pointer) {
        return pointer.objects()
                .anyMatch(csObj -> taintManager.isTaint(csObj.getObject()));
    }
}
