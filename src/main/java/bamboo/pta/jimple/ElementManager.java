/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.jimple;

import bamboo.callgraph.JimpleCallUtils;
import bamboo.pta.element.Variable;
import bamboo.pta.statement.Allocation;
import bamboo.pta.statement.ArrayLoad;
import bamboo.pta.statement.ArrayStore;
import bamboo.pta.statement.Assign;
import bamboo.pta.statement.AssignCast;
import bamboo.pta.statement.Call;
import bamboo.pta.statement.InstanceLoad;
import bamboo.pta.statement.InstanceStore;
import bamboo.pta.statement.StaticLoad;
import bamboo.pta.statement.StaticStore;
import bamboo.util.AnalysisException;
import bamboo.util.MutableInteger;
import soot.Body;
import soot.Local;
import soot.RefLikeType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;
import soot.jimple.internal.JimpleLocal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class ElementManager {

    private Map<Type, JimpleType> types = new HashMap<>();

    private Map<SootMethod, JimpleMethod> methods = new HashMap<>();

    private Map<JimpleMethod, Map<Local, JimpleVariable>> vars = new HashMap<>();

    private Map<SootField, JimpleField> fields = new HashMap<>();

    private MethodBuilder methodBuilder = new MethodBuilder();

    private NewVariableManager varManager = new NewVariableManager();

    JimpleMethod getMethod(SootMethod method) {
        return methods.computeIfAbsent(method, this::createMethod);
    }

    private JimpleMethod createMethod(SootMethod method) {
        JimpleType jType = getType(method.getDeclaringClass());
        JimpleMethod jMethod = new JimpleMethod(method, jType);
        if (method.isNative()) {
            methodBuilder.buildNative(jMethod);
        } else if (!method.isAbstract()) {
            Body body = method.retrieveActiveBody();
            methodBuilder.buildConcrete(jMethod, body);
        }
        return jMethod;
    }

    private JimpleType getType(SootClass sootClass) {
        return types.computeIfAbsent(sootClass.getType(), JimpleType::new);
    }

    private JimpleType getType(Type type) {
        return types.computeIfAbsent(type, JimpleType::new);
    }

    private JimpleField getField(SootField sootField) {
        return fields.computeIfAbsent(sootField, (f) ->
                new JimpleField(sootField,
                        getType(sootField.getDeclaringClass()),
                        getType(sootField.getType())));
    }

    /**
     * Returns all local variables in a given method.
     */
    Collection<JimpleVariable> getLocalVariablesOf(JimpleMethod container) {
        return vars.get(container).values();
    }

    JimpleVariable getVariable(Local var, JimpleMethod container) {
        return var.getType() instanceof RefLikeType
                ? vars.computeIfAbsent(container, (m) -> new HashMap<>())
                    .computeIfAbsent(var, (v) -> {
                        JimpleType type = getType(var.getType());
                        return new JimpleVariable(var, type, container);
                    })
                : null; // returns null for variables with non-reference type
    }

    /**
     * Converts Value to Variable.
     * If the value is Local, return it directly, else assign the value to
     * a temporary variable and return the variable.
     */
    private JimpleVariable getVariable(Value value, JimpleMethod container) {
        if (value instanceof Local) {
            return getVariable((Local) value, container);
        } else if (value instanceof NullConstant) {
            return varManager.getTempVariable("null$",
                    getType(value.getType()), container);
        } else {
            // TODO: handle string constants
            // TODO: handle class constants
            // TODO: handle other cases
            throw new AnalysisException("Cannot handle value: " + value);
        }
    }

    private JimpleCallSite createCallSite(Stmt stmt, JimpleMethod container) {
        InvokeExpr invoke = stmt.getInvokeExpr();
        JimpleCallSite callSite = new JimpleCallSite(
                stmt, JimpleCallUtils.getCallKind(invoke));
        callSite.setMethod(getMethod(invoke.getMethod()));
        if (invoke instanceof InstanceInvokeExpr) {
            callSite.setReceiver(getVariable(
                    ((InstanceInvokeExpr) invoke).getBase(), container));
        }
        callSite.setArguments(
                invoke.getArgs()
                        .stream()
                        .map(v -> getVariable(v, container))
                        .filter(v -> v != null)
                        .collect(Collectors.toList())
        );
        callSite.setContainerMethod(container);
        return callSite;
    }

    private JimpleObj createObject(AssignStmt alloc, JimpleMethod container) {
        return new JimpleObj(alloc,
                getType(alloc.getRightOp().getType()),
                container);
    }

    private class MethodBuilder {

        private RelevantUnitSwitch sw = new RelevantUnitSwitch();

        /**
         * Build parameters, this variable (if exists), and
         * return variable (if exists) for the given native method.
         */
        private void buildNative(JimpleMethod method) {
            SootMethod sootMethod = method.getSootMethod();
            if (!sootMethod.isStatic()) {
                method.setThisVar(varManager.getThisVariable(method));
            }
            int paramCount = sootMethod.getParameterCount();
            if (paramCount > 0) {
                List<Variable> params = new ArrayList<>(paramCount);
                for (int i = 0; i < paramCount; ++i) {
                    params.add(varManager.getParameter(method, i));
                }
                method.setParameters(params);
            }
            if (sootMethod.getReturnType() instanceof RefLikeType) {
                method.addReturnVar(varManager.getReturnVariable(method));
            }
        }

        private void buildConcrete(JimpleMethod method, Body body) {
            // add this variable and parameters
            if (!method.isStatic()) {
                method.setThisVar(getVariable(body.getThisLocal(), method));
            }
            method.setParameters(
                    body.getParameterLocals()
                            .stream()
                            .map(param -> getVariable(param, method))
                            .filter(v -> v != null)
                            .collect(Collectors.toList())
            );
            // add statements
            for (Unit unit : body.getUnits()) {
                unit.apply(sw);
                if (sw.isRelevant()) {
                    if (unit instanceof AssignStmt) {
                        build(method, (AssignStmt) unit);
                    } else if (unit instanceof IdentityStmt) {
                        build(method, (IdentityStmt) unit);
                    } else if (unit instanceof InvokeStmt) {
                        build(method, ((InvokeStmt) unit));
                    } else if (unit instanceof ReturnStmt) {
                        build(method, (ReturnStmt) unit);
                    } else if (unit instanceof ThrowStmt) {
                        build(method, (ThrowStmt) unit);
                    } else {
                        throw new RuntimeException("Cannot handle statement: " + unit);
                    }
                }
            }
        }

        private void build(JimpleMethod method, AssignStmt stmt) {
            Value left = stmt.getLeftOp();
            Value right = stmt.getRightOp();
            // TODO: filter primitive types
            if (left instanceof Local) {
                Variable lhs = getVariable(left, method);
                if (stmt.containsInvokeExpr()) {
                    // x = o.m();
                    JimpleCallSite callSite = createCallSite(stmt, method);
                    Call call = new Call(callSite, lhs);
                    callSite.setCall(call);
                    method.addStatement(call);
                } else if (lhs == null) { // lhs is primitive type
                    return;
                } else if (right instanceof NewExpr
                        || right instanceof NewArrayExpr) {
                    // x = new T();
                    // x = new T[];
                    // TODO: handle allocation comprehensively
                    method.addStatement(new Allocation(lhs, createObject(stmt, method)));
                } else if (right instanceof Local) {
                    // x = y;
                    method.addStatement(new Assign(lhs, getVariable((Local) right, method)));
                } else if (right instanceof CastExpr) {
                    // x = (T) y;
                    CastExpr cast = (CastExpr) right;
                    method.addStatement(new AssignCast(lhs,
                            getType(cast.getCastType()),
                            getVariable((Local) cast.getOp(), method)
                    ));
                } else if (right instanceof InstanceFieldRef) {
                    // x = y.f;
                    InstanceFieldRef ref = (InstanceFieldRef) right;
                    JimpleVariable base = getVariable(ref.getBase(), method);
                    InstanceLoad load = new InstanceLoad(lhs, base, getField(ref.getField()));
                    base.addInstanceLoad(load);
                    method.addStatement(load);
                } else if (right instanceof ArrayRef) {
                    // x = y[i];
                    // TODO: consider constant index?
                    ArrayRef ref = (ArrayRef) right;
                    JimpleVariable base = getVariable(ref.getBase(), method);
                    ArrayLoad load = new ArrayLoad(lhs, base);
                    base.addArrayLoad(load);
                    method.addStatement(load);
                } else if (right instanceof StaticFieldRef) {
                    // x = T.f;
                    StaticFieldRef ref = (StaticFieldRef) right;
                    StaticLoad load = new StaticLoad(lhs, getField(ref.getField()));
                    method.addStatement(load);
                } else {
                    // TODO: x = new T[]+;
                    // TODO: x = "x";
                    // TODO: x = T.class;
                    // TODO: x = other cases
                    throw new AnalysisException("Unhandled case: " + right);
                }
            } else if (left instanceof InstanceFieldRef) {
                // x.f = y;
                InstanceFieldRef ref = (InstanceFieldRef) left;
                JimpleVariable base = getVariable(ref.getBase(), method);
                InstanceStore store = new InstanceStore(base,
                        getField(ref.getField()),
                        getVariable(right, method));
                base.addInstanceStore(store);
                method.addStatement(store);
            } else if (left instanceof StaticFieldRef) {
                // T.f = x;
                StaticFieldRef ref = (StaticFieldRef) left;
                StaticStore store = new StaticStore(
                        getField(ref.getField()),
                        getVariable(right, method)
                );
                method.addStatement(store);
            } else if (left instanceof ArrayRef) {
                // x[i] = y;
                // TODO: consider constant index?
                ArrayRef ref = (ArrayRef) left;
                JimpleVariable base = getVariable(ref.getBase(), method);
                ArrayStore store = new ArrayStore(
                        base, getVariable(right, method));
                base.addArrayStore(store);
                method.addStatement(store);
            } else {
                throw new AnalysisException("Unhandled case: " + left);
            }
        }

        private void build(JimpleMethod method, IdentityStmt stmt) {
            // identity statement is for parameter passing and catch statements
            // parameters have been handled when creating JimpleMethod
            // currently ignore catch statements
        }

        private void build(JimpleMethod method, InvokeStmt stmt) {
            // x.m();
            JimpleCallSite callSite = createCallSite(stmt, method);
            Call call = new Call(callSite, null);
            callSite.setCall(call);
            method.addStatement(call);
        }

        private void build(JimpleMethod method, ReturnStmt stmt) {
            if (stmt.getOp().getType() instanceof RefLikeType) {
                method.addReturnVar(getVariable(stmt.getOp(), method));
            }
        }

        private void build(JimpleMethod method, ThrowStmt stmt) {
            // currently ignore throw statements
        }
    }

    /**
     * Manager for new created variables during method creation.
     */
    private class NewVariableManager {

        private Map<JimpleMethod, MutableInteger> varNumbers = new HashMap<>();

        private JimpleVariable getTempVariable(
                String baseName, JimpleType type, JimpleMethod container) {
            String varName = baseName + getNewNumber(container);
            return getNewVariable(varName, type, container);
        }

        private int getNewNumber(JimpleMethod container) {
            return varNumbers.computeIfAbsent(container,
                    m -> new MutableInteger(0))
                    .increase();
        }

        private JimpleVariable getThisVariable(JimpleMethod container) {
            return getNewVariable("@this", container.getClassType(), container);
        }

        private JimpleVariable getParameter(JimpleMethod container, int index) {
            JimpleType type = getType(container.getSootMethod().getParameterType(index));
            return getNewVariable("@parameter" + index, type, container);
        }

        private JimpleVariable getReturnVariable(JimpleMethod container) {
            JimpleType type = getType(container.getSootMethod().getReturnType());
            return getNewVariable("@return", type, container);
        }

        private JimpleVariable getNewVariable(
                String varName, JimpleType type, JimpleMethod container) {
            Local local = new JimpleLocal(varName, type.getSootType());
            return new JimpleVariable(local, type, container);
        }
    }
}
