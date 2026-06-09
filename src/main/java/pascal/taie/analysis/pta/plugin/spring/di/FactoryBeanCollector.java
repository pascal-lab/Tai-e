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

import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.Streams;
import pascal.taie.analysis.pta.plugin.spring.SpringPluginConfig;
import pascal.taie.analysis.pta.plugin.spring.util.AnnotationManager;
import pascal.taie.analysis.pta.plugin.spring.util.AnnotationUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Collects {@link FactoryBeanDefinition}s from @Bean method annotations.
 */
class FactoryBeanCollector {

    private final ClassHierarchy hierarchy;

    private final AnnotationManager annoManager;

    private final MultiMap<JClass, JMethod> jClass2FactoryMethods = Maps.newMultiMap();

    private final Map<JMethod, FactoryBeanDefinition> factoryMethod2FactoryBeanDefinition = Maps.newMap();

    FactoryBeanCollector(ClassHierarchy hierarchy, AnnotationManager annoManager) {
        this.hierarchy = hierarchy;
        this.annoManager = annoManager;
    }

    void collect(Set<BeanDefinition> beanDefinitions) {
        for (String mAnno : SpringPluginConfig.INSTANCE.getDiFactoryMethodAnnos()) {
            annoManager.visitMethods(mAnno, (anno, method) ->
                    visitFactoryMethod(beanDefinitions, anno, method));
        }
    }

    void registerFactoryMethods() {
        for (String mAnno : SpringPluginConfig.INSTANCE.getDiFactoryMethodAnnos()) {
            annoManager.visitMethods(mAnno, this::registerFactoryMethod);
        }
    }

    void buildFactoryMethodIndex(Set<BeanDefinition> beanDefinitions) {
        beanDefinitions.stream()
                .filter(bean -> bean instanceof FactoryBeanDefinition)
                .forEach(factoryBean -> {
                    JMethod factoryMethod = ((FactoryBeanDefinition) factoryBean).getFactoryMethod();
                    if (factoryMethod2FactoryBeanDefinition.containsKey(factoryMethod)) {
                        throw new IllegalArgumentException(
                                "Duplicate factory method: " + factoryMethod);
                    }
                    factoryMethod2FactoryBeanDefinition.put(factoryMethod, (FactoryBeanDefinition) factoryBean);
                });
    }

    private void visitFactoryMethod(Set<BeanDefinition> beanDefinitions,
                                    Annotation factoryMethodAnno,
                                    JMethod factoryMethod) {
        if (factoryMethod.getReturnType() instanceof ClassType classType) {
            JClass jClass = classType.getJClass();
            List<String> beanNames = AnnotationUtils.getStringArrayElementAlias(
                    factoryMethodAnno, "name", "value");
            if (beanNames.isEmpty()) {
                beanNames = List.of(factoryMethod.getName());
            }
            if (beanNames.isEmpty()) {
                throw new IllegalArgumentException(
                        "A FactoryBeanDefinition must have at least one bean name.");
            }
            beanDefinitions.add(new FactoryBeanDefinition(beanNames, jClass, true, factoryMethod));
        }
    }

    private void registerFactoryMethod(Annotation anno, JMethod factoryMethod) {
        if (factoryMethod.isAbstract()) {
            return;
        }
        resolveContainingClass(factoryMethod).forEach(jClass -> {
            jClass2FactoryMethods.put(jClass, factoryMethod);
        });
    }

    private Set<JClass> resolveContainingClass(JMethod factoryMethod) {
        Set<JClass> containingClasses = Sets.newSet();
        Stack<JClass> stack = new Stack<>();

        JClass rootClass = factoryMethod.getDeclaringClass();
        Subsignature sig = factoryMethod.getSubsignature();
        containingClasses.add(rootClass);

        Streams.concat(hierarchy.getDirectSubclassesOf(rootClass).stream(),
                hierarchy.getDirectImplementorsOf(rootClass).stream(),
                hierarchy.getDirectSubinterfacesOf(rootClass).stream())
                .forEach(stack::push);

        while (!stack.isEmpty()) {
            JClass curClass = stack.pop();
            if (curClass.getDeclaredMethod(sig) != null) {
                continue;
            }
            containingClasses.add(curClass);
            Streams.concat(hierarchy.getDirectSubclassesOf(curClass).stream(),
                            hierarchy.getDirectImplementorsOf(curClass).stream(),
                            hierarchy.getDirectSubinterfacesOf(curClass).stream())
                    .forEach(stack::push);
        }
        return containingClasses;
    }

    Set<JMethod> getFactoryMethods(JClass jClass) {
        return jClass2FactoryMethods.get(jClass);
    }

    FactoryBeanDefinition getFactoryBeanDefinition(JMethod factoryMethod) {
        return factoryMethod2FactoryBeanDefinition.get(factoryMethod);
    }

}
