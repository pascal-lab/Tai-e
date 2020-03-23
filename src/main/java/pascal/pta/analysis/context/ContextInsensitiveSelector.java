package pascal.pta.analysis.context;

import pascal.pta.analysis.data.CSCallSite;
import pascal.pta.analysis.data.CSMethod;
import pascal.pta.analysis.data.CSObj;
import pascal.pta.element.Method;

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
