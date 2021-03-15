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
import pascal.taie.ir.IR;
import pascal.taie.java.ClassHierarchy;
import pascal.taie.java.classes.JClass;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.types.Type;
import pascal.taie.java.types.VoidType;
import pascal.taie.oldpta.env.Environment;
import pascal.taie.oldpta.ir.Allocation;
import pascal.taie.oldpta.ir.ArrayLoad;
import pascal.taie.oldpta.ir.ArrayStore;
import pascal.taie.oldpta.ir.Assign;
import pascal.taie.oldpta.ir.AssignCast;
import pascal.taie.oldpta.ir.Call;
import pascal.taie.oldpta.ir.CallSite;
import pascal.taie.oldpta.ir.DefaultCallSite;
import pascal.taie.oldpta.ir.DefaultPTAIR;
import pascal.taie.oldpta.ir.DefaultVariable;
import pascal.taie.oldpta.ir.PTAIR;
import pascal.taie.oldpta.ir.InstanceLoad;
import pascal.taie.oldpta.ir.InstanceStore;
import pascal.taie.oldpta.ir.NormalObj;
import pascal.taie.oldpta.ir.Obj;
import pascal.taie.oldpta.ir.Statement;
import pascal.taie.oldpta.ir.StaticLoad;
import pascal.taie.oldpta.ir.StaticStore;
import pascal.taie.oldpta.ir.Variable;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.Timer;
import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.RefLikeType;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static pascal.taie.util.CollectionUtils.newConcurrentMap;

/**
 * Jimple-based pointer analysis IR builder.
 */
class IRBuilder implements pascal.taie.java.IRBuilder {

    private final ConcurrentMap<JMethod, Map<Local, Variable>> vars
            = newConcurrentMap();

    private final Converter converter;

    private final NewVariableManager varManager = new NewVariableManager();

    private final Environment env;

    IRBuilder(Converter converter, Environment env) {
        this.converter = converter;
        this.env = env;
    }

    @Override
    public IR buildIR(JMethod method) {
        return new MethodIRBuilder(method, converter).build();
    }

    /**
     * Build IR for all methods in given class hierarchy.
     */
    @Override
    public void buildAll(ClassHierarchy hierarchy) {
        buildAll(hierarchy, JMethod::getIR);
    }

    @Override
    public void buildAllPTA(ClassHierarchy hierarchy) {
        buildAll(hierarchy, JMethod::getPTAIR);
    }

    private void buildAll(ClassHierarchy hierarchy, Consumer<JMethod> builder) {
        Timer timer = new Timer("Build IR for all methods");
        timer.start();
        int nThreads = Runtime.getRuntime().availableProcessors();
        // Group all methods by number of threads
        List<List<JMethod>> groups = new ArrayList<>();
        for (int i = 0; i < nThreads; ++i) {
            groups.add(new ArrayList<>());
        }
        List<JClass> classes = new ArrayList<>(hierarchy.getAllClasses());
        int i = 0;
        for (JClass c : classes) {
            for (JMethod m : c.getDeclaredMethods()) {
                if (!m.isAbstract() || m.isNative()) {
                    groups.get(i++ % nThreads).add(m);
                }
            }
        }
        // Build IR for all methods in parallel
        ExecutorService service = Executors.newFixedThreadPool(nThreads);
        for (List<JMethod> group : groups) {
            service.execute(() -> group.forEach(builder));
        }
        service.shutdown();
        try {
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        timer.stop();
        System.out.println(timer);
    }

    @Override
    public PTAIR buildPTAIR(JMethod method) {
        DefaultPTAIR ir = new DefaultPTAIR(method);
        if (method.isNative()) {
            buildNative(method, ir);
        } else if (!method.isAbstract()) {
            buildConcrete(method, ir);
        }
        env.processNativeCode(ir);
        return ir;
    }

    private void buildNative(JMethod method, DefaultPTAIR ir) {
        if (!method.isStatic()) {
            ir.setThis(varManager.getThisVariable(method));
        }
        int paramCount = method.getParamCount();
        if (paramCount > 0) {
            List<Variable> params = new ArrayList<>(paramCount);
            for (int i = 0; i < paramCount; ++i) {
                params.add(varManager.getParameter(method, i));
            }
            ir.setParams(Collections.unmodifiableList(params));
        }
        if (!(method.getReturnType() instanceof VoidType)) {
            ir.setReturnVars(Collections.singleton(
                    varManager.getReturnVariable(method)));
        }
    }

    private void buildConcrete(JMethod method, DefaultPTAIR ir) {
        SootMethod sootMethod = (SootMethod) method.getMethodSource();
        Body body = sootMethod.retrieveActiveBody();
        // add this variable
        if (!method.isStatic()) {
            ir.setThis(getVariable(body.getThisLocal(), method));
        }
        // add parameters
        if (body.getParameterLocals().size() > 0) {
            List<Variable> params = new ArrayList<>(
                    body.getParameterLocals().size());
            for (Local param : body.getParameterLocals()) {
                params.add(getVariable(param, method));
            }
            ir.setParams(Collections.unmodifiableList(params));
        }
        // add statements
        RelevantUnitSwitch sw = new RelevantUnitSwitch();
        for (Unit unit : body.getUnits()) {
            unit.apply(sw);
            if (sw.isRelevant()) {
                if (unit instanceof AssignStmt) {
                    buildAssign(method, ir, (AssignStmt) unit);
                } else if (unit instanceof IdentityStmt) {
                    buildIdentity(method, ir, (IdentityStmt) unit);
                } else if (unit instanceof InvokeStmt) {
                    buildCall(method, ir, ((InvokeStmt) unit), null);
                } else if (unit instanceof ReturnStmt) {
                    buildReturn(method, ir, (ReturnStmt) unit);
                } else if (unit instanceof ThrowStmt) {
                    buildThrow(method, ir, (ThrowStmt) unit);
                } else {
                    throw new RuntimeException("Cannot handle statement: " + unit);
                }
            }
        }
    }

    private void buildAssign(JMethod method, DefaultPTAIR ir, AssignStmt stmt) {
        Value left = stmt.getLeftOp();
        if (stmt.containsInvokeExpr()) {
            buildCall(method, ir, stmt, left);
        } else if (left.getType() instanceof RefLikeType) {
            // only build statements for non-primitive types
            if (left instanceof Local) {
                buildLeftLocal(method, ir, stmt, (Local) left);
            } else {
                buildLeftNonLocal(method, ir, stmt, left);
            }
        }
    }

    private void buildLeftLocal(JMethod method, DefaultPTAIR ir,
                                AssignStmt stmt, Local left) {
        Variable lhs = getVariable(left, method);
        Value right = stmt.getRightOp();
        if (right instanceof NewExpr
                || right instanceof NewArrayExpr) {
            // x = new T();
            // x = new T[];
            addStatement(ir, new Allocation(lhs, createObject(stmt, method)), stmt);
        } else if (right instanceof NewMultiArrayExpr) {
            // x = new T[][]...;
            newMultiArray(stmt, lhs, (ArrayType) right.getType(), method, ir);
        } else if (right instanceof NullConstant) {
            // x = null;
            // ignore
        } else if (isConstant(right)) {
            addStatement(ir, new Allocation(lhs, getConstantObj(right)), stmt);
        } else if (right instanceof Local) {
            // x = y;
            addStatement(ir, new Assign(lhs, getVariable((Local) right, method)), stmt);
        } else if (right instanceof CastExpr) {
            // x = (T) y;
            CastExpr cast = (CastExpr) right;
            Value op = cast.getOp();
            if (op instanceof Local) {
                addStatement(ir, new AssignCast(lhs,
                                converter.convertType(cast.getCastType()),
                                getVariable((Local) cast.getOp(), method)),
                        stmt);
            } else {
                // TODO: handle constants
            }
        } else if (right instanceof PhiExpr) {
            // x = phi(v1, ..., vn)
            for (Value from : ((PhiExpr) right).getValues()) {
                addStatement(ir,
                        new Assign(lhs, getVariable((Local) from, method)),
                        stmt);
            }
        } else if (right instanceof InstanceFieldRef) {
            // x = y.f;
            InstanceFieldRef ref = (InstanceFieldRef) right;
            Variable base = getVariable((Local) ref.getBase(), method);
            InstanceLoad load = new InstanceLoad(lhs, base,
                    converter.convertFieldRef(ref.getFieldRef()));
            addStatement(ir, load, stmt);
        } else if (right instanceof ArrayRef) {
            // x = y[i];
            // TODO: consider constant index?
            ArrayRef ref = (ArrayRef) right;
            Variable base = getVariable((Local) ref.getBase(), method);
            ArrayLoad load = new ArrayLoad(lhs, base);
            addStatement(ir, load, stmt);
        } else if (right instanceof StaticFieldRef) {
            // x = T.f;
            StaticFieldRef ref = (StaticFieldRef) right;
            StaticLoad load = new StaticLoad(lhs,
                    converter.convertFieldRef(ref.getFieldRef()));
            addStatement(ir, load, stmt);
        } else {
            throw new AnalysisException("Unhandled case: " + right);
        }
    }

    private void buildLeftNonLocal(JMethod method, DefaultPTAIR ir,
                                   AssignStmt stmt, Value left) {
        Value right = stmt.getRightOp();
        Variable rhs;
        if (right instanceof Local) {
            rhs = getVariable((Local) right, method);
        } else if (right instanceof NullConstant) {
            return; // ignore null
        } else if (isConstant(right)) {
            rhs = getVariableOfConstant(right, method, ir);
        } else {
            throw new AnalysisException("Unhandled case: " + right);
        }
        if (left instanceof InstanceFieldRef) {
            // x.f = y;
            InstanceFieldRef ref = (InstanceFieldRef) left;
            Variable base = getVariable((Local) ref.getBase(), method);
            InstanceStore store = new InstanceStore(base,
                    converter.convertFieldRef(ref.getFieldRef()), rhs);
            addStatement(ir, store, stmt);
        } else if (left instanceof StaticFieldRef) {
            // T.f = x;
            StaticFieldRef ref = (StaticFieldRef) left;
            StaticStore store = new StaticStore(
                    converter.convertFieldRef(ref.getFieldRef()), rhs);
            addStatement(ir, store, stmt);
        } else if (left instanceof ArrayRef) {
            // x[i] = y;
            // TODO: consider constant index?
            ArrayRef ref = (ArrayRef) left;
            Variable base = getVariable((Local) ref.getBase(), method);
            ArrayStore store = new ArrayStore(base, rhs);
            addStatement(ir, store, stmt);
        } else {
            throw new AnalysisException("Unhandled case: " + left);
        }
    }

    private void buildIdentity(JMethod method, DefaultPTAIR ir, IdentityStmt stmt) {
        // identity statement is for parameter passing and catch statements
        // parameters have been handled when creating JimpleMethod
        // currently ignore catch statements
    }

    private void buildCall(JMethod method, DefaultPTAIR ir, Stmt stmt, Value left) {
        // x.m()     for left == null
        // r = x.m() for left != null
        Variable lhs = left != null && left.getType() instanceof RefLikeType
                ? getVariable((Local) left, method)
                : null;
        CallSite callSite = createCallSite(stmt, method, ir);
        Call call = new Call(callSite, lhs);
        addStatement(ir, call, stmt);
    }

    private CallSite createCallSite(Stmt stmt, JMethod container, DefaultPTAIR ir) {
        InvokeExpr invoke = stmt.getInvokeExpr();
        DefaultCallSite callSite = new DefaultCallSite(
                JimpleCallUtils.getCallKind(invoke));
        callSite.setStmt(stmt); // TODO: <-- get rid of this
        callSite.setMethodRef(
                converter.convertMethodRef(invoke.getMethodRef()));
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
                        args.add(getVariableOfConstant(arg, container, ir));
                    } else {
                        throw new AnalysisException("Unhandled argument: " + arg);
                    }
                } else { // null for arguments of primitive type or null
                    args.add(null);
                }
            }
            callSite.setArguments(Collections.unmodifiableList(args));
        }
        callSite.setContainerMethod(container);
        return callSite;
    }

    private void buildReturn(JMethod method, DefaultPTAIR ir, ReturnStmt stmt) {
        if (stmt.getOp().getType() instanceof RefLikeType) {
            Value value = stmt.getOp();
            Variable ret;
            if (value instanceof Local) {
                ret = getVariable((Local) value, method);
            } else if (value instanceof NullConstant) {
                // return null;
                return; // ignore
            } else if (isConstant(value)) {
                ret = getVariableOfConstant(value, method, ir);
            } else {
                throw new AnalysisException("Unhandled case: " + value);
            }
            ir.addReturnVar(ret);
        }
    }

    private void buildThrow(JMethod method, DefaultPTAIR ir, ThrowStmt stmt) {
        // currently ignore throw statements
    }

    private void addStatement(DefaultPTAIR ir, Statement stmt, Stmt sootStmt) {
        stmt.setStartLineNumber(sootStmt.getJavaSourceStartLineNumber());
        ir.addStatement(stmt);
    }

    private Variable getVariable(Local var, JMethod container) {
        return vars.computeIfAbsent(container, (m) -> new LinkedHashMap<>())
                .computeIfAbsent(var, (v) -> {
                    Type type = converter.convertType(v.getType());
                    return new DefaultVariable(v.getName(), type, container);
                });
    }

    /**
     * Given a constant, returns the temporary variable which points
     * to the constant. To achieve this, the corresponding Allocation statement
     * (from the constant value to the variable) will be added
     * to the container method.
     */
    private Variable getVariableOfConstant(
            Value constant, JMethod container, DefaultPTAIR ir) {
        Obj obj = getConstantObj(constant);
        Variable temp = varManager.newTempVariable(
                "constant$", converter.convertType(constant.getType()),
                container);
        ir.addStatement(new Allocation(temp, obj));
        return temp;
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
            return env.getClassObj(
                    converter.convertType(((ClassConstant) constant).toSootType()));
        } else if (constant instanceof MethodHandle) {
            throw new UnsupportedOperationException("MethodHandle is not supported");
        } else if (constant instanceof NumericConstant) {
            throw new AnalysisException("Unhandled numeric constant: " + constant);
        } else {
            throw new AnalysisException("Unhandled case: " + constant);
        }
    }

    private Obj createObject(AssignStmt alloc, JMethod container) {
        return new NormalObj(
                converter.convertType(alloc.getRightOp().getType()),
                container);
    }

    /**
     * Processes allocation of multi-array, which needs to be
     * allocated separately for every dimension of the array.
     */
    private void newMultiArray(AssignStmt alloc, Variable lhs, ArrayType arrayType,
                               JMethod container, DefaultPTAIR ir) {
        Obj array = new NormalObj(converter.convertType(arrayType), container);
        addStatement(ir, new Allocation(lhs, array), alloc);
        soot.Type elemType = arrayType.getElementType();
        if (elemType instanceof ArrayType) {
            Variable temp = varManager.newTempVariable("array$",
                    converter.convertType(arrayType), container);
            newMultiArray(alloc, temp, (ArrayType) elemType, container, ir);
            addStatement(ir, new ArrayStore(lhs, temp), alloc);
        }
    }
}
