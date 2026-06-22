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

package pascal.taie.analysis.pta.plugin.spring.di;

import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.graph.flowgraph.FlowKind;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.element.InstanceField;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.cs.element.PointerDescriptor;
import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.plugin.spring.SpringPluginConfig;
import pascal.taie.analysis.pta.plugin.spring.util.AnnotationManager;
import pascal.taie.analysis.pta.plugin.spring.util.AnnotationUtils;
import pascal.taie.analysis.pta.plugin.spring.util.SpringBeanNames;
import pascal.taie.analysis.pta.plugin.spring.util.XmlConfiguration;
import pascal.taie.analysis.pta.plugin.util.SolverHolder;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.InvokeVirtual;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class DiAnalysis extends SolverHolder implements Plugin {

    private static final Descriptor DI_OBJ_DESC = () -> "DiObject";

    private static final PointerDescriptor SINGLETON_PTR_DESC = () -> "SingletonPointer";

    private final BeanDefinitionCollector collector;

    /**
     * Stores all bean objects created for all bean definitions.
     */
    private final Set<CSObj> beanObjs = Sets.newSet();

    /**
     * Since factory beans must be created during analysis, we leverage the pointer analysis
     * listener mechanism instead of writing extra callbacks.
     * For each bean definition, a pointer is created to hold its singleton bean instance.
     * We use a pointer rather than the bean object directly because the bean object may not
     * yet exist when it is needed (e.g., a factory bean object requires the pointer analysis
     * to fully analyze the factory method before the object is ready).
     * Through PFG edges on these pointers, once a bean object is created it is automatically
     * propagated to any target pointer that requires it.
     */
    private final Map<BeanDefinition, Pointer> bean2SingletonPointer = Maps.newMap();

    /**
     * Tracks the return-value pointers of all bean factory methods.
     * These pointers are monitored to detect the creation of factory bean objects.
     * Factory bean objects are created automatically by the pointer analysis solver;
     * once created, they need to be initialized.
     */
    private final Set<CSVar> returnVarsOfBeanMethods = Sets.newSet();

    /**
     * A synthetic Spring class containing a main method that serves as the
     * container method for simulating Spring's bean method invocations.
     */
    private final JMethod springMain;

    private final List<Consumer<CSObj>> newBeanObjListeners = new ArrayList<>();

    public DiAnalysis(Solver solver, AnnotationManager annotationManager, XmlConfiguration xmlConfiguration) {
        super(solver);
        springMain = SyntheticSpringMain.getOrCreate(hierarchy, typeSystem);
        collector = new BeanDefinitionCollector(hierarchy, annotationManager, xmlConfiguration);
        collector.work();

        this.registerNewBeanObjListener(this::initializeBeanObj);
    }

    @Override
    public void onStart() {
        solver.addCSMethod(csManager.getCSMethod(emptyContext, springMain));
        collector.getBeanDefinitions().forEach(bean -> {
            // Every bean definition, including prototype beans, is currently modeled
            // by one singleton-style pointer as a deliberate approximation.
            Pointer singletonPointer = csManager.getMockPointer(
                    SINGLETON_PTR_DESC, bean, bean.getJClass().getType());
            bean2SingletonPointer.put(bean, singletonPointer);
        });
        // Create bean objects and trigger callbacks to initialize them and start WEC analysis.
        // This step is separated from the one above because bean initialization requires the
        // singleton pointers (created above) to resolve parameters and fields.
        bean2SingletonPointer.forEach((bean, singletonPointer) -> {
            // For singleton common beans, create a corresponding bean object and add it to
            // the placeholder pointer. Factory beans depend on the invocation of another
            // bean's factory method, so they are created during bean initialization.
            if (bean instanceof CommonBeanDefinition) {
                CSObj beanObj = newBeanObj(bean);
                solver.addPointsTo(singletonPointer, beanObj);
            }
        });
    }

    @Override
    public void onFinish() {
        collector.processResult(solver.getOptions());
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        if (returnVarsOfBeanMethods.contains(csVar)) {
            pts.forEach(this::onNewBeanObj);
        }
    }

    /**
     * Registers a listener that is called when a new bean object is created.
     */
    public void registerNewBeanObjListener(Consumer<CSObj> newBeanObjListener) {
        this.newBeanObjListeners.add(newBeanObjListener);
    }

    private void onNewBeanObj(CSObj beanObj) {
        if (beanObjs.add(beanObj)) {
            newBeanObjListeners.forEach(listener -> listener.accept(beanObj));
        }
    }

    /**
     * Creates a mock bean object for the given bean definition.
     * Applicable to non-factory bean definitions.
     */
    private CSObj newBeanObj(BeanDefinition bean) {
        CSObj beanObj = csManager.getCSObj(emptyContext,
                heapModel.getMockObj(DI_OBJ_DESC, bean, bean.getJClass().getType()));
        onNewBeanObj(beanObj);
        return beanObj;
    }

    /**
     * Simulates Spring's bean object initialization, including constructor invocation,
     * {@code @Injected} method invocation, {@code @Injected} field injection, and bean
     * factory method invocation.
     */
    private void initializeBeanObj(CSObj beanObj) {
        // For common beans, invoke the constructor and call factory methods to create factory bean objects.
        if (beanObj.getObject() instanceof MockObj
                && beanObj.getObject().getAllocation() instanceof CommonBeanDefinition commonBeanDefinition) {
            // Invoke the constructor
            JMethod constructor = commonBeanDefinition.getConstructor();
            callMethod(beanObj, constructor);
            // Invoke factory methods
            callFactoryMethods(beanObj);
        }

        JClass jClass = ((ClassType) beanObj.getObject().getType()).getJClass();
        collector.getInjectedMethods(jClass).forEach(injectedMethod -> callMethod(beanObj, injectedMethod));

        collector.getInjectedFields(jClass)
                .forEach(jField -> {
                    InstanceField instanceField = csManager.getInstanceField(beanObj, jField);
                    String beanName = getBeanName(jField);
                    handleInjectedPointer(instanceField, beanName);
                });
    }

    /**
     * Simulates invocation of all factory methods associated with the given bean object,
     * including monitoring return-value pointer changes to detect factory bean object creation.
     */
    private void callFactoryMethods(CSObj beanObj) {
        JClass jClass = ((ClassType) beanObj.getObject().getType()).getJClass();
        Set<JMethod> factoryMethods = collector.getFactoryMethods(jClass);
        factoryMethods.forEach(factoryMethod -> {
            FactoryBeanDefinition factoryBean = collector.getFactoryBeanDefinition(factoryMethod);
            // For singletons, directly invoke the bean method to create the factory bean object.

            // Invoke the method
            Context ctx = callMethod(beanObj, factoryMethod);

            // Monitor return-value pointer changes
            Pointer singletonPointer = bean2SingletonPointer.get(factoryBean);
            factoryMethod.getIR()
                    .getReturnVars()
                    .forEach(returnVar -> {
                        CSVar csReturnVar = csManager.getCSVar(ctx, returnVar);
                        returnVarsOfBeanMethods.add(csReturnVar);
                        solver.addPFGEdge(csReturnVar, singletonPointer, FlowKind.OTHER);
                    });
        });
    }

    private int stmtCount = 0;

    /**
     * @return callee context
     */
    private Context callMethod(CSObj recvObj, JMethod callee) {
        InvokeExp mockInvokeExp = callee.isStatic()
                ? new InvokeStatic(callee.getRef(), List.of())
                : new InvokeVirtual(callee.getRef(),
                new Var(springMain, "mockBaseVar", recvObj.getObject().getType(), -1),
                List.of());
        Invoke mockInvoke = new Invoke(springMain, mockInvokeExp, null);
        mockInvoke.setIndex(stmtCount++);
        CSCallSite mockCSCallSite = csManager.getCSCallSite(emptyContext, mockInvoke);

        Context ctx = callee.isStatic()
                ? selector.selectContext(mockCSCallSite, callee)
                : selector.selectContext(mockCSCallSite, recvObj, callee);
        CSMethod csCallee = csManager.getCSMethod(ctx, callee);
        solver.addCSMethod(csCallee);
        solver.addCallEdge(new Edge<>(CallKind.OTHER, mockCSCallSite, csCallee));
        // Handle 'this'
        if (!callee.isStatic()) {
            solver.addPointsTo(csManager.getCSVar(ctx, callee.getIR().getThis()), recvObj);
        }
        // Handle method parameters
        for (int i = 0; i < callee.getParamCount(); ++i) {
            Var param = callee.getIR().getParam(i);
            if (param.getType() instanceof ClassType classType) {
                String beanName = getBeanName(callee, i, classType);
                handleInjectedPointer(csManager.getCSVar(ctx, param), beanName);
            }
        }
        return ctx;
    }

    /**
     * Injects the appropriate bean objects into the given pointer.
     * Looks up bean definitions by bean name first; if none are found,
     * falls back to lookup by type.
     */
    private void handleInjectedPointer(Pointer injectedPointer, String beanName) {
        Set<BeanDefinition> beans = Sets.newSet(collector.getBeanDefinition(beanName));
        if (beans.isEmpty() &&
                injectedPointer.getType() instanceof ClassType injectedClassType) {
            hierarchy.getAllSubclassesOf(injectedClassType.getJClass())
                    .forEach(jClass -> beans.addAll(collector.getBeanDefinition(jClass)));
        }
        beans.removeIf(bean -> !isInjectableByType(injectedPointer, bean));
        beans.forEach(bean -> {
            // For singleton beans, propagate the singleton pointer's points-to set to the target pointer.
            solver.addPFGEdge(bean2SingletonPointer.get(bean), injectedPointer, FlowKind.OTHER);
        });

    }

    private boolean isInjectableByType(Pointer injectedPointer,
                                       BeanDefinition bean) {
        var injectedType = injectedPointer.getType();
        var beanType = bean.getJClass().getType();
        return typeSystem.isAssignable(injectedType, beanType);
    }

    /**
     * Gets the name of the bean that the given field depends on.
     */
    private static String getBeanName(JField jField) {
        String beanName = null;
        for (String qualifierAnno : SpringPluginConfig.INSTANCE.getQualifierAnnos()) {
            Annotation qualifier = jField.getAnnotation(qualifierAnno);
            if (qualifier != null) {
                beanName = AnnotationUtils.getStringElementAlias(
                        qualifier, "value", "name");
                break;
            }
        }
        if (beanName == null) {
            beanName = jField.getName();
        }
        return beanName;
    }

    /**
     * Gets the name of the bean that the specified method parameter depends on
     * (the parameter must be of ClassType).
     */
    private static String getBeanName(JMethod m, int paramIndex, ClassType paramType) {
        String beanName = null;
        for (String qualifierAnno : SpringPluginConfig.INSTANCE.getQualifierAnnos()) {
            Annotation qualifier = m.getParamAnnotation(paramIndex, qualifierAnno);
            if (qualifier != null) {
                beanName = AnnotationUtils.getStringElementAlias(
                        qualifier, "value", "name");
                break;
            }
        }
        if (beanName == null) {
            String paramName = getParamName(m, paramIndex);
            beanName = paramName;
            if (paramName == null || (paramName.length() >= 2 && paramName.charAt(0) == 'r'
                    && Character.isDigit(paramName.charAt(1)))) {
                // parameter name will not be kept without '-parameters' in compiler args
                // so use the lower camel name of class as the parameter name
                beanName = SpringBeanNames.getDefaultBeanName(paramType.getJClass());
            }
        }
        return beanName;
    }

    private static String getParamName(JMethod method, int paramIndex) {
        String paramName = method.getParamName(paramIndex);
        if (paramName == null && !method.isAbstract() && !method.isNative()) {
            paramName = method.getIR().getParam(paramIndex).getName();
        }
        return paramName;
    }

}
