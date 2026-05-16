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

// Java
package pascal.taie.frontend.java.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;

/**
 * Maintains an index of class files for quick lookup based on the class names.
 * Detects and logs duplicate class files found in different containers.
 */
public class ClassIndex {

    private static final Logger logger = LogManager.getLogger(ClassIndex.class);

    /**
     * The list defining the priority order of class file types to index.
     */
    private final List<Class<? extends ClassFile>> classPriority;

    /**
     * The main map from class names to their associated class file.
     */
    private final Map<String, ClassFile> index = Maps.newMap();

    /**
     * A record to represent a duplicate class definition.
     */
    private record DuplicateClass(String className, Pair<FileContainer, FileContainer> containers) {
    }

    /**
     * The list of duplicate classes found during indexing.
     */
    private final List<DuplicateClass> duplicateClasses = new ArrayList<>();

    /**
     * Creates a {@link ClassIndex} for the given project by traversing all root containers.
     *
     * @param project the project to index
     */
    ClassIndex(Project project) {
        classPriority = project.classPriority();
        project.appRootContainers().forEach(this::traverse);
        project.libRootContainers().forEach(this::traverse);
        project.jreRootContainers().forEach(this::traverse);
        logDuplicateClasses();
    }

    /**
     * Finds a {@link ClassFile} by its name.
     *
     * @param className the fully-qualified name of the class to search
     * @return the found ClassFile, or {@code null} if not found.
     */
    @Nullable
    public ClassFile find(String className) {
        return index.get(className);
    }

    /**
     * Recursively traverses the file containers, adding all class files to the index.
     */
    private void traverse(FileContainer container) {
        container.getFiles().forEach(this::add);
        container.getSubContainers().forEach(this::traverse);
    }

    private void add(ClassFile classFile) {
        String className = classFile.getClassName();
        if (index.containsKey(className)
                && !classFile.getClassName().contains("module-info")) {
            ClassFile existed = index.get(className);
            switch (compare(existed, classFile)) {
                // found duplicate classes
                case 0 -> duplicateClasses.add(new DuplicateClass(className,
                        new Pair<>(existed.getRootContainer(), classFile.getRootContainer())));
                // classFile has higher priority than existed,
                // then replace the existed by classFile
                case 1 -> index.put(className, classFile);
            }
            return;
        }
        index.put(className, classFile);
    }

    private int compare(ClassFile c1, ClassFile c2) {
        int index1 = classPriority.indexOf(c1.getClass());
        if (index1 == -1) { // out-of-scope class type, give the lowest priority
            index1 = classPriority.size();
        }
        int index2 = classPriority.indexOf(c2.getClass());
        if (index2 == -1) { // out-of-scope class type, give the lowest priority
            index2 = classPriority.size();
        }
        return Integer.compare(index1, index2);
    }

    private void logDuplicateClasses() {
        MultiMap<Pair<FileContainer, FileContainer>, String> duplicateClassMap = Maps.newMultiMap();
        duplicateClasses.forEach(dup ->
                duplicateClassMap.put(dup.containers(), dup.className()));
        StringBuilder message = new StringBuilder();
        duplicateClassMap.forEachSet((containers, classes) ->
                message.append(truncateDuplicates(containers, classes)));
        logger.warn(message.toString());
    }

    /**
     * Truncate the information about duplicate classes for better display.
     *
     * @param containers the pair of file containers containing the duplicates
     * @param classes    the set of classes duplicated between the containers
     * @return the formatted message
     */
    private static String truncateDuplicates(Pair<FileContainer, FileContainer> containers,
                                             Set<String> classes) {
        String classList = (classes.size() <= 4)
                ? classes.toString()
                : String.format("%s ... (%d more)",
                new ArrayList<>(classes).subList(0, 4),
                classes.size() - 4);
        return String.format(
                """
                        Duplicate classes (total %d) introduced by %s and %s,
                        %s is founded in both containers
                        """,
                classes.size(),
                containers.first(), containers.second(),
                classList);
    }
}
