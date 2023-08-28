package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.plugin.taint.TaintTransfer;
import pascal.taie.analysis.pta.plugin.taint.TransferPoint;
import pascal.taie.analysis.pta.plugin.taint.inferer.InfererContext;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.analysis.pta.plugin.taint.inferer.TransferGenerator;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Based on the functionality,
 * functions can generally be divided into the following categories:
 * 1. Access Methods    (get)   base -> result
 * 2. Mutator Methods   (set)   arg  -> base
 * 3. Initialization Methods    arg  -> result
 * 4. Conversion Methods        arg  -> result  / base -> result
 * 5. Validation Methods        ignore
 * 6. Comparison Methods        ignore
 * 7. Action Methods            ignore
 * 8. Calculation Methods       ignore
 */
public class MethodNameMatching implements TransInferStrategy {

    private static final List<String> base2Result = List.of(
            // Access methods
            "get", "retrieve", "fetch", "obtain", "read",
            "acquire", "query", "receive", "access",

            // Conversion methods
            "toString"
    );

    private static final List<String> arg2Base = List.of(
            // Mutator methods
            "set", "update", "change", "modify", "assign",
            "alter", "edit", "replace"
    );

    private static final List<String> arg2Result = List.of(
            // Initialization methods
            "new", "create", "initialize", "generate", "build",
            "construct", "instantiate", "prepare",

            // Conversion methods
            "to", "from", "as", "parse", "convert",
            "cast", "deserialize", "extract", "normalize", "interpolate",
            "transform"
    );

    private static final List<String> ignoreNames = List.of(
            // Validation methods
            "should", "is", "has", "can", "validate",
            "check", "ensure", "verify", "match", "confirm",
            "assert", "test", "will", "need", "may", "must",
            "contains", "meets",

            // Comparison methods
            "compareTo", "equals",

            // Action methods
            "run", "start", "stop", "pause", "resume",
            "toggle", "enable", "disable", "shutdown", "reset",
            "restart", "zoom", "scroll", "show", "hide",
            "lock", "unlock", "discard", "on", "handle",
            "notify", "trigger",

            // Log methods in action methods
            "log", "trace", "debug", "info", "warn", "error",

            // Special case
            "hashCode"
    );

    private static final List<Rule> allowedRules = new ArrayList<>();

    private static final List<Rule> deniedRules = new ArrayList<>();

    private TransferGenerator generator;

    static {
        for(String prefix : base2Result) {
            allowedRules.add(new Rule(name -> startsWithWord(name, prefix),
                    TransferPointType.BASE,
                    TransferPointType.RESULT,
                    RuleType.ALLOW));
        }

        for(String prefix : arg2Base) {
            allowedRules.add(new Rule(name -> startsWithWord(name, prefix),
                    TransferPointType.ARG,
                    TransferPointType.BASE,
                    RuleType.ALLOW));
        }

        for(String prefix : arg2Result) {
            allowedRules.add(new Rule(name -> startsWithWord(name, prefix),
                    TransferPointType.ARG,
                    TransferPointType.RESULT,
                    RuleType.ALLOW));
        }
    }

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
    public boolean shouldIgnore(CSCallSite csCallSite, int index) {
        String name = csCallSite.getCallSite().getMethodRef().getName();
        return ignoreNames.stream().anyMatch(ignoreName -> startsWithWord(name, ignoreName));
    }

    @Override
    public Set<InferredTransfer> generate(CSCallSite csCallSite, int index) {
        List<Rule> matchedRules = allowedRules.stream()
                .filter(rule -> rule.methodName().test(csCallSite.getCallSite().getMethodRef().getName())
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
        List<Rule> matchedRules = deniedRules.stream()
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
