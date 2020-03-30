package bamboo.pta.analysis.context;

import bamboo.pta.analysis.data.CSCallSite;
import bamboo.pta.analysis.data.CSMethod;
import bamboo.pta.analysis.data.CSObj;
import bamboo.pta.element.Method;

public class ContextInsensitiveSelector implements ContextSelector {

    @Override
    public Context selectContext(CSCallSite callSite, Method callee) {
        return DefaultContext.INSTANCE;
    }

    @Override
    public Context selectContext(CSCallSite callSite, CSObj recv, Method callee) {
        return DefaultContext.INSTANCE;
    }

    @Override
    public Context selectHeapContext(CSMethod method, Object allocation) {
        return DefaultContext.INSTANCE;
    }
}
