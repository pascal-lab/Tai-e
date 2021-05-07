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
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.solver.PointerAnalysis;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.MethodHandle;
import pascal.taie.ir.exp.MethodType;
import pascal.taie.ir.exp.NewInstance;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.CollectionUtils;
import soot.jimple.IntConstant;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class LambdasPlugin implements Plugin {

    private PointerAnalysis pta;

    private ClassHierarchy hierarchy;

    private HeapModel heapModel;

    private Map<ClassType, NewInstance> newInstanceMap;

    private List<LambdasInfo> lambdasInfos = new ArrayList<>();

    private LambdasInfo lambdasInfo;

    private List<LambdasInvoke> lambdasInvokes = new ArrayList<>();

    private LambdasInvoke lambdasInvoke;

    private List<Var> params;

    @Override
    public void setPointerAnalysis(PointerAnalysis pta) {
        this.pta = pta;
        hierarchy = pta.getHierarchy();
        heapModel = pta.getHeapModel();
    }

    @Override
    public void handleNewMethod(JMethod method) {
        extractInvokeDynamics(method.getIR()).forEach(invoke -> {
            lambdasInfo = new LambdasInfo();
            lambdasInfo.setIndyObject(invoke.getResult());
            InvokeDynamic indy = (InvokeDynamic) invoke.getInvokeExp();
            Stmt indyCallsiteMethod = indy.getCallSite().getStmt();
            System.out.println("indy: " + indy.toString());
            System.out.println("indyCallsiteMethod: " + indyCallsiteMethod);

            String bsmName = indy.getBootstrapMethodRef().getName();
            if ("metafactory".equals(bsmName) || "altMetafactory".equals(bsmName)) {
                System.out.println("indy MethodName & MethodType: " + indy.getMethodName() + "   " + indy.getMethodType());
                MethodRef implMethodRef = mockLambdaObject(indy);
                lambdasInfo.setCapturedValues(indy.getArgs());

                try {
                    methodInvocation(implMethodRef);
                } catch (NoSuchMethodException | InstantiationException
                        | InvocationTargetException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });
        //获取实际调用点呢

        extractInvoke(method.getIR()).forEach(invoke -> {
            // TODO 匹配到对应的lambdasInfo
            if (invoke.getMethodRef().getName().equals("apply")) {
                System.out.println("hello apply");
                System.out.println("invokeExp: " + invoke.getInvokeExp().toString());
                System.out.println("methodRef: " + invoke.getMethodRef());
                System.out.println("methodRef declaring class: " + invoke.getMethodRef().getDeclaringClass());//binaryoperator
                System.out.println("result: " + invoke.getResult());
                System.out.println("apply end\n");

                lambdasInvoke = new LambdasInvoke();
                lambdasInvoke.setResult(invoke.getResult());
                lambdasInvoke.setActualParams(invoke.getInvokeExp().getArgs());
                //lambdasInvoke.setLambdasIndex(0);

                lambdasInvokes.add(lambdasInvoke);

            }
        });

    }

    @Override
    public void handleNewCSMethod(CSMethod csMethod) {
//        System.out.println("hello handleNewCSMethod");
//        System.out.println("context: " + csMethod.getContext().toString());
        Context context = csMethod.getContext();
        JMethod method = csMethod.getMethod();
        if (!CollectionUtils.isEmpty(lambdasInfos)){
            lambdasInfos.stream()
                    .filter(l -> l.getIndyObject() != null)
                    .filter(l -> method.equals(l.getIndyObject().getMethod()))
                    .forEach(l -> l.setIndyContext(context));
            lambdasInfos.stream()
                    .filter(l -> l.getImplMethodThis() !=null)
                    .filter(l -> method.equals(l.getImplMethodThis().getMethod()))
                    .forEach(l -> l.setImplMethodContext(context));
        }
        if (!CollectionUtils.isEmpty(lambdasInvokes)) {
            lambdasInvokes.stream()
                    .filter(i -> i.getResult() != null)
                    .filter(i -> method.equals(i.getResult().getMethod()))
                    .forEach(i -> i.setInvokeContext(context));
        }
    }

    // mock出来的Lambdas对象
    private MethodRef mockLambdaObject(InvokeDynamic indy) {
        System.out.println("hello mockLambdaObject");
        String methodName = indy.getMethodName();
        MethodType type = indy.getMethodType();

        List<Literal> bootstrapArgs = indy.getBootstrapArgs();
        if (bootstrapArgs.size() < 3
                || !(bootstrapArgs.get(0) instanceof MethodType)
                || !(bootstrapArgs.get(1) instanceof MethodHandle)
                || !(bootstrapArgs.get(2) instanceof MethodType)
                || (bootstrapArgs.size() > 3 && !(bootstrapArgs.get(3) instanceof IntConstant))) {
            // handle unexpected arguments
        }

        MethodType samMethodType = ((MethodType) indy.getBootstrapArgs().get(0));
        MethodHandle implMethod = ((MethodHandle) bootstrapArgs.get(1));
        MethodType instantiatedMethodType = ((MethodType) indy.getBootstrapArgs().get(2));

        //  TODO  mock lambdas object
//        lambdasInfo.setMockedLambdasObject(
//                new Var(implMethod.getMethodRef().resolve(), methodName, type.getType()));


        System.out.println("samMethodType :" + samMethodType.toString());
        System.out.println("implMethod :" + implMethod.toString());
        System.out.println("instantiatedMethodType :" + instantiatedMethodType.toString());

        return implMethod.getMethodRef();
    }

    private void methodInvocation(MethodRef methodRef)
            throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException {
        System.out.println("hello methodInvocation");
        JMethod implMethod = methodRef.resolve();
        lambdasInfo.setImplParamCount(implMethod.getParamCount()); //#i
        // handle constructor
        if (implMethod.isConstructor()) {
            lambdasInfo.setConstructor(true);
            return;
        }
        Type receiverType = implMethod.getRef().getDeclaringClass().getType();
        System.out.println("receiverType: " + receiverType.toString());
        System.out.println(implMethod.toString());

        if (!methodRef.isStatic()) {
            lambdasInfo.setStatic(false);
            implMethod = hierarchy.dispatch(receiverType, methodRef);
            //参数位移shift
            lambdasInfo.setShiftFlagK(lambdasInfo.getImplParamCount() == 0 ? 0 : 1);
            lambdasInfo.setShiftFlagN(1 - lambdasInfo.getShiftFlagK());
        }
        lambdasInfo.setReturnValues(implMethod.getIR().getReturnVars());
        System.out.println("implreturnvalues: " + lambdasInfo.getReturnValues());
        lambdasInfo.setImplMethodParams(implMethod.getIR().getParams());
        lambdasInfo.setImplMethodDeclaringClass(implMethod.getDeclaringClass());
        lambdasInfo.setImplMethodThis(implMethod.getIR().getThis());

        lambdasInfos.add(lambdasInfo);
    }

    @Override
    public void handleNewPointsToSet(CSVar csVar, PointsToSet pts) {
//        System.out.println("hello handleNewPointsToSet");
//        System.out.println("csVar" + csVar.toString());
//        System.out.println("pts: " + pts.toString());
        Var var = csVar.getVar();

//        NewInstance constructedInstance = new NewInstance(implClass.getType());
//        lambdasInfo.setConstructedObj(heapModel.getObj(constructedInstance));
         // TODO constructor
        int varInReturn = varInImplReturnValues(var);
//        System.out.println("varinreturn: " + varInReturn);
        if (varInReturn!=-1){
            lambdasInvokes.stream()
                    .filter(i -> i.getLambdasIndex() == varInReturn)
                    .forEach(i -> {
                        System.out.println("lambdasinvoke: " + i.getInvokeContext() + " " + i.getResult() + ' ' + i.getActualParams());
                        pta.addVarPointsTo(i.getInvokeContext(), i.getResult(), pts);
                    });
        }

        // TODO params
    }

    private int varInImplReturnValues(Var var){
        for (int i = 0; i < lambdasInfos.size() ; i++) {
            if (lambdasInfos.get(i).getReturnValues().contains(var)){
                return i;
            }
        }
        return -1;
    }

    private static Stream<Invoke> extractInvokeDynamics(IR ir) {
        return ir.getStmts()
                .stream()
                .filter(s -> s instanceof Invoke)
                .map(s -> (Invoke) s)
                .filter(s -> s.getInvokeExp() instanceof InvokeDynamic);
    }

    private static Stream<Invoke> extractInvoke(IR ir) {
        return ir.getStmts()
                .stream()
                .filter(s -> s instanceof Invoke)
                .map(s -> (Invoke) s)
                .filter(s -> !(s.getInvokeExp() instanceof InvokeDynamic));
    }

    private static class LambdasInfo {

        private boolean isStatic = true;

        private boolean isConstructor = false;

        private JClass implMethodDeclaringClass;

        private List<Var> capturedValues;

        private Var indyObject;

        private Var mockedLambdasObject;

        private Context indyContext;

        private List<Var> returnValues;

        private List<Var> implMethodParams;

        private Var implMethodThis;

        private Context implMethodContext;

        private int shiftFlagK = 0;

        private int shiftFlagN = 0;

        private int implParamCount;

        public boolean isStatic() {
            return isStatic;
        }

        public void setStatic(boolean aStatic) {
            isStatic = aStatic;
        }

        public boolean isConstructor() {
            return isConstructor;
        }

        public void setConstructor(boolean aConstructor) {
            isConstructor = aConstructor;
        }

        public List<Var> getCapturedValues() {
            return capturedValues;
        }

        public void setCapturedValues(List<Var> capturedValues) {
            this.capturedValues = capturedValues;
        }

        public Var getIndyObject() {
            return indyObject;
        }

        public void setIndyObject(Var indyObject) {
            this.indyObject = indyObject;
        }

        public Context getIndyContext() {
            return indyContext;
        }

        public void setIndyContext(Context indyContext) {
            this.indyContext = indyContext;
        }

        public List<Var> getReturnValues() {
            return returnValues;
        }

        public void setReturnValues(List<Var> returnValues) {
            this.returnValues = returnValues;
        }

        public List<Var> getImplMethodParams() {
            return implMethodParams;
        }

        public void setImplMethodParams(List<Var> implMethodParams) {
            this.implMethodParams = implMethodParams;
        }

        public Var getImplMethodThis() {
            return implMethodThis;
        }

        public void setImplMethodThis(Var implMethodThis) {
            this.implMethodThis = implMethodThis;
        }

        public Context getImplMethodContext() {
            return implMethodContext;
        }

        public void setImplMethodContext(Context implMethodContext) {
            this.implMethodContext = implMethodContext;
        }

        public int getShiftFlagK() {
            return shiftFlagK;
        }

        public void setShiftFlagK(int shiftFlagK) {
            this.shiftFlagK = shiftFlagK;
        }

        public int getShiftFlagN() {
            return shiftFlagN;
        }

        public void setShiftFlagN(int shiftFlagN) {
            this.shiftFlagN = shiftFlagN;
        }

        public int getImplParamCount() {
            return implParamCount;
        }

        public void setImplParamCount(int implParamCount) {
            this.implParamCount = implParamCount;
        }

        public JClass getImplMethodDeclaringClass() {
            return implMethodDeclaringClass;
        }

        public void setImplMethodDeclaringClass(JClass implMethodDeclaringClass) {
            this.implMethodDeclaringClass = implMethodDeclaringClass;
        }

        public Var getMockedLambdasObject() {
            return mockedLambdasObject;
        }

        public void setMockedLambdasObject(Var mockedLambdasObject) {
            this.mockedLambdasObject = mockedLambdasObject;
        }
    }

    private static class LambdasInvoke {

        private List<Var> actualParams;

        private Var result;

        private int LambdasIndex;

        private Context invokeContext;

        public List<Var> getActualParams() {
            return actualParams;
        }

        public void setActualParams(List<Var> actualParams) {
            this.actualParams = actualParams;
        }

        public Var getResult() {
            return result;
        }

        public void setResult(Var result) {
            this.result = result;
        }

        public int getLambdasIndex() {
            return LambdasIndex;
        }

        public void setLambdasIndex(int lambdasIndex) {
            LambdasIndex = lambdasIndex;
        }

        public Context getInvokeContext() {
            return invokeContext;
        }

        public void setInvokeContext(Context invokeContext) {
            this.invokeContext = invokeContext;
        }
    }
}
