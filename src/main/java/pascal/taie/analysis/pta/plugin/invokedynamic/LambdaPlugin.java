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
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.MethodHandle;
import pascal.taie.ir.exp.NewInstance;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.util.collection.CollectionUtils.addToMapSet;
import static pascal.taie.util.collection.CollectionUtils.newMap;

public class LambdaPlugin implements Plugin {

    private Solver solver;

    private ContextSelector selector;

    private ClassHierarchy hierarchy;

    private HeapModel heapModel;

    private CSManager csManager;

    /**
     * Map from method to the lambda functional objects created in the method.
     */
    private final Map<JMethod, Set<LambdaObj>> lambdaObjs = newMap();

    /**
     * Map from lambda functional objects to the actual params when the objects are invoked..
     */
    private final Map<LambdaObj, Set<List<Var>>> invokeParams = newMap();

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        this.selector = solver.getContextSelector();
        this.hierarchy = solver.getHierarchy();
        this.heapModel = solver.getHeapModel();
        this.csManager = solver.getCSManager();
    }

    @Override
    public void handleNewMethod(JMethod method) {
        extractLambdaMetaFactories(method.getIR()).forEach(indy -> {
            Type type = indy.getMethodType().getReturnType();
            JMethod container = indy.getCallSite().getMethod();
            // record lambda meta factories of new discovered methods
            addToMapSet(lambdaObjs, container, new LambdaObj(type, indy, container));
            System.out.println(lambdaObjs.values());
        });
    }

    private static Stream<InvokeDynamic> extractLambdaMetaFactories(IR ir) {
        return ir.getStmts()
                .stream()
                .filter(s -> s instanceof Invoke &&
                        ((Invoke) s).getInvokeExp() instanceof InvokeDynamic)
                .map(s -> (InvokeDynamic) ((Invoke) s).getInvokeExp())
                .filter(LambdaPlugin::isLambdaMetaFactory);
    }

    static boolean isLambdaMetaFactory(InvokeDynamic indy) {
        JMethod bsm = indy.getBootstrapMethodRef().resolve();
        String bsmSig = bsm.getSignature();
        return bsmSig.equals(StringReps.LAMBDA_METAFACTORY) ||
                bsmSig.equals(StringReps.LAMBDA_ALTMETAFACTORY);
    }

    @Override
    public void handleNewCSMethod(CSMethod csMethod) {
        JMethod method = csMethod.getMethod();
        Set<LambdaObj> lambdas = lambdaObjs.get(method);
        if (lambdas != null) {
            Context context = csMethod.getContext();
            lambdas.forEach(lambdaObj -> {
                // propagate lambda functional objects
                InvokeDynamic indy = lambdaObj.getAllocation();
                Invoke invoke = (Invoke) indy.getCallSite().getStmt();
                Var ret = invoke.getResult();
                // here we use full method context as the heap context of
                // lambda object, so that it can be directly used to obtain
                // captured values.
                solver.addVarPointsTo(context, ret, context, lambdaObj);
            });
        }
    }

    @Override
    public void handleUnresolvedCall(CSObj recv, Context context, Invoke invoke) {
        if (recv.getObject() instanceof LambdaObj) {
            LambdaObj lambdaObj = (LambdaObj) recv.getObject();
            List<Var> actualParams = invoke.getInvokeExp().getArgs();
            Var invokeResult = invoke.getResult();
            addToMapSet(invokeParams, lambdaObj, actualParams);

            JMethod implMethod =
                    ((MethodHandle) lambdaObj.getAllocation().getBootstrapArgs().get(1))
                            .getMethodRef().resolve();
            if (invokeResult != null) {
                // special: constructor, mock result
                if (implMethod.isConstructor()) {
                    ClassType type = implMethod.getDeclaringClass().getType();
                    NewInstance constructedInstance = new NewInstance(type);
                    Obj constructedObj = heapModel.getObj(constructedInstance);
                    solver.addVarPointsTo(context, invokeResult, context, constructedObj);
                    // pta.addVarPointsTo(context, implMethod.getIR().getThis(), context, constructedObj);
                    // TODO here is no implMethod context, implMethod/This -> constructedObj needs to be handled in other methods?
                }
            }

            LambdaCallEdge callEdge = new LambdaCallEdge(
                    csManager.getCSCallSite(context, invoke.getInvokeExp()),
                    csManager.getCSMethod(recv.getContext(), implMethod));
            callEdge.setLambdaParams(
                    invoke.getResult(),
                    lambdaObj.getAllocation().getArgs(),
                    recv.getContext());
            solver.addCallEdge(callEdge);
        }
    }

    @Override
    public void handleNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        if (edge instanceof LambdaCallEdge) {
            LambdaCallEdge lambdaCallEdge = (LambdaCallEdge) edge;
            CSCallSite csCallSite = lambdaCallEdge.getCallSite();
            CSMethod csMethod = lambdaCallEdge.getCallee();

            Var invokeResult = lambdaCallEdge.getInvokeResult();
            List<Var> actualParams = csCallSite.getCallSite().getArgs();
            JMethod implMethod = csMethod.getMethod();
            List<Var> capturedValues = lambdaCallEdge.getCapturedValues();

            Context callerContext = csCallSite.getContext();
            Context implMethodContext = csMethod.getContext();
            Context lambdaContext = lambdaCallEdge.getLambdaContext();

            // shift flags for passing parameters
            int shiftFlagK = 0;
            int shiftFlagN = 0;
            int paramCount = implMethod.getParamCount();
            List<Var> implParams = implMethod.getIR().getParams();

            if (!implMethod.isStatic()) {
                shiftFlagK = paramCount == 0 ? 0 : 1;
                shiftFlagN = 1 - shiftFlagK;
            }

            // pass parameters: from actual parameters & from captured values
            if (paramCount != 0) {
                for (int i = 0; i + shiftFlagK <= paramCount
                                    && i < capturedValues.size()
                                    && i - shiftFlagK >= 0; i++) {
                    solver.addPFGEdge(
                            csManager.getCSVar(lambdaContext, capturedValues.get(i)),
                            csManager.getCSVar(implMethodContext, implParams.get(i - shiftFlagK)),
                            PointerFlowEdge.Kind.PARAMETER_PASSING);
                }

                for (int i = 0; i < actualParams.size(); i++) {
                    int index = paramCount - (shiftFlagK + shiftFlagN) + i;
                    if (index >= paramCount) {
                        break;
                    }
                    solver.addPFGEdge(
                            csManager.getCSVar(callerContext, actualParams.get(i)),
                            csManager.getCSVar(implMethodContext, implParams.get(index)),
                            PointerFlowEdge.Kind.PARAMETER_PASSING);
                }
            }

            // pass return values
            List<Var> returnValues = implMethod.getIR().getReturnVars();
            if (invokeResult != null && !CollectionUtils.isEmpty(returnValues)) {
                CSVar csInvokeResult = csManager.getCSVar(callerContext, invokeResult);
                returnValues.stream()
                        .map(r -> csManager.getCSVar(implMethodContext, r))
                        .forEach(r ->
                                solver.addPFGEdge(r, csInvokeResult, PointerFlowEdge.Kind.RETURN));
            }

            if (shiftFlagK == 1
                    && !implMethod.isStatic()
                    && !CollectionUtils.isEmpty(capturedValues)) {
                solver.addPFGEdge(
                        csManager.getCSVar(lambdaContext, capturedValues.get(0)),
                        csManager.getCSVar(implMethodContext, implMethod.getIR().getThis()),
                        PointerFlowEdge.Kind.LOCAL_ASSIGN);
            }

            if (shiftFlagN == 1 && !CollectionUtils.isEmpty(actualParams)) {
                solver.addPFGEdge(
                        csManager.getCSVar(callerContext, actualParams.get(0)),
                        csManager.getCSVar(implMethodContext, implMethod.getIR().getThis()),
                        PointerFlowEdge.Kind.LOCAL_ASSIGN);
            }
        }
    }
}
