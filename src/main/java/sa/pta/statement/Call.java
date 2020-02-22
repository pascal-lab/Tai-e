package sa.pta.statement;

import sa.pta.element.CallSite;
import sa.pta.element.Variable;

/**
 * Represents a call statement r = o.m()/r = T.m();
 */
public class Call implements Statement {

    private final CallSite callSite;

    private final Variable lhs;

    public Call(CallSite callSite, Variable lhs) {
        this.callSite = callSite;
        this.lhs = lhs;
    }

    public CallSite getCallSite() {
        return callSite;
    }

    public Variable getLHS() {
        return lhs;
    }

    @Override
    public Kind getKind() {
        return Kind.CALL;
    }

    @Override
    public String toString() {
        return lhs != null
                ? lhs + " = " + callSite.toString()
                : callSite.toString();
    }
}
