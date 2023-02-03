/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
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
import pascal.taie.language.classes.Signatures;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;

public class LambdaAnalysis implements Plugin {

    /**
     * Description for lambda functional objects.
     */
    private static final String LAMBDA_DESC = "LambdaObj";

    /**
     * Description for objects created by lambda constructor.
     */
    private static final String LAMBDA_NEW_DESC = "LambdaConstructedObj";

    private Solver solver;

    private HeapModel heapModel;

    private ContextSelector selector;

    private ClassHierarchy hierarchy;

    private CSManager csManager;

    /**
     * Map from method to the lambda functional objects created in the method.
     */
    private final MultiMap<JMethod, Obj> lambdaObjs = Maps.newMultiMap();

    /**
     * Map from receiver variable to the information about the related
     * instance invocation sites. When new objects reach the receiver variable,
     * this information will be used to build lambda call edges.
     */
    private final MultiMap<CSVar, InstanceInvoInfo> invoInfos = Maps.newMultiMap();

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        this.heapModel = solver.getHeapModel();
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
            // record lambda meta factories of new reachable methods
            lambdaObjs.put(container,
                    heapModel.getMockObj(LAMBDA_DESC, invoke, type, container));
        });
    }

    private static Stream<Invoke> extractLambdaMetaFactories(IR ir) {
        return ir.invokes(true)
                .filter(Invoke::isDynamic)
                .filter(LambdaAnalysis::isLambdaMetaFactory);
    }

    static boolean isLambdaMetaFactory(Invoke invoke) {
        // Declaring class of bootstrap method reference may be phantom
        // (looks like an issue of current (soot-based) front end),
        // thus we use resolveNullable() to avoid exception.
        // TODO: change to resolve() after using new front end
        JMethod bsm = ((InvokeDynamic) invoke.getInvokeExp())
                .getBootstrapMethodRef()
                .resolveNullable();
        if (bsm != null) {
            String bsmSig = bsm.getSignature();
            return bsmSig.equals(Signatures.LAMBDA_METAFACTORY) ||
                    bsmSig.equals(Signatures.LAMBDA_ALTMETAFACTORY);
        } else {
            return false;
        }
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        JMethod method = csMethod.getMethod();
        Context context = csMethod.getContext();
        lambdaObjs.get(method).forEach(lambdaObj -> {
            // propagate lambda functional objects
            Invoke invoke = (Invoke) lambdaObj.getAllocation();
            Var ret = invoke.getResult();
            assert ret != null;
            // here we use full method context as the heap context of
            // lambda object, so that it can be directly used to obtain
            // context-sensitive captured values later.
            solver.addVarPointsTo(context, ret, context, lambdaObj);
        });
    }

    @Override
    public void onUnresolvedCall(CSObj recv, Context context, Invoke invoke) {
        if (!isLambdaObj(recv.getObject())) {
            return;
        }
        MockObj lambdaObj = (MockObj) recv.getObject();
        Invoke indyInvoke = (Invoke) lambdaObj.getAllocation();
        InvokeDynamic indy = (InvokeDynamic) indyInvoke.getInvokeExp();
        if (!indy.getMethodName().equals(invoke.getMethodRef().getName())) {
            // Use method name to filter out mismatched (caused by imprecision
            // of pointer analysis) lambda objects and actual invocation sites.
            // TODO: use more information to filter out mismatches?
            return;
        }
        Context indyCtx = recv.getContext();
        CSCallSite csCallSite = csManager.getCSCallSite(context, invoke);
        MethodHandle mh = getMethodHandle(indy);
        final MethodRef targetRef = mh.getMethodRef();

        switch (mh.getKind()) {
            case REF_newInvokeSpecial -> { // targetRef is constructor
                ClassType type = targetRef.getDeclaringClass().getType();
                // Create mock object (if absent) which represents
                // the newly-allocated object. Note that here we use the
                // *invokedynamic* to represent the *allocation site*,
                // instead of the actual invocation site of the constructor.
                Obj newObj = heapModel.getMockObj(LAMBDA_NEW_DESC,
                        indyInvoke, type, indyInvoke.getContainer());
                // pass the mock object to result variable (if present)
                // TODO: double-check if the heap context is proper
                CSObj csNewObj = csManager.getCSObj(context, newObj);
                Var result = invoke.getResult();
                if (result != null) {
                    solver.addVarPointsTo(context, result, csNewObj);
                }
                // add call edge to constructor
                addLambdaCallEdge(csCallSite, csNewObj, targetRef, indy, indyCtx);
            }
            case REF_invokeInterface, REF_invokeVirtual, REF_invokeSpecial -> {
                // targetRef is instance method
                List<Var> capturedArgs = indy.getArgs();
                List<Var> actualArgs = invoke.getInvokeExp().getArgs();
                // Obtain receiver variable and context
                Var recvVar;
                Context recvCtx;
                if (!capturedArgs.isEmpty()) {
                    // if captured arguments are not empty, then the first one
                    // must be the receiver object for targetRef
                    recvVar = capturedArgs.get(0);
                    recvCtx = indyCtx;
                } else {
                    // otherwise, the first actual argument is the receiver
                    recvVar = actualArgs.get(0);
                    recvCtx = context;
                }
                CSVar csRecvVar = csManager.getCSVar(recvCtx, recvVar);
                solver.getPointsToSetOf(csRecvVar).forEach(recvObj ->
                        addLambdaCallEdge(csCallSite, recvObj, targetRef,
                                indy, indyCtx));
                // New objects may reach csRecvVar later, thus we store it
                // together with information about the related Lambda invocation.
                invoInfos.put(csRecvVar,
                        new InstanceInvoInfo(csCallSite, indy, indyCtx));
            }
            case REF_invokeStatic -> // targetRef is static method
                    addLambdaCallEdge(csCallSite, null, targetRef, indy, indyCtx);
            default -> throw new AnalysisException(mh.getKind() + " is not supported");
        }
    }

    private static boolean isLambdaObj(Obj obj) {
        return obj instanceof MockObj mockObj &&
                mockObj.getDescription().equals(LAMBDA_DESC);
    }

    /**
     * @return the MethodHandle to the target method from invokedynamic.
     */
    private static MethodHandle getMethodHandle(InvokeDynamic indy) {
        return (MethodHandle) indy.getBootstrapArgs().get(1);
    }

    private void addLambdaCallEdge(
            CSCallSite csCallSite, @Nullable CSObj recvObj, MethodRef targetRef,
            InvokeDynamic indy, Context indyCtx) {
        JMethod callee;
        Context calleeCtx;
        if (recvObj != null) {
            // recvObj is not null, meaning that callee is instance method
            callee = hierarchy.dispatch(recvObj.getObject().getType(), targetRef);
            if (callee == null) {
                return;
            }
            calleeCtx = selector.selectContext(csCallSite, recvObj, callee);
            // pass receiver object to 'this' variable of callee
            solver.addVarPointsTo(calleeCtx, callee.getIR().getThis(), recvObj);
        } else { // otherwise, callee is static method
            callee = targetRef.resolveNullable();
            if (callee == null) {
                // this may happen in special cases, e.g.,
                // when the declaring class of targetRef is phantom class
                return;
            }
            calleeCtx = selector.selectContext(csCallSite, callee);
        }
        LambdaCallEdge callEdge = new LambdaCallEdge(csCallSite,
                csManager.getCSMethod(calleeCtx, callee), indy, indyCtx);
        solver.addCallEdge(callEdge);
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        if (edge instanceof LambdaCallEdge lambdaCallEdge) {
            CSCallSite csCallSite = lambdaCallEdge.getCallSite();
            CSMethod csCallee = lambdaCallEdge.getCallee();
            Context calleeContext = csCallee.getContext();
            List<Var> capturedArgs = lambdaCallEdge.getCapturedArgs();
            Context lambdaContext = lambdaCallEdge.getLambdaContext();

            JMethod target = csCallee.getMethod();
            // pass arguments captured at invokedynamic
            int shiftC; // shift of captured arguments
            if (capturedArgs.isEmpty()) {
                shiftC = 0;
            } else if (target.isStatic() || target.isConstructor()) {
                shiftC = 0;
            } else { // target is instance method and there is at least
                // one capture argument, then it must be the receiver object,
                // which has been passed to target's this variable when
                // adding call edge. Thus, we skip it and don't pass it
                // to target's parameters.
                shiftC = 1;
            }
            List<Var> targetParams = target.getIR().getParams();
            int j = 0;
            for (int i = shiftC; i < capturedArgs.size(); ++i, ++j) {
                solver.addPFGEdge(
                        csManager.getCSVar(lambdaContext, capturedArgs.get(i)),
                        csManager.getCSVar(calleeContext, targetParams.get(j)),
                        // filter spurious objects caused by imprecise lambda objects
                        PointerFlowEdge.Kind.PARAMETER_PASSING,
                        targetParams.get(j).getType());
            }
            // pass arguments from actual invocation site
            int shiftA; // shift of actual arguments
            if (capturedArgs.isEmpty() &&
                    !target.isStatic() && !target.isConstructor()) {
                // target is instance method and there is no any captured arguments,
                // then the first argument at actual invocation site must be
                // the receiver object, which has been passed to target's
                // this variable when adding call edge. So we can also skip it.
                shiftA = 1;
            } else {
                shiftA = 0;
            }
            Invoke invoke = csCallSite.getCallSite();
            List<Var> actualArgs = invoke.getInvokeExp().getArgs();
            Context callerContext = csCallSite.getContext();
            for (int i = shiftA; i < actualArgs.size(); ++i, ++j) {
                solver.addPFGEdge(
                        csManager.getCSVar(callerContext, actualArgs.get(i)),
                        csManager.getCSVar(calleeContext, targetParams.get(j)),
                        // filter spurious objects caused by imprecise lambda objects
                        PointerFlowEdge.Kind.PARAMETER_PASSING, targetParams.get(j).getType()
                );
            }
            // pass return value
            Var result = invoke.getResult();
            if (result != null) {
                CSVar csResult = csManager.getCSVar(callerContext, result);
                target.getIR().getReturnVars().forEach(ret -> {
                    CSVar csRet = csManager.getCSVar(calleeContext, ret);
                    solver.addPFGEdge(csRet, csResult,
                            // filter spurious objects caused by imprecise lambda objects
                            PointerFlowEdge.Kind.RETURN, result.getType());
                });
            }
        }
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        invoInfos.get(csVar).forEach(info -> {
            // handle the case of that new objects reach base variable
            // of lambda invocation
            InvokeDynamic indy = info.getLambdaIndy();
            MethodRef targetRef = getMethodHandle(indy).getMethodRef();
            pts.forEach(recvObj ->
                    addLambdaCallEdge(info.getCSCallSite(), recvObj,
                            targetRef, indy, info.getLambdaContext()));
        });
    }
}
