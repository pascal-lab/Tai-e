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

package pascal.taie.analysis.pta.plugin.spring.wec;

import pascal.taie.config.AnalysisOptions;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.analysis.pta.plugin.spring.SpringPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.annotation.ArrayElement;
import pascal.taie.language.annotation.Element;
import pascal.taie.language.annotation.EnumElement;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.analysis.pta.plugin.spring.util.AnnotationManager;
import pascal.taie.analysis.pta.plugin.spring.util.AnnotationUtils;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is responsible for collecting all web endpoints in the Java web application.
 */
public class WecCollector {

    private static final Logger logger = LoggerFactory.getLogger(WecCollector.class);

    /**
     * The class hierarchy used for inheritance-based lookups.
     */
    private final ClassHierarchy hierarchy;

    /**
     * Manager for handling annotations within the app.
     */
    private final AnnotationManager annoManager;

    /**
     * The cache of {@link WecCollector#getPathOnClass(JClass)}
     */
    private final MultiMap<JClass, String> class2Paths = Maps.newMultiMap();

    /**
     * Set containing all detected web endpoints in the application.
     */
    private final Set<WebEndpoint> webEndpoints = Sets.newSet();

    /**
     * Mapping between containing class and corresponding web end points.
     */
    private final MultiMap<JClass, WebEndpoint> containingClass2Wecs = Maps.newMultiMap();

    public WecCollector(ClassHierarchy hierarchy, AnnotationManager annoManager) {
        this.hierarchy = hierarchy;
        this.annoManager = annoManager;
    }

    public void work() {
        for (String mAnno : SpringPluginConfig.INSTANCE.getEndpointMetadataMethodAnnos()) {
            annoManager.visitMethods(mAnno, this::collectWebEndPoint);
        }
        webEndpoints.forEach(webEndpoint -> containingClass2Wecs.put(webEndpoint.containingClass(), webEndpoint));
    }

    public void processResult(AnalysisOptions options) {
        WecResultProcessor.INSTANCE.process(options, webEndpoints.stream().toList());
    }

    /**
     * Handles a method annotated with an endpoint-related annotation.
     * Resolves all concrete handler methods corresponding to the annotated method
     * and their containing classes, then constructs and records web endpoints.
     * Each endpoint consists of a concrete handler method, its containing class,
     * request paths, and the associated request methods
     *
     * @param anno the endpoint metadata annotation on the method
     * @param annotatedMethod the method with the endpoint metadata annotation
     */
    private void collectWebEndPoint(Annotation anno, JMethod annotatedMethod) {
        JClass jClass = annotatedMethod.getDeclaringClass();
        Set<RequestMethod> requestMethods = getRequestMethods(anno);
        if (requestMethods.isEmpty()) {
            return;
        }
        // resolves all concrete handler methods corresponding to the annotated method, and their containing classes.
        MultiMap<JMethod, JClass> concreteMethod2ContainingClasses = Maps.newMultiMap();
        resolve(annotatedMethod.getSubsignature(), jClass, null, concreteMethod2ContainingClasses, true);

        concreteMethod2ContainingClasses.forEach((concreteMethod, containingClass) -> {
            if (isEndpointClass(containingClass)) {
                // A wec consists of a concrete (handler) method and a corresponding containing class.
                webEndpoints.add(buildWebEndpoint(concreteMethod, annotatedMethod, containingClass, anno, requestMethods));
            }
        });

    }

    private WebEndpoint buildWebEndpoint(JMethod handlerMethod, JMethod annotatedMethod, JClass containingClass, Annotation anno, Set<RequestMethod> requestMethods) {

        Map<String, Integer> requestParam2ParamIndex = getRequestParam2ParamIndex(annotatedMethod);

        Set<String> pathsOnClass = getPathOnClass(containingClass);
        if (pathsOnClass.isEmpty()) {
            pathsOnClass = Set.of("/");
        }

        List<String> annoPaths = AnnotationUtils.getStringArrayElementAlias(anno, "value", "path");
        List<String> pathsOnMethod = annoPaths.isEmpty() ? List.of("") : annoPaths;

        // merge the paths defined on the containing class and annotated method.
        Set<String> paths =
                pathsOnClass.stream()
                        .map(pathOnClass ->
                                pathsOnMethod.stream()
                                        .map(pathOnMethod -> WebEndpoint.mergePath(pathOnClass, pathOnMethod)).collect(Collectors.toList()))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());

        return new WebEndpoint(containingClass, handlerMethod, paths, requestMethods, requestParam2ParamIndex);
    }

    /**
     * Determines if a class is an endpoint class by checking if it has any endpoint class annotations.
     *
     * @param jClass the class to check
     * @return true if the class has any endpoint class annotations, false otherwise
     */
    private boolean isEndpointClass(JClass jClass) {
        for (String anno : SpringPluginConfig.INSTANCE.getEndpointClassAnnos()) {
            if (jClass.getAnnotation(anno) != null) {
                return true;
            }
        }
        return false;
    }


    /**
     * Traverses the class hierarchy to resolves all concrete handler methods corresponding to the annotated method and their containing classes.
     * <p>
     * The traversal accounts for two important cases:
     * 1. The annotated method may be implemented or overridden in subclasses
     *    during downward traversal.
     * 2. A single concrete method may correspond to multiple containing classes
     *    (e.g., when inherited by multiple subclasses).
     *
     * @param sig the subsignature of the annotated method
     * @param curClass the current class under traversal
     * @param curConcreteMethod the concrete implementation of the annotated method found so far (if any, possibly an override)
     * @param concreteMethod2ContainingClasses a mapping that records each concrete method and its corresponding containing classes
     * @param isRoot Whether the current class being traversed is the root node
     */
    private void resolve(Subsignature sig, JClass curClass, @Nullable JMethod curConcreteMethod, MultiMap<JMethod, JClass> concreteMethod2ContainingClasses, boolean isRoot) {
        if (curClass == null) {
            return;
        }

        JMethod jMethod = curClass.getDeclaredMethod(sig);

        if (jMethod != null) {
            if (!isRoot && hasEndpointMetadataMethodAnno(jMethod)) {
                return; // a new process will begin from this method
            }
            // the annotated method is implemented or override
            if (!jMethod.isAbstract()) {
                curConcreteMethod = jMethod;
            }
        }

        // add the concrete method and corresponding containing class to results
        if (!curClass.isAbstract() && curConcreteMethod != null) {
            concreteMethod2ContainingClasses.put(curConcreteMethod, curClass);
        }

        // traverses the class hierarchy to resolves
        if (curClass.isInterface()) {
            Collection<JClass> directImplementors = hierarchy.getDirectImplementorsOf(curClass);
            for (JClass directImplementor : directImplementors) {
                resolve(sig, directImplementor, curConcreteMethod, concreteMethod2ContainingClasses, false);
            }
            Collection<JClass> directSubinterfaces = hierarchy.getDirectSubinterfacesOf(curClass);
            for (JClass directSubinterface : directSubinterfaces) {
                resolve(sig, directSubinterface, curConcreteMethod, concreteMethod2ContainingClasses, false);
            }
        } else {
            Collection<JClass> directSubclasses = hierarchy.getDirectSubclassesOf(curClass);
            for (JClass directSubclass : directSubclasses) {
                resolve(sig, directSubclass, curConcreteMethod, concreteMethod2ContainingClasses, false);
            }
        }
    }

    /**
     * Checks if a method has any endpoint metadata annotations.
     *
     * @param jMethod the method to check
     * @return true if the method has any endpoint metadata annotations, false otherwise
     */
    private boolean hasEndpointMetadataMethodAnno(JMethod jMethod) {
        for (String anno : SpringPluginConfig.INSTANCE.getEndpointMetadataMethodAnnos()) {
            if (jMethod.getAnnotation(anno) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the request paths defined on a class through annotations.
     * If the class doesn't have path annotations, recursively searches
     * through its interfaces and superclasses.
     *
     * @param jClass the class to get paths from, can be null
     * @return a set of paths associated with the class, empty if none found
     */
    private Set<String> getPathOnClass(@Nullable JClass jClass) {
        if (jClass == null || ClassNames.OBJECT.equals(jClass.getName())) {
            return Set.of();
        }
        if (class2Paths.containsKey(jClass)) {
            return class2Paths.get(jClass);
        }
        Set<String> allPaths = Sets.newSet();
        // to find
        for (Annotation anno : getEndpointMetadataAnnotations(jClass)) {
            List<String> paths = AnnotationUtils.getStringArrayElementAlias(anno, "value", "path");
            allPaths.addAll(paths);
        }
        // found and return
        if (!allPaths.isEmpty()) {
            class2Paths.putAll(jClass, allPaths);
            return allPaths;
        }
        // recursively find
        for (JClass anInterface : jClass.getInterfaces()) {
            Set<String> pathOnClass = getPathOnClass(anInterface);
            if (!pathOnClass.isEmpty()) {
                class2Paths.putAll(jClass, pathOnClass);
                return pathOnClass;
            }
        }
        return getPathOnClass(jClass.getSuperClass());
    }

    /**
     * Gets all endpoint metadata annotations from a class.
     *
     * @param jClass the class to get annotations from
     * @return a set of endpoint metadata annotations on the class
     */
    private Set<Annotation> getEndpointMetadataAnnotations(JClass jClass) {
        Set<Annotation> annotations = Sets.newSet();
        for (String anno : SpringPluginConfig.INSTANCE.getEndpointMetadataClassAnnos()) {
            Annotation annotation = jClass.getAnnotation(anno);
            if (annotation != null) {
                annotations.add(annotation);
            }
        }
        return annotations;
    }

    /**
     * Extracts the HTTP request methods (GET, POST, etc.) from an endpoint annotation.
     * Handles both explicit method specifications and implicit methods based on annotation type.
     *
     * @param anno the endpoint annotation to extract methods from
     * @return a set of request methods, empty if none could be determined
     */
    private Set<RequestMethod> getRequestMethods(Annotation anno) {
        JClass annoClass = hierarchy.getClass(anno.getType());
        if (annoClass != null && annoClass.getDeclaredMethod("method") != null) {
            Set<RequestMethod> results = Sets.newSet();
            if (anno.getElement("method") instanceof ArrayElement arrayElement) {
                for (Element element : arrayElement.elements()) {
                    if (element instanceof EnumElement enumElement) {
                        String name = enumElement.name().toLowerCase();
                        for (RequestMethod requestMethod : RequestMethod.values()) {
                            if (name.equals(requestMethod.name().toLowerCase())) {
                                results.add(requestMethod);
                            }
                        }
                    }
                }
            }
            if (results.isEmpty()) {
                return EnumSet.allOf(RequestMethod.class);
            }
            return results;
        } else {
            String name = anno.getType().toLowerCase();
            for (RequestMethod requestMethod : RequestMethod.values()) {
                if (name.contains(requestMethod.name().toLowerCase())) {
                    return EnumSet.of(requestMethod);
                }
            }
        }
        logger.info("[WEC Analysis] Unknown request method: {}", anno.getType());
        return Set.of();
    }

    /**
     * Maps request parameter names to their positions in the method parameter list.
     * Extracts this information from parameter annotations and method parameter names.
     *
     * @param method the handler method to process
     * @return a map from parameter names to their indices in the method signature
     */
    private Map<String, Integer> getRequestParam2ParamIndex(JMethod method) {
        Map<String, Integer> result = Maps.newLinkedHashMap();
        for (String paramAnno : SpringPluginConfig.INSTANCE.getEndpointParameterAnnos()) {
            for (int i = 0, paramCount = method.getParamCount(); i < paramCount; i++) {
                Annotation anno = method.getParamAnnotation(i, paramAnno);
                if (anno == null) {
                    continue;
                }
                String paramName = AnnotationUtils.getStringElementAlias(anno, "name", "value");
                if (paramName == null) {
                    paramName = getParamName(method, i);
                    if (paramName == null) {
                        logger.info("[WEC Analysis] Cannot get parameter name {} for method: {}",
                                i, method);
                    }
                }
                if (paramName == null) {
                    continue;
                }
                result.put(paramName, i);
            }
        }
        return result;
    }

    private static String getParamName(JMethod method, int paramIndex) {
        String paramName = method.getParamName(paramIndex);
        if (paramName == null && !method.isAbstract() && !method.isNative()) {
            paramName = method.getIR().getParam(paramIndex).getName();
        }
        return paramName;
    }

    public Set<WebEndpoint> getWebEndpoints(JClass containingClass) {
        return containingClass2Wecs.get(containingClass);
    }

}
