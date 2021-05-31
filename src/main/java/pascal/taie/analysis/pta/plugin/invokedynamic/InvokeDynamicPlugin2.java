/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.pta.plugin.invokedynamic;

import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.plugin.util.CSObjUtils;
import pascal.taie.analysis.pta.plugin.util.Model;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.MethodHandle;
import pascal.taie.ir.exp.ReferenceLiteral;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.MapUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class InvokeDynamicPlugin2 implements Plugin {

    /**
     * Lambdas are supposed to be processed by LambdaPlugin.
     */
    private static final boolean processLambdas = false;
    // TODO add log warning when there is Lambdas while LambdaPlugin not in use

    private Solver solver;

    private CSManager csManager;

    private ContextSelector selector;

    private HeapModel heapModel;

    private ClassHierarchy hierarchy;

    private Context defContext;

    /**
     * Class type java.lang.invoke.MethodHandles$Lookup
     */
    private ClassType lookup;

    /**
     * Class type java.lang.invoke.MethodHandle
     */
    private ClassType methodHandle;

    /**
     * Method java.lang.invoke.ConstantCallSite.<init>(MethodHandle)
     */
    private JMethod constCallSiteCtor;

    private Model methodTypeModel;

    private Model lookupModel;

    /**
     * Map from method to the invokedynamic invocations included in the method.
     * Updated in {@link #onNewMethod}.
     */
    private final Map<JMethod, Set<Invoke>> method2indys = MapUtils.newMap();

    /**
     * Map from method (containing invokedynamic) to its context-sensitive methods.
     * Updated in {@link #onNewCSMethod}.
     */
    private final Map<JMethod, Set<CSMethod>> method2csMethod = MapUtils.newMap();

    /**
     * Map from variable that holds the MethodHandle to the corresponding
     * invokedynamic invocation site.
     * Updated in {@link #onNewMethod}.
     */
    private final Map<Var, Set<Invoke>> mhVar2indys = MapUtils.newMap();

    /**
     * Map from invokedynamic invocation site to the corresponding
     * MethodHandle bound to it.
     * Updated in {@link #onNewPointsToSet}.
     */
    private final Map<Invoke, Set<MethodHandle>> indy2mhs = MapUtils.newMap();

    /**
     * Map from base variable (arg0) to the corresponding invokedynamic
     * invocation sites.
     * Updated in {@link #onNewPointsToSet}
     */
    private final Map<Var, Set<Invoke>> base2Indys = MapUtils.newMap();

    /**
     * Description for MethodHandles.Lookup objects.
     */
    private static final String LOOKUP_DESC = "MethodHandlesLookupObj";

    /**
     * Map from class type to corresponding Method.Lookup object.
     */
    private final Map<ClassType, MockObj> lookupObjs = MapUtils.newMap();

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        csManager = solver.getCSManager();
        selector = solver.getContextSelector();
        heapModel = solver.getHeapModel();
        hierarchy = solver.getHierarchy();

        defContext = selector.getDefaultContext();
        lookup = hierarchy.getJREClass(StringReps.LOOKUP).getType();

        methodHandle = hierarchy.getJREClass(StringReps.METHOD_HANDLE).getType();
        constCallSiteCtor = hierarchy.getJREMethod(
                "<java.lang.invoke.ConstantCallSite: void <init>(java.lang.invoke.MethodHandle)>");
        methodTypeModel = new MethodTypeModel(solver);
        lookupModel = new LookupModel(solver);
    }

    @Override
    public void onNewMethod(JMethod method) {
        method.getIR().getStmts().forEach(stmt -> {
            if (stmt instanceof Invoke) {
                Invoke invoke = (Invoke) stmt;
                if (!invoke.isDynamic()) {
                    methodTypeModel.handleNewInvoke(invoke);
                    lookupModel.handleNewInvoke(invoke);
                }
                InvokeDynamic indy = getInvokeDynamic(invoke);
                if (indy != null) {
                    MapUtils.addToMapSet(method2indys, method, invoke);
                    // add BSM call edge
                    JMethod bsm = indy.getBootstrapMethodRef().resolve();
                    extractMHVars(bsm).forEach(mhVar ->
                            MapUtils.addToMapSet(mhVar2indys, mhVar, invoke));
                    addBSMCallEdge(invoke, bsm);
                }
            }
        });
    }

    private static @Nullable InvokeDynamic getInvokeDynamic(Invoke invoke) {
        InvokeExp invokeExp = invoke.getInvokeExp();
        if (invokeExp instanceof InvokeDynamic) {
            if (processLambdas || !LambdaPlugin.isLambdaMetaFactory(invoke)) {
                return (InvokeDynamic) invokeExp;
            }
        }
        return null;
    }

    private Stream<Var> extractMHVars(JMethod bsm) {
        return bsm.getIR().getStmts()
                .stream()
                .filter(s -> s instanceof Invoke)
                .map(s -> ((Invoke) s).getInvokeExp())
                .map(i -> {
                    JMethod target = i.getMethodRef().resolve();
                    if (target.equals(constCallSiteCtor)) {
                        return i.getArg(0);
                    }
                    if (target.getName().equals("setTarget")) {
                        Var tg = i.getArg(0);
                        if (tg.getType().equals(methodHandle)) {
                            return tg;
                        }
                    }
                    return null;
                })
                .filter(Objects::nonNull);
    }

    private void addBSMCallEdge(Invoke invoke, JMethod bsm) {
        // TODO: use 1-call-site sensitivity for BSM
        BSMCallEdge edge = new BSMCallEdge(
                csManager.getCSCallSite(defContext, invoke),
                csManager.getCSMethod(defContext, bsm));
        solver.addCallEdge(edge);
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        if (edge instanceof BSMCallEdge) {
            // pass arguments of boostrap method, for details, please refer to
            // https://docs.oracle.com/javase/7/docs/api/java/lang/invoke/package-summary.html
            Invoke invoke = edge.getCallSite().getCallSite();
            InvokeDynamic indy = (InvokeDynamic) invoke.getInvokeExp();
            Context context = edge.getCallee().getContext();
            IR ir = edge.getCallee().getMethod().getIR();
            // arg 0: MethodHandles.Lookup object
            solver.addVarPointsTo(context, ir.getParam(0), defContext,
                    getLookupObj(invoke));
            // arg 1: method name
            solver.addVarPointsTo(context, ir.getParam(1), defContext,
                    heapModel.getConstantObj(StringLiteral.get(indy.getMethodName())));
            // arg 2: MethodType object
            solver.addVarPointsTo(context, ir.getParam(2), defContext,
                    heapModel.getConstantObj(indy.getMethodType()));
            // arg 3+: optionally, additional static arguments
            for (int i = 0, j = 2;
                 i < indy.getBootstrapArgs().size() && j < ir.getParams().size();
                 ++i, ++j) {
                Literal arg = indy.getBootstrapArgs().get(i);
                if (arg instanceof ReferenceLiteral) {
                    CSObj argObj = csManager.getCSObj(defContext,
                            heapModel.getConstantObj((ReferenceLiteral) arg));
                    solver.addVarPointsTo(context, ir.getParam(j), argObj);
                }
            }
            // TODO: mock array for varargs
        }
        if (edge instanceof InvokeDynamicCallEdge) {
            JMethod callee = edge.getCallee().getMethod();
            // pass arguments
            int shift;
            if (callee.isStatic() || callee.isConstructor()) {
                shift = 0;
            } else {
                shift = 1;
                // TODO: consider case of outer class, where shift = 2
            }
            Context callerCtx = edge.getCallSite().getContext();
            Invoke caller = edge.getCallSite().getCallSite();
            List<Var> args = caller.getInvokeExp().getArgs();
            Context calleeCtx = edge.getCallee().getContext();
            List<Var> params = callee.getIR().getParams();
            for (int i = shift, j = 0;
                 i < args.size() && j < params.size(); ++i, ++j) {
                // TODO: filter unconcerned pointers
                solver.addPFGEdge(
                        csManager.getCSVar(callerCtx, args.get(i)),
                        csManager.getCSVar(calleeCtx, params.get(j)),
                        PointerFlowEdge.Kind.PARAMETER_PASSING);
            }
            // pass return values
            Var result = caller.getResult();
            if (result != null) { // TODO: filter unconcerned pointers
                CSVar csResult = csManager.getCSVar(callerCtx, result);
                callee.getIR().getReturnVars().forEach(ret -> {
                    CSVar csRet = csManager.getCSVar(calleeCtx, ret);
                    solver.addPFGEdge(csRet, csResult, PointerFlowEdge.Kind.RETURN);
                });
            }
        }
    }

    private MockObj getLookupObj(Invoke invoke) {
        ClassType type = invoke.getContainer().getDeclaringClass().getType();
        return lookupObjs.computeIfAbsent(type,
                t -> new MockObj(LOOKUP_DESC, t, lookup));
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        Var var = csVar.getVar();
        if (lookupModel.isRelevantVar(var)) {
            lookupModel.handleNewPointsToSet(csVar, pts);
        }
        if (methodTypeModel.isRelevantVar(var)) {
            methodTypeModel.handleNewPointsToSet(csVar, pts);
        }
        if (mhVar2indys.containsKey(var)) {
            pts.forEach(csObj -> {
                MethodHandle mh = CSObjUtils.toMethodHandle(csObj);
                if (mh != null) {
                    mhVar2indys.get(var).forEach(invoke ->
                            handleNewMethodHandle(invoke, mh));
                }
            });
        }
        if (base2Indys.containsKey(var)) {
            // TODO: dispatch and add invokedynamic call edge
        }
    }

    private void handleNewMethodHandle(Invoke invoke, MethodHandle mh) {
        if (MapUtils.addToMapSet(indy2mhs, invoke, mh)) {
            System.out.println(mh);
            switch (mh.getKind()) {
                case REF_invokeVirtual: { // record
                    Var base = invoke.getInvokeExp().getArg(0);
                    MapUtils.addToMapSet(base2Indys, base, invoke);
                    // TODO: consider outer class, where arg1 is
                    //  the enclosing object
                    break;
                }
                case REF_invokeStatic: { // add invokedynamic call edge
                    break;
                }
            }
        }
    }

    private void handleInvokeDynamic(Context context, Invoke invoke, MethodHandle mh) {

    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        JMethod method = csMethod.getMethod();
        if (method2indys.containsKey(method)) {
            MapUtils.addToMapSet(method2csMethod, method, csMethod);
            Context context = csMethod.getContext();
            method2indys.get(method).forEach(invoke ->
                    indy2mhs.get(invoke).forEach(mh ->
                            handleInvokeDynamic(context, invoke, mh)));
        }
    }
}
