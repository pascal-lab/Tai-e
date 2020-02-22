package sa.pta.jimple;

import sa.callgraph.JimpleCallUtils;
import sa.util.AnalysisException;
import soot.Body;
import soot.Local;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ThrowStmt;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

class ElementManager {

    private Map<Type, JimpleType> types = new HashMap<>();

    private Map<SootMethod, JimpleMethod> methods = new HashMap<>();

    private Map<JimpleMethod, Map<Local, JimpleVariable>> vars = new HashMap<>();

    private Map<SootField, JimpleField> fields = new HashMap<>();

    private BodyBuilder bodyBuilder = new BodyBuilder();

    JimpleMethod getMethod(SootMethod method) {
        return methods.computeIfAbsent(method, this::createMethod);
    }

    private JimpleMethod createMethod(SootMethod method) {
        JimpleType jType = getType(method.getDeclaringClass());
        JimpleMethod jMethod = new JimpleMethod(method, jType);
        // TODO - skip special (e.g., native or phantom) methods
        Body body = method.retrieveActiveBody();
        // add this variable and parameters
        if (!method.isStatic()) {
            jMethod.setThisVar(getVariable(body.getThisLocal(), jMethod));
        }
        jMethod.setParameters(
                body.getParameterLocals()
                        .stream()
                        .map(param -> getVariable(param, jMethod))
                        .collect(Collectors.toList())
        );
        // add statements
        bodyBuilder.build(jMethod, body);
        return jMethod;
    }

    private JimpleType getType(SootClass sootClass) {
        return types.computeIfAbsent(sootClass.getType(), JimpleType::new);
    }

    private JimpleType getType(Type type) {
        return types.computeIfAbsent(type, JimpleType::new);
    }

    private JimpleVariable getVariable(Local var, JimpleMethod container) {
        return vars.computeIfAbsent(container, (m) -> new HashMap<>())
                .computeIfAbsent(var, (v) -> {
                    JimpleType type = getType(var.getType());
                    return new JimpleVariable(var, type, container);
                });
    }

    /**
     * Converts Value to Variable.
     * If the value is Local, return it directly, else assign the value to
     * a temporary variable and return the variable.
     */
    private JimpleVariable getVariable(Value value, JimpleMethod container) {
        if (value instanceof Local) {
            return getVariable((Local) value, container);
        } else {
            // TODO: handle string constants
            // TODO: handle class constants
            // TODO: handle other cases
            throw new AnalysisException("Cannot handle value: " + value);
        }
    }

    private JimpleCallSite createCallSite(InvokeExpr invoke, JimpleMethod container) {
        JimpleCallSite callSite = new JimpleCallSite(
                invoke, JimpleCallUtils.getCallKind(invoke));
        callSite.setMethod(getMethod(invoke.getMethod()));

        callSite.setContainerMethod(container);

        return null;
    }

    private class BodyBuilder {

        private RelevantUnitSwitch sw = new RelevantUnitSwitch();

        private void build(JimpleMethod jMethod, Body body) {
            for (Unit unit : body.getUnits()) {
                unit.apply(sw);
                if (sw.isRelevant()) {
                    if (unit instanceof AssignStmt) {
                        build(jMethod, (AssignStmt) unit);
                    } else if (unit instanceof IdentityStmt) {
                        build(jMethod, (IdentityStmt) unit);
                    } else if (unit instanceof InvokeStmt) {
                        build(jMethod, ((InvokeStmt) unit));
                    } else if (unit instanceof ReturnStmt) {
                        build(jMethod, (ReturnStmt) unit);
                    } else if (unit instanceof ThrowStmt) {
                        build(jMethod, (ThrowStmt) unit);
                    } else {
                        throw new RuntimeException("Cannot handle statement: " + unit);
                    }
                }
            }
        }

        private void build(JimpleMethod jMethod, AssignStmt stmt) {
            // x = new T();
            // x = y;
            // x.f = y;
            // x = y.f;
            // r = x.m();
        }

        private void build(JimpleMethod jMethod, IdentityStmt stmt) {
            // identity statement is for parameter passing and catch statements
            // parameters have been handled when creating JimpleMethod
            // currently ignore catch statements
        }

        private void build(JimpleMethod jMethod, InvokeStmt stmt) {
            // x.m();
        }

        private void build(JimpleMethod jMethod, ReturnStmt stmt) {
            jMethod.addReturnVar(getVariable(stmt.getOp(), jMethod));
            Value v = stmt.getOp();
        }

        private void build(JimpleMethod jMethod, ThrowStmt stmt) {
            // currently ignore throw statements
        }
    }
}
