package bamboo.pta.analysis.context;

import bamboo.pta.analysis.data.CSCallSite;
import bamboo.pta.analysis.data.CSMethod;
import bamboo.pta.analysis.data.CSObj;
import bamboo.pta.element.Method;

/**
 * 1-object-sensitivity with no heap context.
 */
public class OneObjectSelector implements ContextSelector {

    @Override
    public Context selectContext(CSCallSite callSite, Method callee) {
        return callSite.getContext();
    }

    @Override
    public Context selectContext(CSCallSite callSite, CSObj recv, Method callee) {
        return new OneContext<>(recv.getObject());
    }

    @Override
    public Context selectHeapContext(CSMethod method, Object allocationSite) {
        return getDefaultContext();
    }
}
