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
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.plugin.reflection.ReflectionUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.MethodType;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.MapUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class InvokedynamicPlugin implements Plugin {

    /**
     * Lambdas are supposed to be processed by LambdaPlugin.
     */
    private static final boolean processLambdas = false;
    // TODO add log warning when there is Lambdas while LambdaPlugin not in use

    private Solver solver;

    private MethodTypeModel methodTypeModel;

    private CSManager csManager;

    private ContextSelector selector;

    private HeapModel heapModel;

    private ClassHierarchy hierarchy;

    /**
     * Map from method to the invokedynamic created in the method.
     */
    private final Map<JMethod, Set<Invoke>> indyPoints = MapUtils.newMap();

    /**
     * Map from method find class variable to the indyCallEdgeInfos
     */
    private final Map<Var, Set<IndyCallEdgeInfo>> indyCallEdgeInfos = MapUtils.newMap();

    public static final String LOOKUP_DESC = "MethodLookupObj";

    /**
     * Map from class type to corresponding Method.Lookup object.
     */
    private final Map<ClassType, MockObj> lookupObjs = MapUtils.newMap();

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        this.methodTypeModel = new MethodTypeModel(solver);
        this.csManager = solver.getCSManager();
        this.selector = solver.getContextSelector();
        this.heapModel = solver.getHeapModel();
        this.hierarchy = solver.getHierarchy();
    }

    @Override
    public void onNewMethod(JMethod method) {
        method.getIR().getStmts().forEach(stmt -> {
            if (stmt instanceof Invoke) {
                Invoke invoke = (Invoke) stmt;
                methodTypeModel.handleNewInvoke(invoke);
            }
        });
        extractInvokeDynamics(method.getIR()).forEach(invoke -> {
            JMethod container = invoke.getContainer();
            MapUtils.addToMapSet(indyPoints, container, invoke);
            System.out.println(indyPoints.values());
        });
    }

    private static Stream<Invoke> extractInvokeDynamics(IR ir) {
        return ir.getStmts()
                .stream()
                .filter(s -> s instanceof Invoke)
                .map(s -> (Invoke) s)
                .filter(invoke -> invoke.getInvokeExp() instanceof InvokeDynamic)
                .filter(invoke -> processLambdas ||
                        !LambdaPlugin.isLambdaMetaFactory(invoke));
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        JMethod method = csMethod.getMethod();
        Set<Invoke> invokes = indyPoints.get(method);
        if (invokes != null) {
            Context context = csMethod.getContext();
            invokes.forEach(invoke -> {
                InvokeDynamic indy = (InvokeDynamic) invoke.getInvokeExp();
                Var invokeResult = invoke.getResult();
                System.out.println("args: " + indy.getArgs());
                System.out.println("invoke result = " + invokeResult);
                // TODO pass result

                JMethod bsm = indy.getBootstrapMethodRef().resolve();
                System.out.println(bsm.toString());
                // TODO bsm context should be invokedynamic call site
                Context bsmContext = selector.getDefaultContext();

//                System.out.println(invoke.toString());
//                System.out.println(indy.getMethodType());

                // pass parameters to bootstrap method
                List<Var> bsmParams = bsm.getIR().getParams();

                ClassType callerType = invoke.getContainer().getDeclaringClass().getType();
                MockObj lookup = lookupObjs.computeIfAbsent(callerType, type -> {
                    Type lookupType = hierarchy.getJREClass(StringReps.LOOKUP).getType();
                    return new MockObj(LOOKUP_DESC, type, lookupType);
                });
                solver.addVarPointsTo(bsmContext, bsmParams.get(0), context, lookup);
                solver.addVarPointsTo(bsmContext, bsmParams.get(1), context,
                        heapModel.getConstantObj(StringLiteral.get(indy.getMethodName())));
                solver.addVarPointsTo(bsmContext, bsmParams.get(2), context,
                        heapModel.getConstantObj(indy.getMethodType()));

                if (bsm.getParamCount() > 3) {
                    List<Literal> args = indy.getBootstrapArgs();
                    if (args.size() <= 3) {
                        for (int i = 0; i < args.size() && i + 3 < bsm.getParamCount() && i < 3; i++) {
                            // TODO Literal类型的处理
//                        solver.addVarPointsTo(bsmContext, bsmParams.get(i +3), context,
//                                heapModel.getObj());
                        }
                    } else {
                        // TODO mock array for varargs
                    }
                }

                // add indy -> bsm call edge
                solver.addCallEdge(new BSMCallEdge(
                        csManager.getCSCallSite(context, invoke),
                        csManager.getCSMethod(bsmContext, bsm),
                        method.getDeclaringClass()));
            });
        }
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        if (edge instanceof BSMCallEdge) {
            // 看看这里面能不能有足够信息获取到impl
            BSMCallEdge bsmCallEdge = (BSMCallEdge) edge;
            CSCallSite csCallSite = bsmCallEdge.getCallSite();
            CSMethod csMethod = bsmCallEdge.getCallee();
            JClass lookupClass = bsmCallEdge.getLookupClass();

            List<Var> actualParams = csCallSite.getCallSite()
                    .getInvokeExp().getArgs();
            JMethod bsm = csMethod.getMethod();
            Context bsmContext = csMethod.getContext();

            bsm.getIR().getStmts().stream()
                    .filter(stmt -> stmt instanceof Invoke)
                    .map(stmt -> (Invoke) stmt)
                    .filter(invoke -> isFindMethod(
                            invoke.getMethodRef().resolve().getSignature()) != 0)
                    .forEach(invoke ->
                            MapUtils.addToMapSet(indyCallEdgeInfos,
                                    invoke.getMethodRef().resolve().getIR().getParams().get(0),
                                    new IndyCallEdgeInfo(
                                            isFindMethod(invoke.getMethodRef().resolve().getSignature()),
                                            csCallSite)));
        }

        if (edge instanceof InvokedynamicCallEdge) {

        }
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        Var var = csVar.getVar();
        if (methodTypeModel.isRelevantVar(var)) {
            methodTypeModel.handleNewPointsToSet(csVar, pts);
        }
        Set<IndyCallEdgeInfo> callEdgeInfos = indyCallEdgeInfos.get(var);
        if (callEdgeInfos != null) {
            System.out.println("findargs: " + csVar.getVar() + ": " + csVar.getPointsToSet());
            callEdgeInfos.stream()
                    .filter(info -> !info.hadFindMethod())
                    .forEach(info -> {
                        CSCallSite csCallSite = info.getIndyCallSite();
                        InvokeDynamic indy = (InvokeDynamic) csCallSite
                                .getCallSite().getInvokeExp();

                        int findType = info.getFindType();
                        String methodName = indy.getMethodName();
                        MethodType mt = indy.getMethodType();

                        pts.forEach(csObj -> {
                            Object obj = csObj.getObject().getAllocation();
                            String objString = obj.toString();
                            if (objString.contains(".class")) {
                                JClass jClass = hierarchy.getClass(
                                        objString.substring(0, objString.length() - 6));
                                // constructors
                                if (findType == 4) {
                                    ReflectionUtils.getConstructors(jClass)
                                            .filter(m -> m.getParamTypes().equals(mt.getParamTypes()))
                                            .forEach(m -> {
                                                System.out.println("find method!: " + m);
                                                solver.addCallEdge(new InvokedynamicCallEdge(
                                                        csCallSite, csManager.getCSMethod(selector.getDefaultContext(), m)));
                                                info.setFindMethod(true);
                                            });
                                } else {
                                    ReflectionUtils.getMethods(jClass, methodName)
                                            // TODO asType
                                            .filter(m -> m.getParamTypes().equals(mt.getParamTypes())
                                                    && m.getReturnType().equals(mt.getReturnType()))
                                            .forEach(m -> {
                                                System.out.println("find method!: " + m);
                                                solver.addCallEdge(new InvokedynamicCallEdge(
                                                        csCallSite, csManager.getCSMethod(selector.getDefaultContext(), m)));
                                                info.setFindMethod(true);
                                            });
                                }
                            }
                        });
                    });
        }
    }

    private int isFindMethod(String signature) {
        switch (signature) {
            case StringReps.INVOKEDYNAMIC_FINDSTATIC:
                return 1;
            case StringReps.INVOKEDYNAMIC_FINDVIRTUAL:
                return 2;
            case StringReps.INVOKEDYNAMIC_FINDSPECIAL:
                return 3;
            case StringReps.INVOKEDYNAMIC_FINDCONSTRUCTOR:
                return 4;
            default:
                return 0;
        }
    }

    private static class IndyCallEdgeInfo {
        // type: InvokedynamicPlugin.isFindMethod
        private int findType;

        private CSCallSite indyCallSite;

        private boolean findMethod = false;

        public IndyCallEdgeInfo(int findType, CSCallSite indyCallSite) {
            this.findType = findType;
            this.indyCallSite = indyCallSite;
        }

        public int getFindType() {
            return findType;
        }

        public void setFindType(int findType) {
            this.findType = findType;
        }

        public CSCallSite getIndyCallSite() {
            return indyCallSite;
        }

        public void setIndyCallSite(CSCallSite indyCallSite) {
            this.indyCallSite = indyCallSite;
        }

        public boolean hadFindMethod() {
            return findMethod;
        }

        public void setFindMethod(boolean findMethod) {
            this.findMethod = findMethod;
        }
    }
}
