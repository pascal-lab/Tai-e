package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.taint.inferer.InfererContext;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.util.collection.Sets;

import java.util.Set;
import java.util.stream.Collectors;

public class FilterAlias implements TransInferStrategy {

    public static final String ID = "filter-alias";

    private PointerAnalysisResult result;

    @Override
    public void setContext(InfererContext context) {
        result = context.solver().getResult();
    }

    @Override
    public Set<InferredTransfer> apply(JMethod method, int index, Set<InferredTransfer> transfers) {
        Var taintParam;
        if(index == InvokeUtils.BASE) {
            taintParam = method.getIR().getThis();
        } else {
            taintParam = method.getIR().getParam(index);
        }

        Set<Integer> nonAliasParamIndex = Sets.newHybridSet();
        if(method.getReturnType() instanceof ReferenceType) {
            if(method.getIR().getReturnVars().stream().anyMatch(v -> !result.mayAlias(v, taintParam))) {
                nonAliasParamIndex.add(InvokeUtils.RESULT);
            }
        }

        if(!method.isStatic()) {
            if(!result.mayAlias(method.getIR().getThis(), taintParam)) {
                nonAliasParamIndex.add(InvokeUtils.BASE);
            }
        }

        return transfers.stream()
                .filter(tf -> nonAliasParamIndex.contains(tf.getTo().index()))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public int getPriority() {
        return 3;
    }
}
