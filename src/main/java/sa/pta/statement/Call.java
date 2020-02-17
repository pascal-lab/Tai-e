package sa.pta.statement;

import sa.pta.element.CallSite;
import sa.pta.element.Variable;

/**
 * Represents a call statement r = o.m()/r = T.m();
 */
public class Call implements Statement {

    private final CallSite callSite;

    private final Variable receivingVar;

    public Call(CallSite callSite, Variable receivingVar) {
        this.callSite = callSite;
        this.receivingVar = receivingVar;
    }

    public CallSite getCallSite() {
        return callSite;
    }

    public Variable getReceivingVar() {
        return receivingVar;
    }
}
