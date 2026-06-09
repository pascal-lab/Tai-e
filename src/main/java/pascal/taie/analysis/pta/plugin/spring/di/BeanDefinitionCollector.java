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

import pascal.taie.config.AnalysisOptions;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.analysis.pta.plugin.spring.SpringPluginConfig;
import pascal.taie.analysis.pta.plugin.spring.util.AnnotationManager;
import pascal.taie.analysis.pta.plugin.spring.util.XmlConfiguration;

import java.util.Set;

/**
 * Aggregates bean definition collection from multiple sub-collectors
 * and maintains shared indexes for bean lookup and injection.
 */
public class BeanDefinitionCollector {

    private final ClassHierarchy hierarchy;

    private final AnnotationManager annoManager;

    private final CommonBeanCollector commonBeanCollector;

    private final FactoryBeanCollector factoryBeanCollector;

    private final Set<BeanDefinition> beanDefinitions = Sets.newSet();

    private final MultiMap<JClass, JField> jClass2InjectedFields = Maps.newMultiMap();

    private final MultiMap<JClass, JMethod> jClass2InjectedMethods = Maps.newMultiMap();

    /**
     * At runtime, a bean name maps to a single bean definition, but since static analysis
     * cannot determine the registration order, all definitions are kept for soundness.
     */
    private final MultiMap<String, BeanDefinition> beanName2BeanDefinitions = Maps.newMultiMap();

    private final MultiMap<JClass, BeanDefinition> beanType2BeanDefinitions = Maps.newMultiMap();

    public BeanDefinitionCollector(ClassHierarchy hierarchy,
                                   AnnotationManager annoManager,
                                   XmlConfiguration xmlConfig) {
        this.hierarchy = hierarchy;
        this.annoManager = annoManager;
        this.commonBeanCollector = new CommonBeanCollector(hierarchy, annoManager, xmlConfig);
        this.factoryBeanCollector = new FactoryBeanCollector(hierarchy, annoManager);
    }

    public void work() {
        commonBeanCollector.collect(beanDefinitions);
        factoryBeanCollector.collect(beanDefinitions);

        beanDefinitions.forEach(definition -> {
            definition.getBeanNames().forEach(name ->
                    beanName2BeanDefinitions.put(name, definition));
            beanType2BeanDefinitions.put(definition.getJClass(), definition);
        });

        for (String mAnno : SpringPluginConfig.INSTANCE.getInjectedMethodAnnos()) {
            annoManager.visitMethods(mAnno, this::registerInjectedMethod);
        }
        for (String fAnno : SpringPluginConfig.INSTANCE.getInjectedFieldAnnos()) {
            annoManager.visitFields(fAnno, this::registerInjectedField);
        }

        factoryBeanCollector.registerFactoryMethods();
        factoryBeanCollector.buildFactoryMethodIndex(beanDefinitions);
    }

    public void processResult(AnalysisOptions options) {
        DiResultProcessor.INSTANCE.process(options, beanDefinitions.stream().toList());
    }

    private void registerInjectedMethod(Annotation anno, JMethod injectedMethod) {
        if (injectedMethod.isAbstract()) {
            return;
        }
        hierarchy.getAllSubclassesOf(injectedMethod.getDeclaringClass())
                .forEach(jClass -> jClass2InjectedMethods.put(jClass, injectedMethod));
    }

    private void registerInjectedField(Annotation anno, JField injectedField) {
        hierarchy.getAllSubclassesOf(injectedField.getDeclaringClass())
                .forEach(jClass -> jClass2InjectedFields.put(jClass, injectedField));
    }

    public Set<BeanDefinition> getBeanDefinitions() {
        return beanDefinitions;
    }

    protected Set<JField> getInjectedFields(JClass jClass) {
        return jClass2InjectedFields.get(jClass);
    }

    protected Set<JMethod> getInjectedMethods(JClass jClass) {
        return jClass2InjectedMethods.get(jClass);
    }

    protected Set<JMethod> getFactoryMethods(JClass jClass) {
        return factoryBeanCollector.getFactoryMethods(jClass);
    }

    protected Set<BeanDefinition> getBeanDefinition(String beanName) {
        return beanName2BeanDefinitions.get(beanName);
    }

    protected Set<BeanDefinition> getBeanDefinition(JClass jClass) {
        return beanType2BeanDefinitions.get(jClass);
    }

    protected FactoryBeanDefinition getFactoryBeanDefinition(JMethod factoryMethod) {
        return factoryBeanCollector.getFactoryBeanDefinition(factoryMethod);
    }

}
