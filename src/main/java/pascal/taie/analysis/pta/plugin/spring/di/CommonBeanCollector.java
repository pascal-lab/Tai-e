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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.analysis.pta.plugin.spring.SpringPluginConfig;
import pascal.taie.analysis.pta.plugin.spring.util.AnnotationManager;
import pascal.taie.analysis.pta.plugin.spring.util.AnnotationUtils;
import pascal.taie.analysis.pta.plugin.spring.util.SpringBeanNames;
import pascal.taie.analysis.pta.plugin.spring.util.XmlConfiguration;

import java.util.List;
import java.util.Set;

import static pascal.taie.analysis.pta.plugin.spring.SpringPluginConfig.DiXmlTag;

/**
 * Collects {@link CommonBeanDefinition}s from annotations and XML configuration.
 */
class CommonBeanCollector {

    private static final Logger logger = LoggerFactory.getLogger(CommonBeanCollector.class);

    private final ClassHierarchy hierarchy;

    private final AnnotationManager annoManager;

    private final XmlConfiguration xmlConfig;

    CommonBeanCollector(ClassHierarchy hierarchy,
                        AnnotationManager annoManager,
                        XmlConfiguration xmlConfig) {
        this.hierarchy = hierarchy;
        this.annoManager = annoManager;
        this.xmlConfig = xmlConfig;
    }

    void collect(Set<BeanDefinition> beanDefinitions) {
        collectFromXml(beanDefinitions);
        collectFromAnnotations(beanDefinitions);
    }

    private void collectFromXml(Set<BeanDefinition> beanDefinitions) {
        for (DiXmlTag diXmlTag : SpringPluginConfig.INSTANCE.getDiXmlTags()) {
            String beanTagName = diXmlTag.tagName();
            String classAttrName = diXmlTag.classAttr();
            String idAttrName = diXmlTag.idAttr();

            for (Node xmlNode : xmlConfig.getNodesByTag(beanTagName)) {
                NamedNodeMap attrs = xmlNode.getAttributes();
                Node classAttr = attrs.getNamedItem(classAttrName);
                if (classAttr == null) {
                    continue;
                }
                String className = classAttr.getTextContent();
                String beanName;

                Node idAttr = attrs.getNamedItem(idAttrName);
                if (idAttr != null) {
                    beanName = idAttr.getTextContent();
                } else {
                    beanName = SpringBeanNames.getDefaultBeanName(className);
                }
                JClass jClass = hierarchy.getClass(className);
                if (jClass == null) {
                    logger.info("[DI Analysis] Missing bean class '{}' with bean name '{}' required in XML.",
                            className, beanName);
                    continue;
                }

                createCommonBean(beanDefinitions, jClass, beanName);
            }
        }
    }

    private void collectFromAnnotations(Set<BeanDefinition> beanDefinitions) {
        for (String classAnno : SpringPluginConfig.INSTANCE.getDiClassAnnos()) {
            annoManager.visitClasses(classAnno, (anno, jClass) -> {
                String beanName = AnnotationUtils.getStringElement(anno);
                if (beanName == null) {
                    beanName = SpringBeanNames.getDefaultBeanName(jClass);
                }
                createCommonBean(beanDefinitions, jClass, beanName);
            });
        }
    }

    private void createCommonBean(Set<BeanDefinition> beanDefinitions,
                                  JClass jClass, String beanName) {
        if (jClass.isInterface()) {
            return;
        }

        List<JMethod> allConstructors = jClass.getDeclaredMethods()
                .stream().filter(JMethod::isConstructor).toList();

        JMethod constructor = null;
        List<String> diClassCtorAnnos = SpringPluginConfig.INSTANCE.getDiClassCtorAnnos();
        for (JMethod ctor : allConstructors) {
            for (String anno : diClassCtorAnnos) {
                if (ctor.getAnnotation(anno) != null) {
                    if (constructor != null) {
                        throw new IllegalStateException("Multiple constructors in class "
                                + jClass.getName() + " are annotated with DI annotations: "
                                + diClassCtorAnnos);
                    }
                    constructor = ctor;
                }
            }
        }
        if (constructor == null) {
            constructor = allConstructors.stream()
                    .filter(m -> m.getParamCount() == 0)
                    .findFirst().orElse(null);
        }
        if (constructor == null) {
            if (allConstructors.size() != 1) {
                throw new IllegalArgumentException(
                        "Only one constructor allowed in a CommonBeanDefinition.");
            }
            constructor = allConstructors.get(0);
        }
        if (constructor == null) {
            throw new IllegalArgumentException(
                    "The constructor of a CommonBeanDefinition is null.");
        }

        beanDefinitions.add(new CommonBeanDefinition(beanName, jClass, true, constructor));
    }

}
