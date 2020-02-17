package sa.pta.analysis.context;

import sa.pta.analysis.data.CSCallSite;
import sa.pta.analysis.data.CSMethod;
import sa.pta.element.Method;

public interface ContextSelector {

    Context selectContext(CSCallSite callSite, Method callee);

    Context selectHeapContext(CSMethod method, Object allocationSite);
}
