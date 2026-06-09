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

package pascal.taie.analysis.pta.plugin.spring.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pascal.taie.World;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;

import java.util.Collection;
import java.util.function.BiConsumer;

/**
 * A manager extracting the annotation-related information
 * of concerned classes from the {@link World}.
 */
public final class AnnotationManager {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationManager.class);

    private final Collection<String> classNames;

    /**
     * Stores the mapping from annotation name to the classes annotated by it.
     * For example: "org.springframework.stereotype.Service" -> [(ServiceAnnotation, UserService)]
     */
    private final MultiMap<String, Pair<Annotation, JClass>> anno2Classes
            = Maps.newMultiMap();

    /**
     * Stores the mapping from annotation name to the fields annotated by it.
     * For example: "javax.persistence.Id" -> [(IdAnnotation, userIdField)]
     *              "org.springframework.beans.factory.annotation.Autowired" -> [(AutowiredAnnotation, repositoryField)]
     */
    private final MultiMap<String, Pair<Annotation, JField>> anno2Fields
            = Maps.newMultiMap();

    /**
     * Stores the mapping from annotation name to the methods annotated by it.
     * For example: "org.springframework.web.bind.annotation.GetMapping" -> [(GetMappingAnnotation, getUserMethod)]
     */
    private final MultiMap<String, Pair<Annotation, JMethod>> anno2Methods
            = Maps.newMultiMap();

    public AnnotationManager(Collection<String> classNames) {
        this.classNames = classNames;
    }

    public void initialize() {
        ClassHierarchy hierarchy = World.get().getClassHierarchy();
        classNames.forEach(name -> {
            JClass jClass = hierarchy.getClass(name);
            if (jClass == null) {
                logger.warn("Failed to resolve class: {}", name);
            } else {
                visitClass(jClass);
            }
        });
    }

    /**
     * Processes a single class and extracts all annotation information
     * for the class itself, its fields, and its methods.
     */
    private void visitClass(JClass jClass) {
        // Process class-level annotations
        for (Annotation anno : jClass.getAnnotations()) {
            anno2Classes.put(anno.getType(), new Pair<>(anno, jClass));
        }
        // Process field-level annotations
        for (JField jField : jClass.getDeclaredFields()) {
            for (Annotation anno : jField.getAnnotations()) {
                anno2Fields.put(anno.getType(), new Pair<>(anno, jField));
            }
        }
        // Process method-level annotations
        for (JMethod jMethod : jClass.getDeclaredMethods()) {
            for (Annotation anno : jMethod.getAnnotations()) {
                anno2Methods.put(anno.getType(), new Pair<>(anno, jMethod));
            }
        }
    }

    /**
     * Visits all classes annotated with the specified annotation.
     *
     * @param annotation the annotation type name (e.g., "org.springframework.stereotype.Service")
     * @param consumer   the consumer to process each (annotation, class) pair
     */
    public void visitClasses(String annotation,
                             BiConsumer<Annotation, JClass> consumer) {
        anno2Classes.get(annotation)
                .forEach(pair -> consumer.accept(
                        pair.first(), pair.second()));
    }

    /**
     * Visits all fields annotated with the specified annotation across all processed classes.
     *
     * @param annotation the annotation type name
     * @param consumer   the consumer to process each (annotation, field) pair
     */
    public void visitFields(String annotation,
                            BiConsumer<Annotation, JField> consumer) {
        anno2Fields.get(annotation).forEach(pair ->
                consumer.accept(pair.first(), pair.second()));
    }

    /**
     * Visits all methods annotated with the specified annotation.
     *
     * @param annotation the annotation type name
     * @param consumer   the consumer to process each (annotation, method) pair
     */
    public void visitMethods(String annotation,
                             BiConsumer<Annotation, JMethod> consumer) {
        anno2Methods.get(annotation).forEach(pair ->
                consumer.accept(pair.first(), pair.second()));
    }

}
