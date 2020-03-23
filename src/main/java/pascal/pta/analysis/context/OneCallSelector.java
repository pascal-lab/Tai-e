package pascal.pta.analysis.context;

import pascal.pta.analysis.data.CSCallSite;
import pascal.pta.analysis.data.CSMethod;
import pascal.pta.analysis.data.CSObj;
import pascal.pta.element.Method;

/**
 * 1-call-site-sensitivity with no heap context.
 */
public class OneCallSelector implements ContextSelector {

    @Override
    public Context selectContext(CSCallSite callSite, Method callee) {
        return new OneContext<>(callSite.getCallSite());
    }

    @Override
    public Context selectContext(CSCallSite callSite, CSObj recv, Method callee) {
        return new OneContext<>(callSite.getCallSite());
    }

    @Override
    public Context selectHeapContext(CSMethod method, Object allocationSite) {
        return getDefaultContext();
    }
}
