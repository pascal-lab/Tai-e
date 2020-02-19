package sa.pta.analysis.context;

import sa.pta.analysis.data.CSCallSite;
import sa.pta.analysis.data.CSMethod;
import sa.pta.analysis.data.CSObj;
import sa.pta.element.Method;

public interface ContextSelector {

    default Context getDefaultContext() {
        return DefaultContext.INSTANCE;
    }

    /**
     * Select context for static method.
     */
    Context selectContext(CSCallSite callSite, Method callee);

    /**
     * Select context for instance method.
     */
    Context selectContext(CSCallSite callSite, CSObj recv, Method callee);

    Context selectHeapContext(CSMethod method, Object allocationSite);
}
