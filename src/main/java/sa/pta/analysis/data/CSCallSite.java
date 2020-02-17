package sa.pta.analysis.data;

import sa.pta.analysis.context.Context;
import sa.pta.element.CallSite;

public class CSCallSite extends AbstractCSElement {

    private final CallSite callSite;

    CSCallSite(Context context, CallSite callSite) {
        super(context);
        this.callSite = callSite;
    }

    public CallSite getCallSite() {
        return callSite;
    }

}
