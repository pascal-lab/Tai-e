package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.plugin.taint.TaintTransfer;
import pascal.taie.analysis.pta.plugin.taint.TransferPoint;
import pascal.taie.analysis.pta.plugin.taint.inferer.InfererContext;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.analysis.pta.plugin.taint.inferer.TransferGenerator;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.util.collection.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class NameMatching implements TransInferStrategy {

    private static final List<Rule> rules = List.of(
            // Allow
            new Rule(name -> startsWithWord(name, "get"), TransferPointType.BASE, TransferPointType.RESULT, RuleType.ALLOW),
            new Rule(name -> startsWithWord(name, "new"), TransferPointType.ARG, TransferPointType.RESULT, RuleType.ALLOW),
            new Rule(name -> startsWithWord(name, "create"), TransferPointType.ARG, TransferPointType.RESULT, RuleType.ALLOW),
            // Deny
            new Rule(name -> name.startsWith("equals"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(name -> name.startsWith("hashCode"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(name -> name.startsWith("compareTo"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(name -> startsWithWord(name, "should"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(name -> startsWithWord(name, "match"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(name -> startsWithWord(name, "will"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(name -> startsWithWord(name, "set"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(name -> startsWithWord(name, "is"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(name -> startsWithWord(name, "has"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(name -> startsWithWord(name, "can"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(name -> startsWithWord(name, "needs"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(name -> startsWithWord(name, "check"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(name -> startsWithWord(name, "may"), TransferPointType.ANY, TransferPointType.ANY, RuleType.DENY),
            new Rule(name -> name.equals("log"), TransferPointType.ARG, TransferPointType.BASE, RuleType.DENY),
            new Rule(name -> name.equals("trace"), TransferPointType.ARG, TransferPointType.BASE, RuleType.DENY),
            new Rule(name -> name.equals("debug"), TransferPointType.ARG, TransferPointType.BASE, RuleType.DENY),
            new Rule(name -> name.equals("info"), TransferPointType.ARG, TransferPointType.BASE, RuleType.DENY),
            new Rule(name -> name.equals("warn"), TransferPointType.ARG, TransferPointType.BASE, RuleType.DENY),
            new Rule(name -> name.equals("error"), TransferPointType.ARG, TransferPointType.BASE, RuleType.DENY)
    );

    private TransferGenerator generator;

    private static boolean startsWithWord(String text, String word) {
        if (text.startsWith(word)) {
            return text.length() == word.length() || Character.isUpperCase(text.charAt(word.length()));
        }
        return false;
    }

    @Override
    public void setContext(InfererContext context) {
        generator = context.generator();
    }

    @Override
    public Set<InferredTransfer> generate(CSCallSite csCallSite, int index) {
        List<Rule> matchedRules = rules.stream()
                .filter(rule -> rule.type == RuleType.ALLOW
                        && rule.methodName().test(csCallSite.getCallSite().getMethodRef().getName())
                        && matchTransferPointType(rule.from, index))
                .toList();
        if (matchedRules.isEmpty()) {
            return Set.of();
        }
        return matchedRules.stream()
                .map(rule -> generator.getTransfers(csCallSite, index, getTransferPointIndex(rule.to)))
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<InferredTransfer> filter(CSCallSite csCallSite, int index, Set<InferredTransfer> transfers) {
        List<Rule> matchedRules = rules.stream()
                .filter(rule -> rule.methodName().test(csCallSite.getCallSite().getMethodRef().getName()))
                .toList();
        if (matchedRules.isEmpty()) {
            return Collections.unmodifiableSet(transfers);
        }

        return transfers.stream()
                .filter(tf -> matchAllRules(tf, matchedRules))
                .collect(Collectors.toUnmodifiableSet());
    }

    private TransferPointType getTransferPointType(TransferPoint transferPoint) {
        return switch (transferPoint.index()) {
            case InvokeUtils.BASE -> TransferPointType.BASE;
            case InvokeUtils.RESULT -> TransferPointType.RESULT;
            default -> TransferPointType.ARG;
        };
    }

    private int getTransferPointIndex(TransferPointType transferPointType) {
        return switch (transferPointType) {
            case BASE -> InvokeUtils.BASE;
            case RESULT -> InvokeUtils.RESULT;
            default -> throw new UnsupportedOperationException();
        };
    }

    private boolean matchTransferPointType(TransferPointType transferPointType, int index) {
        return switch (transferPointType) {
            case ANY -> true;
            case ARG -> index > 0;
            case BASE -> index == InvokeUtils.BASE;
            case RESULT -> index == InvokeUtils.RESULT;
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

    private record Rule(Predicate<String> methodName,
                        TransferPointType from,
                        TransferPointType to,
                        RuleType type) {
    }
}
