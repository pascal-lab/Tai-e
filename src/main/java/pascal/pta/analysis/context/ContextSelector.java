package pascal.pta.analysis.context;

import pascal.pta.analysis.data.CSCallSite;
import pascal.pta.analysis.data.CSMethod;
import pascal.pta.analysis.data.CSObj;
import pascal.pta.element.Method;

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
