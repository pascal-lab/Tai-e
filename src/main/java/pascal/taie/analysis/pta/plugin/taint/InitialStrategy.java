package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Sets;

import java.util.Collections;
import java.util.Set;

class InitialStrategy implements TransInferStrategy {


    private final Set<JMethod> methodsExistTransfers = Sets.newSet();

    private final Set<JClass> ignoreClasses = Sets.newSet();

    private final Set<JMethod> ignoreMethods = Sets.newSet();


    @Override
    public void setContext(InfererContext context) {
        context.config().transfers().forEach(t -> methodsExistTransfers.add(t.getMethod()));
        ignoreClasses.addAll(context.config().inferenceConfig().ignoreClasses());
        ignoreMethods.addAll(context.config().inferenceConfig().ignoreMethods());
    }

    @Override
    public Set<TaintTransfer> apply(JMethod method, Set<TaintTransfer> transfers) {
        // check whether this method or its class needs to be ignored
        if (ignoreMethods.contains(method) || ignoreClasses.contains(method.getDeclaringClass()))
            return Set.of();
        // check if exists transfer for this method
        if (methodsExistTransfers.contains(method))
            return Set.of();

        // add whole transfers for this method
        Set<TaintTransfer> taintTransferSet = Sets.newSet();
        TransferPoint fromPoint;
        TransferPoint toPoint;
        if (!method.isStatic()) {
            // add base-to-result transfer
            fromPoint = new TransferPoint(TransferPoint.Kind.VAR, InvokeUtils.BASE, null);
            toPoint = new TransferPoint(TransferPoint.Kind.VAR, InvokeUtils.RESULT, null);
            taintTransferSet.add(new InferredTransfer(method, fromPoint, toPoint, method.getReturnType(), getWeight()));
            // add arg-to-result transfer(s)
            for (int i = 0; i < method.getParamCount(); i++) {
                fromPoint = new TransferPoint(TransferPoint.Kind.VAR, i, null);
                toPoint = new TransferPoint(TransferPoint.Kind.VAR, InvokeUtils.BASE, null);
                taintTransferSet.add(new InferredTransfer(method, fromPoint, toPoint, method.getDeclaringClass().getType(), getWeight()));
            }
        }
        // add arg-to-result transfer(s)
        for (int i = 0; i < method.getParamCount(); i++) {
            fromPoint = new TransferPoint(TransferPoint.Kind.VAR, i, null);
            toPoint = new TransferPoint(TransferPoint.Kind.VAR, InvokeUtils.RESULT, null);
            taintTransferSet.add(new InferredTransfer(method, fromPoint, toPoint, method.getReturnType(), getWeight()));
        }

        return Collections.unmodifiableSet(taintTransferSet);
    }

    @Override
    public int getPriority() {
        return 0;
    }

    private int getWeight() {
        return 0;
    }
}
