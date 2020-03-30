package bamboo.pta.analysis.context;

import bamboo.pta.analysis.data.CSCallSite;
import bamboo.pta.analysis.data.CSMethod;
import bamboo.pta.analysis.data.CSObj;
import bamboo.pta.element.Method;

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
