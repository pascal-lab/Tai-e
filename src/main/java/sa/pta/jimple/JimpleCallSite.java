package sa.pta.jimple;

import sa.callgraph.CallKind;
import sa.pta.element.CallSite;
import sa.pta.element.Method;
import sa.pta.element.Variable;
import soot.SootMethod;
import soot.jimple.InvokeExpr;

import java.util.List;

public class JimpleCallSite implements CallSite {

    private InvokeExpr invoke;

    private CallKind kind;

    private Method method;

    private Variable receiver;

    private List<Variable> arguments;

    private Variable lhs;

    private Method containingMethod;

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
    public Method getMethod() {
        return method;
    }

    @Override
    public Variable getReceiver() {
        return receiver;
    }

    @Override
    public List<Variable> getArguments() {
        return arguments;
    }

    @Override
    public Variable getLHS() {
        return lhs;
    }

    @Override
    public Method getContainingMethod() {
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
}
