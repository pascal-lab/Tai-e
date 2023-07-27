package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.language.classes.JMethod;

import java.util.Set;

public class ErrorClassFilter implements TransInferStrategy{


    @Override
    public Set<InferredTransfer> apply(JMethod method, Set<InferredTransfer> transfers) {
        return null;
    }



    @Override
    public int getPriority() {
        return 0;
    }
}
