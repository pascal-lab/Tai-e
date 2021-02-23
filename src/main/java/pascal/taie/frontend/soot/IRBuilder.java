/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.frontend.soot;

import pascal.taie.callgraph.JimpleCallUtils;
import pascal.taie.java.classes.FieldReference;
import pascal.taie.java.classes.JField;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.types.Type;
import pascal.taie.pta.PTAOptions;
import pascal.taie.pta.env.Environment;
import pascal.taie.pta.ir.Allocation;
import pascal.taie.pta.ir.ArrayLoad;
import pascal.taie.pta.ir.ArrayStore;
import pascal.taie.pta.ir.Assign;
import pascal.taie.pta.ir.AssignCast;
import pascal.taie.pta.ir.Call;
import pascal.taie.pta.ir.InstanceLoad;
import pascal.taie.pta.ir.InstanceStore;
import pascal.taie.pta.ir.Obj;
import pascal.taie.pta.ir.StaticLoad;
import pascal.taie.pta.ir.StaticStore;
import pascal.taie.pta.ir.Variable;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.Timer;
import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.ClassConstant;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.MethodHandle;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.NumericConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.ThrowStmt;
import soot.shimple.PhiExpr;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Jimple-based pointer analysis IR builder.
 */
class IRBuilder {

    private final ConcurrentMap<soot.Type, Type> types
            = new ConcurrentHashMap<>();

    private final ConcurrentMap<SootMethod, JMethod> methods
            = new ConcurrentHashMap<>();

    private final ConcurrentMap<JMethod, Map<Local, Variable>> vars
            = new ConcurrentHashMap<>();

    private final ConcurrentMap<SootField, JField> fields
            = new ConcurrentHashMap<>();

    private final NewVariableManager varManager = new NewVariableManager();

    private final ClassDumper classDumper = new ClassDumper();

    private final Environment env;

    IRBuilder(Environment env) {
        this.env = env;
    }

    /**
     * Build IR for all methods in given Scene.
     */
    void buildAllMethods(Scene scene) {
        Timer timer = new Timer("Build IR for all methods");
        timer.start();
        int nThreads = Runtime.getRuntime().availableProcessors();
        // Group all methods by number of threads
        List<List<SootMethod>> groups = new ArrayList<>();
        for (int i = 0; i < nThreads; ++i) {
            groups.add(new ArrayList<>());
        }
        List<SootClass> classes = new ArrayList<>(scene.getClasses());
        int i = 0;
        for (SootClass c : classes) {
            for (SootMethod m : c.getMethods()) {
                if ((m.isConcrete() || m.isNative()) && !m.isPhantom()) {
                    groups.get(i++ % nThreads).add(m);
                }
            }
        }
        // Build IR for all methods in parallel
        ExecutorService service = Executors.newFixedThreadPool(nThreads);
        for (List<SootMethod> group : groups) {
            service.execute(() -> group.forEach(this::getMethod));
        }
        service.shutdown();
        try {
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        timer.stop();
        System.out.println(timer);
        System.out.println("#methods: " + methods.size());
    }

    JimpleMethod getMethod(SootMethod sootMethod) {
        return getMethod(sootMethod, true);
    }

    private JimpleMethod getMethod(SootMethod sootMethod, boolean builtBody) {
        assert sootMethod != null;
        JimpleMethod method = methods.computeIfAbsent(sootMethod, (m) -> {
            JimpleType type = getType(m.getDeclaringClass());
            return new JimpleMethod(m, type);
        });
        if (builtBody && !method.hasBuilt()) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (method) {
                if (!method.hasBuilt()) {
                    if (method.isNative()) {
                        buildNative(method);
                    } else if (!sootMethod.isAbstract()) {
                        Body body = sootMethod.retrieveActiveBody();
                        buildConcrete(method, body);
                    }
                    env.processNativeCode(method);
                    method.setBuilt(true);
                }
            }
        }
        return method;
    }

    private JimpleType getType(SootClass sootClass) {
        return getType(sootClass.getType());
    }

    JimpleType getType(Type sootType) {
        JimpleType type = types.computeIfAbsent(sootType, JimpleType::new);
        if (!type.hasBuilt()) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (type) {
                if (!type.hasBuilt()) { // double-check
                    buildType(type, sootType);
                }
            }
        }
        return type;
    }

    /**
     * Build type information.
     */
    private void buildType(JimpleType type, Type sootType) {
        if (sootType instanceof ArrayType) {
            ArrayType t = (ArrayType) sootType;
            type.setElementType(getType(t.getElementType()));
            type.setBaseType(getType(t.baseType));
        }
        if (sootType instanceof RefType) {
            SootClass c = ((RefType) sootType).getSootClass();
            if (PTAOptions.get().isDumpClasses()) {
                classDumper.dump(c);
            }
            type.setSootClass(c);
            if (c.hasSuperclass() && !c.isInterface()) {
                type.setSuperClass(getType(c.getSuperclass()));
            }
            c.getInterfaces()
                    .forEach(i -> type.addSuperInterface(getType(i)));
        }
        type.setBuilt(true);
    }

    FieldReference getField(SootField sootField) {
        return fields.computeIfAbsent(sootField, (field) ->
                new JimpleField(field,
                        getType(field.getDeclaringClass()),
                        getType(field.getType())));
    }

    JimpleVariable getVariable(Local var, JimpleMethod container) {
        if (var.getType() instanceof RefLikeType) {
            return vars.computeIfAbsent(container, (m) -> new LinkedHashMap<>())
                    .computeIfAbsent(var, (v) -> {
                        JimpleType type = getType(var.getType());
                        return new JimpleVariable(var, type, container);
                    });
        } else {
            throw new AnalysisException("Local variable of primitive type: " + var);
        }
    }

    private JimpleCallSite createCallSite(Stmt stmt, JimpleMethod container) {
        InvokeExpr invoke = stmt.getInvokeExpr();
        JimpleCallSite callSite = new JimpleCallSite(
                stmt, JimpleCallUtils.getCallKind(invoke));
        callSite.setMethod(getMethod(invoke.getMethod(), false));
        if (invoke instanceof InstanceInvokeExpr) {
            Local base = (Local) ((InstanceInvokeExpr) invoke).getBase();
            callSite.setReceiver(getVariable(base, container));
        }
        // TODO: handle DynamicInvokeExpr
        if (invoke.getArgCount() > 0) {
            List<Variable> args = new ArrayList<>(invoke.getArgCount());
            for (Value arg : invoke.getArgs()) {
                if (arg.getType() instanceof RefLikeType
                        && !(arg instanceof NullConstant)) {
                    if (arg instanceof Local) {
                        args.add(getVariable((Local) arg, container));
                    } else if (isConstant(arg)) {
                        args.add(getVariableOfConstant(arg, container));
                    } else {
                        throw new AnalysisException("Unhandled argument: " + arg);
                    }
                } else { // null for arguments of primitive type or null
                    args.add(null);
                }
            }
            callSite.setArguments(args);
        }
        callSite.setContainerMethod(container);
        return callSite;
    }

    private JimpleObj createObject(AssignStmt alloc, JimpleMethod container) {
        return new JimpleObj(alloc,
                getType(alloc.getRightOp().getType()),
                container);
    }

    /**
     * Processes allocation of multi-array, which needs to be
     * allocated separately for every dimension of the array.
     */
    private void newMultiArray(AssignStmt alloc, Variable lhs,
                               ArrayType arrayType, JimpleMethod container) {
        JimpleObj array = new JimpleObj(alloc, getType(arrayType), container);
        container.addStatement(new Allocation(lhs, array));
        Type elemType = arrayType.getElementType();
        if (elemType instanceof ArrayType) {
            Variable temp = varManager.newTempVariable("array$",
                    getType(elemType), container);
            newMultiArray(alloc, temp, (ArrayType) elemType, container);
            container.addStatement(new ArrayStore(lhs, temp));
        }
    }

    private boolean isConstant(Value value) {
        return value instanceof StringConstant
                || value instanceof ClassConstant
                || value instanceof MethodHandle
                || value instanceof NumericConstant;
    }

    /**
     * Returns the Obj which represents the given constant.
     */
    private Obj getConstantObj(Value constant) {
        if (constant instanceof StringConstant) {
            return env.getStringConstant(((StringConstant) constant).value);
        } else if (constant instanceof ClassConstant) {
            return env.getClassObj(getType(((ClassConstant) constant).toSootType()));
        } else if (constant instanceof MethodHandle) {
            throw new UnsupportedOperationException("MethodHandle is not supported");
        } else if (constant instanceof NumericConstant) {
            throw new AnalysisException("Unhandled numeric constant: " + constant);
        } else {
            throw new AnalysisException("Unhandled case: " + constant);
        }
    }

    /**
     * Given a constant, returns the temporary variable which points
     * to the constant. To achieve this, the corresponding Allocation statement
     * (from the constant value to the variable) will be added
     * to the container method.
     */
    private JimpleVariable getVariableOfConstant(
            Value constant, JimpleMethod container) {
        Obj obj = getConstantObj(constant);
        JimpleVariable temp = varManager.newTempVariable(
                "constant$", getType(constant.getType()), container);
        container.addStatement(new Allocation(temp, obj));
        return temp;
    }

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
                if (sootMethod.getParameterType(i) instanceof RefLikeType) {
                    params.add(varManager.getParameter(method, i));
                } else { // null for parameters of primitive type
                    params.add(null);
                }
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
        // add parameters
        if (body.getParameterLocals().size() > 0) {
            List<Variable> params = new ArrayList<>(
                    body.getParameterLocals().size());
            for (Local param : body.getParameterLocals()) {
                if (param.getType() instanceof RefLikeType) {
                    params.add(getVariable(param, method));
                } else {
                    params.add(null);
                }
            }
            method.setParameters(params);
        }
        // add statements
        RelevantUnitSwitch sw = new RelevantUnitSwitch();
        for (Unit unit : body.getUnits()) {
            unit.apply(sw);
            if (sw.isRelevant()) {
                if (unit instanceof AssignStmt) {
                    buildAssign(method, (AssignStmt) unit);
                } else if (unit instanceof IdentityStmt) {
                    buildIdentity(method, (IdentityStmt) unit);
                } else if (unit instanceof InvokeStmt) {
                    buildCall(method, ((InvokeStmt) unit), null);
                } else if (unit instanceof ReturnStmt) {
                    buildReturn(method, (ReturnStmt) unit);
                } else if (unit instanceof ThrowStmt) {
                    buildThrow(method, (ThrowStmt) unit);
                } else {
                    throw new RuntimeException("Cannot handle statement: " + unit);
                }
            }
        }
    }

    private void buildAssign(JimpleMethod method, AssignStmt stmt) {
        Value left = stmt.getLeftOp();
        if (stmt.containsInvokeExpr()) {
            buildCall(method, stmt, left);
        } else if (left.getType() instanceof RefLikeType) {
            // only build statements for non-primitive types
            if (left instanceof Local) {
                buildLeftLocal(method, stmt, (Local) left);
            } else {
                buildLeftNonLocal(method, stmt, left);
            }
        }
    }

    private void buildLeftLocal(JimpleMethod method, AssignStmt stmt, Local left) {
        Variable lhs = getVariable(left, method);
        Value right = stmt.getRightOp();
        if (right instanceof NewExpr
                || right instanceof NewArrayExpr) {
            // x = new T();
            // x = new T[];
            method.addStatement(new Allocation(lhs, createObject(stmt, method)));
        } else if (right instanceof NewMultiArrayExpr) {
            // x = new T[][]...;
            newMultiArray(stmt, lhs, (ArrayType) right.getType(), method);
        } else if (right instanceof NullConstant) {
            // x = null;
            // ignore
        } else if (isConstant(right)) {
            method.addStatement(new Allocation(lhs, getConstantObj(right)));
        } else if (right instanceof Local) {
            // x = y;
            method.addStatement(new Assign(lhs, getVariable((Local) right, method)));
        } else if (right instanceof CastExpr) {
            // x = (T) y;
            CastExpr cast = (CastExpr) right;
            Value op = cast.getOp();
            if (op instanceof Local) {
                method.addStatement(new AssignCast(lhs,
                        getType(cast.getCastType()),
                        getVariable((Local) cast.getOp(), method)
                ));
            }
            // ignore other casting cases
        } else if (right instanceof PhiExpr) {
            // x = phi(v1, ..., vn)
            for (Value from : ((PhiExpr) right).getValues()) {
                method.addStatement(new Assign(lhs,
                        getVariable((Local) from, method)));
            }
        } else if (right instanceof InstanceFieldRef) {
            // x = y.f;
            InstanceFieldRef ref = (InstanceFieldRef) right;
            JimpleVariable base = getVariable((Local) ref.getBase(), method);
            InstanceLoad load = new InstanceLoad(lhs, base, getField(ref.getField()));
            method.addStatement(load);
        } else if (right instanceof ArrayRef) {
            // x = y[i];
            // TODO: consider constant index?
            ArrayRef ref = (ArrayRef) right;
            JimpleVariable base = getVariable((Local) ref.getBase(), method);
            ArrayLoad load = new ArrayLoad(lhs, base);
            method.addStatement(load);
        } else if (right instanceof StaticFieldRef) {
            // x = T.f;
            StaticFieldRef ref = (StaticFieldRef) right;
            StaticLoad load = new StaticLoad(lhs, getField(ref.getField()));
            method.addStatement(load);
        } else {
            throw new AnalysisException("Unhandled case: " + right);
        }
    }

    private void buildLeftNonLocal(JimpleMethod method, AssignStmt stmt, Value left) {
        Value right = stmt.getRightOp();
        Variable rhs;
        if (right instanceof Local) {
            rhs = getVariable((Local) right, method);
        } else if (right instanceof NullConstant) {
            return; // ignore null
        } else if (isConstant(right)) {
            rhs = getVariableOfConstant(right, method);
        } else {
            throw new AnalysisException("Unhandled case: " + right);
        }
        if (left instanceof InstanceFieldRef) {
            // x.f = y;
            InstanceFieldRef ref = (InstanceFieldRef) left;
            JimpleVariable base = getVariable((Local) ref.getBase(), method);
            InstanceStore store = new InstanceStore(base,
                    getField(ref.getField()), rhs);
            method.addStatement(store);
        } else if (left instanceof StaticFieldRef) {
            // T.f = x;
            StaticFieldRef ref = (StaticFieldRef) left;
            StaticStore store = new StaticStore(
                    getField(ref.getField()), rhs);
            method.addStatement(store);
        } else if (left instanceof ArrayRef) {
            // x[i] = y;
            // TODO: consider constant index?
            ArrayRef ref = (ArrayRef) left;
            JimpleVariable base = getVariable((Local) ref.getBase(), method);
            ArrayStore store = new ArrayStore(base, rhs);
            method.addStatement(store);
        } else {
            throw new AnalysisException("Unhandled case: " + left);
        }
    }

    private void buildIdentity(JimpleMethod method, IdentityStmt stmt) {
        // identity statement is for parameter passing and catch statements
        // parameters have been handled when creating JimpleMethod
        // currently ignore catch statements
    }

    private void buildCall(JimpleMethod method, Stmt stmt, Value left) {
        // x.m()     for left == null
        // r = x.m() for left != null
        Variable lhs = left != null && left.getType() instanceof RefLikeType
                ? getVariable((Local) left, method)
                : null;
        JimpleCallSite callSite = createCallSite(stmt, method);
        Call call = new Call(callSite, lhs);
        method.addStatement(call);
    }

    private void buildReturn(JimpleMethod method, ReturnStmt stmt) {
        if (stmt.getOp().getType() instanceof RefLikeType) {
            Value value = stmt.getOp();
            JimpleVariable ret;
            if (value instanceof Local) {
                ret = getVariable((Local) value, method);
            } else if (value instanceof NullConstant) {
                // return null;
                return; // ignore
            } else if (isConstant(value)) {
                ret = getVariableOfConstant(value, method);
            } else {
                throw new AnalysisException("Unhandled case: " + value);
            }
            method.addReturnVar(ret);
        }
    }

    private void buildThrow(JimpleMethod method, ThrowStmt stmt) {
        // currently ignore throw statements
    }
}
