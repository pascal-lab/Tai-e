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
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.solver.PointerAnalysis;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.Type;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.util.collection.CollectionUtils.addToMapSet;
import static pascal.taie.util.collection.CollectionUtils.newMap;

public class LambdaPlugin implements Plugin {

    private PointerAnalysis pta;

    private ContextSelector selector;

    /**
     * Map from method to the lambda functional objects created in the method.
     */
    private final Map<JMethod, Set<LambdaObj>> lambdaObjs = newMap();

    @Override
    public void setPointerAnalysis(PointerAnalysis pta) {
        this.pta = pta;
        this.selector = pta.getContextSelector();
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
        // TODO: model invocation
    }
}
