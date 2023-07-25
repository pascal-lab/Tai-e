package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.language.classes.JMethod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import pascal.taie.analysis.pta.plugin.util.InvokeUtils;

public class NameMatchingStrategy implements TransInferStrategy {


    private static final List<Rule> rulesList = List.of(
            new Rule(method -> method.getName().startsWith("get"), Type.BASE, Type.RESULT),
            new Rule(method -> method.getName().startsWith("new"), Type.ARGS, Type.RESULT),
            new Rule(method -> method.getName().startsWith("create"), Type.ARGS, Type.RESULT)
    );

    @Override
    public void setContext(InfererContext context) {
        //initialize the context
    }

    @Override
    public Set<TaintTransfer> apply(JMethod method, Set<TaintTransfer> transfers) {

        List<Rule> matchedRules = rulesList.stream().filter(rule -> rule.predicate().test(method)).toList();

        Set<TaintTransfer> result = new HashSet<>();

        matchedRules.forEach(rule -> transfers.stream()
                .filter(transfer -> TransferPoint2Type(transfer.getFrom()) == rule.from() && TransferPoint2Type(transfer.getTo()) == rule.to())
                .forEach(result::add));

        return result;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    private Type TransferPoint2Type(TransferPoint transferPoint) {
        return switch (transferPoint.index()) {
            case InvokeUtils.BASE -> Type.BASE;
            case InvokeUtils.RESULT -> Type.RESULT;
            default -> Type.ARGS;
        };
    }

    private enum Type {

        ARGS,
        BASE,
        RESULT
    }


    private record Rule(Predicate<JMethod> predicate, Type from,
                        Type to) {
    }


}
