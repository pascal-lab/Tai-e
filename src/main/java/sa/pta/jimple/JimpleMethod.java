package sa.pta.jimple;

import sa.pta.element.Method;
import sa.pta.element.Variable;
import sa.pta.statement.Statement;
import soot.SootMethod;

import java.util.List;
import java.util.Set;

class JimpleMethod implements Method {

    private SootMethod method;

    private JimpleType classType;

    private JimpleVariable thisVar;

    private List<Variable> parameters;

    private Set<Variable> returnVars;

    private Set<Statement> statements;

    @Override
    public boolean isInstance() {
        return !method.isStatic();
    }

    @Override
    public boolean isStatic() {
        return method.isStatic();
    }

    @Override
    public boolean isNative() {
        return method.isNative();
    }

    @Override
    public JimpleType getClassType() {
        return classType;
    }

    @Override
    public String getName() {
        return method.getName();
    }

    @Override
    public JimpleVariable getThis() {
        return thisVar;
    }

    @Override
    public List<Variable> getParameters() {
        return parameters;
    }

    @Override
    public Set<Variable> getReturnVariables() {
        return null;
    }

    @Override
    public Set<Statement> getStatements() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JimpleMethod that = (JimpleMethod) o;
        return method.equals(that.method);
    }

    @Override
    public int hashCode() {
        return method.hashCode();
    }

    SootMethod getSootMethod() {
        return method;
    }
}
