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

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.PointerAnalysis;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.MethodHandle;
import pascal.taie.ir.exp.NewInstance;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.Type;
import soot.Hierarchy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.util.collection.CollectionUtils.addToMapSet;
import static pascal.taie.util.collection.CollectionUtils.newMap;

public class LambdaPlugin implements Plugin {

    private PointerAnalysis pta;

    private ContextSelector selector;

    private ClassHierarchy hierarchy;

    private HeapModel heapModel;

    /**
     * Map from method to the lambda functional objects created in the method.
     */
    private final Map<JMethod, Set<LambdaObj>> lambdaObjs = newMap();

    /**
     * Map from lambda functional objects to the actual params when the objects are invoked..
     */
    private final Map<LambdaObj, Set<List<Var>>> invokeParams = newMap();

    @Override
    public void setPointerAnalysis(PointerAnalysis pta) {
        this.pta = pta;
        this.selector = pta.getContextSelector();
        this.hierarchy = pta.getHierarchy();
        this.heapModel = pta.getHeapModel();
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

    private static boolean isLambdaMetaFactory(InvokeDynamic indy) {
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
                Context heapContext = selector.selectHeapContext(
                        csMethod, lambdaObj);
                pta.addVarPointsTo(context, ret, heapContext, lambdaObj);
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
                    ((MethodHandle)lambdaObj.getAllocation().getBootstrapArgs().get(1))
                            .getMethodRef().resolve();
            // shift flags for passing parameters
            int shiftFlagK = 0;
            int shiftFlagN = 0;

            if (!implMethod.isStatic()) {
                Type receiverType = implMethod.getRef().getDeclaringClass().getType();
                implMethod = hierarchy.dispatch(receiverType, implMethod.getRef());
                shiftFlagK = implMethod.getParamCount() == 0 ? 0 : 1;
                shiftFlagN = 1 - shiftFlagK;
            }
            // TODO 在这个方法里params和returnValue都获取到的是Var，怎么在这个方法里addVarPointsTo到Obj诶
            //  如果都要在handleNewPointsToSet里面point，那除了lambda-ActualParam的map，还需要维护实际调用的return值吗

            // pass return values
            if (invokeResult != null) {
                // special: constructor, mock result
                if (implMethod.isConstructor()) {
                    NewInstance constructedInstance = new NewInstance(implMethod.getDeclaringClass().getType());
                    Obj constructedObj = heapModel.getObj(constructedInstance);
                    pta.addVarPointsTo(context, invokeResult, context, constructedObj);
                } else {
//                    implMethod.getIR().getReturnVars()
//                            .forEach(r -> pta.addVarPointsTo(context, invokeResult, recv.getContext(), r));
                }
            }
        }
    }

    @Override
    public void handleNewPointsToSet(CSVar csVar, PointsToSet pts) {

    }
}
