package sa.pta.analysis.context;

import sa.pta.analysis.data.CSCallSite;
import sa.pta.analysis.data.CSMethod;
import sa.pta.element.Method;

public class ContextInsensitiveSelector implements ContextSelector {
    @Override
    public Context selectContext(CSCallSite callSite, Method callee) {
        return DefaultContext.INSTANCE;
    }

    @Override
    public Context selectHeapContext(CSMethod method, Object allocation) {
        return DefaultContext.INSTANCE;
    }
}
