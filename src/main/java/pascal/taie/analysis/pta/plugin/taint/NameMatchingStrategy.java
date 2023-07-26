package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.language.classes.JMethod;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class NameMatchingStrategy implements TransInferStrategy {

    private static final List<Rule> IncludeRules = List.of(
            new Rule(method -> method.getName().startsWith("get"), TransferPointType.BASE, TransferPointType.RESULT, RuleType.INCLUDE),
            new Rule(method -> method.getName().startsWith("new"), TransferPointType.ARG, TransferPointType.RESULT, RuleType.INCLUDE),
            new Rule(method -> method.getName().startsWith("create"), TransferPointType.ARG, TransferPointType.RESULT, RuleType.INCLUDE)
    );

    private static final List<Rule> ExcludeRules = List.of(
            new Rule(method -> method.getName().startsWith("equals"), TransferPointType.ANY, TransferPointType.ANY, RuleType.EXCLUDE),
            new Rule(method -> method.getName().startsWith("hashCode"), TransferPointType.ANY, TransferPointType.ANY, RuleType.EXCLUDE),
            new Rule(method -> method.getName().startsWith("compareTo"), TransferPointType.ANY, TransferPointType.ANY, RuleType.EXCLUDE),
            new Rule(method -> method.getName().startsWith("set")
                    && method.getName().length() > 3
                    && Character.isUpperCase(method.getName().charAt(3)), TransferPointType.ARG, TransferPointType.BASE, RuleType.EXCLUDE),
            new Rule(method -> method.getName().startsWith("is")
                    && method.getName().length() > 2
                    && Character.isUpperCase(method.getName().charAt(2)), TransferPointType.ARG, TransferPointType.BASE, RuleType.EXCLUDE),
            new Rule(method -> method.getName().startsWith("has")
                    && method.getName().length() > 3
                    && Character.isUpperCase(method.getName().charAt(3)), TransferPointType.ARG, TransferPointType.BASE, RuleType.EXCLUDE),
            new Rule(method -> method.getName().startsWith("can")
                    && method.getName().length() > 3
                    && Character.isUpperCase(method.getName().charAt(3)), TransferPointType.ARG, TransferPointType.BASE, RuleType.EXCLUDE),
            new Rule(method -> method.getName().startsWith("should")
                    && method.getName().length() > 6
                    && Character.isUpperCase(method.getName().charAt(6)), TransferPointType.ARG, TransferPointType.BASE, RuleType.EXCLUDE),
            new Rule(method -> method.getName().startsWith("will")
                    && method.getName().length() > 4
                    && Character.isUpperCase(method.getName().charAt(4)), TransferPointType.ARG, TransferPointType.BASE, RuleType.EXCLUDE)
    );

    @Override
    public Set<TaintTransfer> apply(JMethod method, Set<TaintTransfer> transfers) {
        List<Rule> matchedIncludeRules = IncludeRules.stream().filter(rule -> rule.predicate().test(method)).toList();
        List<Rule> matchedExcludeRules = ExcludeRules.stream().filter(rule -> rule.predicate().test(method)).toList();
        if (matchedIncludeRules.isEmpty() && matchedExcludeRules.isEmpty()) {
            return Collections.unmodifiableSet(transfers);
        }

        return transfers.stream()
                .filter(tf -> matchAnyRule(tf, matchedIncludeRules))
                .filter(tf -> !matchAnyRule(tf, matchedExcludeRules))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public int getPriority() {
        return 1;
    }

    public int getWeight() {
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
        return rules.stream().anyMatch(rule ->  (rule.from == TransferPointType.ANY || rule.to == TransferPointType.ANY)
                                                || (rule.from == from && rule.to == to));
    }

    private enum TransferPointType {
        ANY, ARG, BASE, RESULT
    }

    private enum RuleType {
        INCLUDE, EXCLUDE
    }

    private record Rule(Predicate<JMethod> predicate, TransferPointType from, TransferPointType to,
                        RuleType type) {
    }
}
