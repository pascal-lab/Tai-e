package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.language.classes.JMethod;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class NameMatchingStrategy implements TransInferStrategy {

    private static final List<Rule> ruleList = List.of(
            new Rule(method -> method.getName().startsWith("get"), TransferPointType.BASE, TransferPointType.RESULT),
            new Rule(method -> method.getName().startsWith("new"), TransferPointType.ARG, TransferPointType.RESULT),
            new Rule(method -> method.getName().startsWith("create"), TransferPointType.ARG, TransferPointType.RESULT)
    );

    @Override
    public Set<TaintTransfer> apply(JMethod method, Set<TaintTransfer> transfers) {
        List<Rule> matchedRules = ruleList.stream().filter(rule -> rule.predicate().test(method)).toList();
        if (matchedRules.isEmpty()) {
            return Collections.unmodifiableSet(transfers);
        }

        return transfers.stream()
                .filter(tf -> matchAnyRule(tf, matchedRules))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public int getPriority() {
        return 1;
    }

    private TransferPointType getTransferPointType(TransferPoint transferPoint) {
        return switch (transferPoint.index()) {
            case InvokeUtils.BASE -> TransferPointType.BASE;
            case InvokeUtils.RESULT -> TransferPointType.RESULT;
            default -> TransferPointType.ARG;
        };
    }

    private boolean matchAnyRule(TaintTransfer transfer, List<Rule> rules) {
        TransferPointType from = getTransferPointType(transfer.getFrom());
        TransferPointType to = getTransferPointType(transfer.getTo());
        return rules.stream().anyMatch(rule -> rule.from == from && rule.to == to);
    }

    private enum TransferPointType {
        ARG, BASE, RESULT
    }

    private record Rule(Predicate<JMethod> predicate,
                        TransferPointType from,
                        TransferPointType to) {
    }
}
