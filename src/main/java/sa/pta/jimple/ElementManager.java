package sa.pta.jimple;

import soot.Body;
import soot.Local;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
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

    private Map<SootMethod, Map<Local, JimpleVariable>> vars = new HashMap<>();

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
            jMethod.setThisVar(getVariable(body.getThisLocal(), method));
        }
        jMethod.setParameters(
                body.getParameterLocals()
                        .stream()
                        .map(param -> getVariable(param, method))
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

    private JimpleVariable getVariable(Local var, SootMethod method) {
        return vars.computeIfAbsent(method, (m) -> new HashMap<>())
                .computeIfAbsent(var, (v) -> {
                    JimpleType type = getType(var.getType());
                    return new JimpleVariable(var, type, getMethod(method));
                });
    }

    private JimpleCallSite createCallSite(InvokeExpr invoke, SootMethod method) {
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

        }

        private void build(JimpleMethod jMethod, IdentityStmt stmt) {
        }

        private void build(JimpleMethod jMethod, InvokeStmt stmt) {
        }

        private void build(JimpleMethod jMethod, ReturnStmt stmt) {

        }

        private void build(JimpleMethod jMethod, ThrowStmt stmt) {
        }
    }
}
