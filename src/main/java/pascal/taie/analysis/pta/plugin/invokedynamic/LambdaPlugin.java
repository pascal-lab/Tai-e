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
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.MethodHandle;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.CollectionUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.util.collection.MapUtils.addToMapMap;
import static pascal.taie.util.collection.MapUtils.addToMapSet;
import static pascal.taie.util.collection.MapUtils.getMapMap;
import static pascal.taie.util.collection.MapUtils.newMap;

public class LambdaPlugin implements Plugin {

    /**
     * Description for lambda functional objects.
     */
    public static final String LAMBDA_DESC = "LambdaObj";

    /**
     * Description for objects created by lambda constructor.
     */
    public static final String LAMBDA_NEW_DESC = "LambdaConstructedObj";

    private Solver solver;

    private ContextSelector selector;

    private ClassHierarchy hierarchy;

    private CSManager csManager;

    /**
     * Map from method to the lambda functional objects created in the method.
     */
    private final Map<JMethod, Set<MockObj>> lambdaObjs = newMap();

    /**
     * Map from Invoke (of invokedynamic) and type to mock obj to avoid mocking same objects
     */
    private final Map<Invoke, Map<ClassType, MockObj>> newObjs = newMap();

    /**
     * Map from receiver variable to the information about the related
     * instance invocation sites. When new objects reach the receiver variable,
     * these information will be used to build lambda call edges.
     */
    private final Map<CSVar, Set<InstanceInvoInfo>> invoInfos = newMap();

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        this.selector = solver.getContextSelector();
        this.hierarchy = solver.getHierarchy();
        this.csManager = solver.getCSManager();
    }

    @Override
    public void onNewMethod(JMethod method) {
        extractLambdaMetaFactories(method.getIR()).forEach(invoke -> {
            InvokeDynamic indy = (InvokeDynamic) invoke.getInvokeExp();
            Type type = indy.getMethodType().getReturnType();
            JMethod container = invoke.getContainer();
            // record lambda meta factories of new discovered methods
            addToMapSet(lambdaObjs, container,
                    new MockObj(LAMBDA_DESC, invoke, type, container));
        });
    }

    private static Stream<Invoke> extractLambdaMetaFactories(IR ir) {
        return ir.getStmts()
                .stream()
                .filter(s -> s instanceof Invoke)
                .map(s -> (Invoke) s)
                .filter(LambdaPlugin::isLambdaMetaFactory);
    }

    static boolean isLambdaMetaFactory(Invoke invoke) {
        if (invoke.getInvokeExp() instanceof InvokeDynamic) {
            JMethod bsm = ((InvokeDynamic) invoke.getInvokeExp())
                    .getBootstrapMethodRef().resolve();
            String bsmSig = bsm.getSignature();
            return bsmSig.equals(StringReps.LAMBDA_METAFACTORY) ||
                    bsmSig.equals(StringReps.LAMBDA_ALTMETAFACTORY);
        }
        return false;
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        JMethod method = csMethod.getMethod();
        Set<MockObj> lambdas = lambdaObjs.get(method);
        if (lambdas != null) {
            Context context = csMethod.getContext();
            lambdas.forEach(lambdaObj -> {
                // propagate lambda functional objects
                Invoke invoke = (Invoke) lambdaObj.getAllocation();
                Var ret = invoke.getResult();
                assert ret != null;
                // here we use full method context as the heap context of
                // lambda object, so that it can be directly used to obtain
                // captured values.
                solver.addVarPointsTo(context, ret, context, lambdaObj);
            });
        }
    }

    @Override
    public void onUnresolvedCall(CSObj recv, Context context, Invoke invoke) {
        if (!isLambdaObj(recv.getObject())) {
            return;
        }
        MockObj lambdaObj = (MockObj) recv.getObject();
        Invoke indyInvoke = (Invoke) lambdaObj.getAllocation();
        InvokeDynamic indy = (InvokeDynamic) indyInvoke.getInvokeExp();
        Context indyCtx = recv.getContext();
        CSCallSite csCallSite = csManager.getCSCallSite(context, invoke);
        MethodHandle mh = (MethodHandle) indy.getBootstrapArgs().get(1);
        final JMethod target = mh.getMethodRef().resolve();

        switch (mh.getKind()) {
            case REF_newInvokeSpecial: { // target is constructor
                ClassType type = target.getDeclaringClass().getType();
                // Create mock object (if absent) which represents
                // the newly-allocated object
                MockObj newObj = getMapMap(newObjs, indyInvoke, type);
                if (newObj == null) {
                    // TODO: use heapModel to process mock obj?
                    newObj = new MockObj(LAMBDA_NEW_DESC, indyInvoke, type,
                            indyInvoke.getContainer());
                    addToMapMap(newObjs, indyInvoke, type, newObj);
                }
                // Pass the mock object to LHS variable (if present)
                // TODO: double-check if the heap context is proper
                Var result = invoke.getResult();
                if (result != null) {
                    solver.addVarPointsTo(context, result, context, newObj);
                }
                //  Pass the mock object to 'this' variable of the constructor
                CSObj csNewObj = csManager.getCSObj(context, newObj);
                Context ctorCtx = selector.selectContext(csCallSite, csNewObj, target);
                solver.addVarPointsTo(ctorCtx, target.getIR().getThis(),
                        context, newObj);
                // Add call edge to constructor
                addLambdaCallEdge(csCallSite, csNewObj, target, indy, indyCtx);
                break;
            }
            case REF_invokeInterface:
            case REF_invokeVirtual:
            case REF_invokeSpecial: { // target is instance methods
                List<Var> actualArgs = invoke.getInvokeExp().getArgs();
                List<Var> lambdaArgs = indy.getArgs();
                // Obtain receiver variable and context
                Var recvVar;
                Context recvCtx;
                if (lambdaArgs.isEmpty()) {
                    recvVar = actualArgs.get(0);
                    recvCtx = context;
                } else {
                    recvVar = lambdaArgs.get(0);
                    recvCtx = recv.getContext();
                }
                CSVar csRecvVar = csManager.getCSVar(recvCtx, recvVar);
                solver.getPointsToSetOf(csRecvVar).forEach(recvObj -> {
                    // Handle receiver objects
                    Type rectType = recvObj.getObject().getType();
                    JMethod callee = hierarchy.dispatch(rectType, target.getRef());
                    if (callee != null) {
                        addLambdaCallEdge(csCallSite, recvObj, callee, indy, indyCtx);
                    }
                });
                // New objects may reach csRecvVar later, thus we store it
                // together with information about the related Lambda invocation.
                addToMapSet(invoInfos, csRecvVar,
                        new InstanceInvoInfo(csCallSite, indy, indyCtx));
                break;
            }
            case REF_invokeStatic: {
                addLambdaCallEdge(csCallSite, null, target, indy, indyCtx);
                break;
            }
            default:
                throw new AnalysisException(mh.getKind() + " is not supported");
        }
    }

    private static boolean isLambdaObj(Obj obj) {
        return obj instanceof MockObj &&
                ((MockObj) obj).getDescription().equals(LAMBDA_DESC);
    }

    private void addLambdaCallEdge(
            CSCallSite csCallSite, @Nullable CSObj recv, JMethod callee,
            InvokeDynamic indy, Context indyCtx) {
        Context calleeCtx;
        if (recv != null) {
            calleeCtx = selector.selectContext(csCallSite, recv, callee);
        } else {
            calleeCtx = selector.selectContext(csCallSite, callee);
        }
        LambdaCallEdge callEdge = new LambdaCallEdge(csCallSite,
                csManager.getCSMethod(calleeCtx, callee), indy, indyCtx);
        solver.addCallEdge(callEdge);
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        if (edge instanceof LambdaCallEdge) {
            LambdaCallEdge lambdaCallEdge = (LambdaCallEdge) edge;
            CSCallSite csCallSite = lambdaCallEdge.getCallSite();
            CSMethod csCallee = lambdaCallEdge.getCallee();

            Invoke invoke = csCallSite.getCallSite();
            Var invokeResult = invoke.getResult();
            List<Var> actualArgs = invoke.getInvokeExp().getArgs();
            JMethod target = csCallee.getMethod();
            List<Var> lambdaArgs = lambdaCallEdge.getLambdaArgs();

            Context callerContext = csCallSite.getContext();
            Context calleeContext = csCallee.getContext();
            Context lambdaContext = lambdaCallEdge.getLambdaContext();

            // shift flags for passing parameters
            int shiftFlagK = 0;
            int shiftFlagN = 0;
            int capturedCount = lambdaArgs.size(); // #i
            List<Var> implParams = target.getIR().getParams();

            if (!target.isStatic() && !target.isConstructor()) {
                shiftFlagK = capturedCount == 0 ? 0 : 1;
                shiftFlagN = 1 - shiftFlagK;
            }

            // pass parameters: from actual parameters & from captured values
            if (implParams.size() != 0) {
                for (int i = 0; i + shiftFlagK <= capturedCount && i < lambdaArgs.size(); i++) {
                    if (i - shiftFlagK < 0) {
                        continue;
                    }
                    solver.addPFGEdge(
                            csManager.getCSVar(lambdaContext, lambdaArgs.get(i)),
                            csManager.getCSVar(calleeContext, implParams.get(i - shiftFlagK)),
                            PointerFlowEdge.Kind.PARAMETER_PASSING);
                }

                for (int i = 0; i < actualArgs.size(); i++) {
                    int index = capturedCount - (shiftFlagK + shiftFlagN) + i;
                    if (index < 0) {
                        continue;
                    }
                    if (index >= implParams.size()) {
                        break;
                    }
                    solver.addPFGEdge(
                            csManager.getCSVar(callerContext, actualArgs.get(i)),
                            csManager.getCSVar(calleeContext, implParams.get(index)),
                            PointerFlowEdge.Kind.PARAMETER_PASSING);
                }
            }

            // pass return values
            List<Var> returnValues = target.getIR().getReturnVars();
            if (invokeResult != null && !CollectionUtils.isEmpty(returnValues)) {
                CSVar csInvokeResult = csManager.getCSVar(callerContext, invokeResult);
                returnValues.stream()
                        .map(r -> csManager.getCSVar(calleeContext, r))
                        .forEach(r ->
                                solver.addPFGEdge(r, csInvokeResult, PointerFlowEdge.Kind.RETURN));
            }

            if (shiftFlagK == 1 && !CollectionUtils.isEmpty(lambdaArgs)) {
                solver.addPFGEdge(
                        csManager.getCSVar(lambdaContext, lambdaArgs.get(0)),
                        csManager.getCSVar(calleeContext, target.getIR().getThis()),
                        PointerFlowEdge.Kind.LOCAL_ASSIGN);
            }

            if (shiftFlagN == 1 && !CollectionUtils.isEmpty(actualArgs)) {
                solver.addPFGEdge(
                        csManager.getCSVar(callerContext, actualArgs.get(0)),
                        csManager.getCSVar(calleeContext, target.getIR().getThis()),
                        PointerFlowEdge.Kind.LOCAL_ASSIGN);
            }
        }
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        Set<InstanceInvoInfo> infos = invoInfos.get(csVar);
        if (infos == null) {
            return;
        }
        infos.forEach(info -> {
            InvokeDynamic indy = info.getLambdaIndy();
            MethodHandle mh = (MethodHandle) indy.getBootstrapArgs().get(1);
            MethodRef targetRef = mh.getMethodRef();
            pts.forEach(recvObj -> {
                Type recvType = recvObj.getObject().getType();
                JMethod callee = hierarchy.dispatch(recvType, targetRef);
                if (callee != null) {
                    addLambdaCallEdge(info.getCSCallSite(), recvObj,
                            callee, indy, info.getLambdaContext());
                }
            });
        });
    }
}
