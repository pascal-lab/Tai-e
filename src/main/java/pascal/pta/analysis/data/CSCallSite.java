package pascal.pta.analysis.data;

import pascal.pta.analysis.context.Context;
import pascal.pta.element.CallSite;

public class CSCallSite extends AbstractCSElement {

    private final CallSite callSite;

    CSCallSite(Context context, CallSite callSite) {
        super(context);
        this.callSite = callSite;
    }

    public CallSite getCallSite() {
        return callSite;
    }

    @Override
    public String toString() {
        return context + ":" + callSite;
    }
}
