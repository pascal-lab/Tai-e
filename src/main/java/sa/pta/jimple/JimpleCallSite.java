package sa.pta.jimple;

import sa.callgraph.CallKind;
import sa.pta.element.CallSite;
import sa.pta.element.Variable;
import soot.jimple.InvokeExpr;

import java.util.List;

class JimpleCallSite implements CallSite {

    private InvokeExpr invoke;

    private CallKind kind;

    private JimpleMethod method;

    private JimpleVariable receiver;

    private List<Variable> arguments;

    private JimpleVariable lhs;

    private JimpleMethod containingMethod;

    @Override
    public boolean isVirtual() {
        return kind == CallKind.VIRTUAL;
    }

    @Override
    public boolean isSpecial() {
        return kind == CallKind.SPECIAL;
    }

    @Override
    public boolean isStatic() {
        return kind == CallKind.STATIC;
    }

    @Override
    public JimpleMethod getMethod() {
        return method;
    }

    @Override
    public JimpleVariable getReceiver() {
        return receiver;
    }

    @Override
    public List<Variable> getArguments() {
        return arguments;
    }

    @Override
    public JimpleVariable getLHS() {
        return lhs;
    }

    @Override
    public JimpleMethod getContainerMethod() {
        return containingMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JimpleCallSite that = (JimpleCallSite) o;
        return invoke.equals(that.invoke);
    }

    @Override
    public int hashCode() {
        return invoke.hashCode();
    }

    InvokeExpr getSootInvokeExpr() {
        return invoke;
    }
}
