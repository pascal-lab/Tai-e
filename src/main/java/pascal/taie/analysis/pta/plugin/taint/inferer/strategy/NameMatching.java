package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.pta.plugin.taint.TaintTransfer;
import pascal.taie.analysis.pta.plugin.taint.TransferPoint;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.language.classes.JMethod;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class NameMatching implements TransInferStrategy {

    private static final List<Rule> rules = List.of(
            // Allow
            new Rule(method -> startsWithWord(method.getName(), "get"), TransferPointType.BASE, TransferPointType.RESULT, RuleType.ALLOW),
            new Rule(method -> startsWithWord(method.getName(), "new"), TransferPointType.ARG, TransferPointType.RESULT, RuleType.ALLOW),
            new Rule(method -> startsWithWord(method.getName(), "create"), TransferPointType.ARG, TransferPointType.RESULT, RuleType.ALLOW),
            // Deny
            new Rule(method -> method.getName().startsWith("equals"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(method -> method.getName().startsWith("hashCode"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(method -> method.getName().startsWith("compareTo"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(method -> startsWithWord(method.getName(), "should"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(method -> startsWithWord(method.getName(), "match"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(method -> startsWithWord(method.getName(), "will"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(method -> startsWithWord(method.getName(), "set"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(method -> startsWithWord(method.getName(), "is"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(method -> startsWithWord(method.getName(), "has"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(method -> startsWithWord(method.getName(), "can"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(method -> startsWithWord(method.getName(), "needs"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(method -> startsWithWord(method.getName(), "check"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(method -> startsWithWord(method.getName(), "may"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY)
    );
    public static final String ID = "name-matching";

    private static boolean startsWithWord(String text, String word) {
        if (text.startsWith(word)) {
            return text.length() == word.length() || Character.isUpperCase(text.charAt(word.length()));
        }
        return false;
    }

    @Override
    public Set<InferredTransfer> apply(JMethod method, int index, Set<InferredTransfer> transfers) {
        List<Rule> matchedRules = rules.stream().filter(rule -> rule.predicate().test(method)).toList();
        if (matchedRules.isEmpty()) {
            return Collections.unmodifiableSet(transfers);
        }

        return transfers.stream()
                .filter(tf -> matchAllRules(tf, matchedRules))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public int getPriority() {
        return 20;
    }

    private TransferPointType getTransferPointType(TransferPoint transferPoint) {
        return switch (transferPoint.index()) {
            case InvokeUtils.BASE -> TransferPointType.BASE;
            case InvokeUtils.RESULT -> TransferPointType.RESULT;
            default -> TransferPointType.ARG;
        };
    }

    private boolean matchAllRules(TaintTransfer transfer, List<Rule> rules) {
        TransferPointType from = getTransferPointType(transfer.getFrom());
        TransferPointType to = getTransferPointType(transfer.getTo());
        return rules.stream().allMatch(rule -> {
            boolean match = rule.from == TransferPointType.ANY || rule.to == TransferPointType.ANY
                    || (rule.from == from && rule.to == to);
            return (rule.type == RuleType.ALLOW) == match;
        });
    }

    private enum TransferPointType {
        ANY, ARG, BASE, RESULT
    }

    private enum RuleType {
        ALLOW, DENY
    }

    private record Rule(Predicate<JMethod> predicate,
                        TransferPointType from,
                        TransferPointType to,
                        RuleType type) {
    }
}
