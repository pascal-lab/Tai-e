package sa.pta.jimple;

import soot.Body;
import soot.Local;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

class ElementManager {

    Map<Type, JimpleType> types = new HashMap<>();

    Map<SootMethod, JimpleMethod> methods = new HashMap<>();

    Map<SootMethod, Map<Local, JimpleVariable>> vars = new HashMap<>();

    Map<SootField, JimpleField> fields = new HashMap<>();


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
        processMethodBody(method, body, jMethod);
        return jMethod;
    }

    private void processMethodBody(
            SootMethod method, Body body, JimpleMethod jMethod) {
        for (Unit unit : body.getUnits()) {
        }
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
}
