package bamboo.pta.analysis.context;

import bamboo.pta.analysis.data.CSCallSite;
import bamboo.pta.analysis.data.CSMethod;
import bamboo.pta.analysis.data.CSObj;
import bamboo.pta.element.Method;

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
