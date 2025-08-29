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
package pascal.taie.project;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maintains an index of class files for quick lookup based on the class names.
 * Detects and logs duplicate class files found in different containers.
 */
public class ClassIndex {

    private static final Logger logger = LogManager.getLogger(ClassIndex.class);

    /**
     * A record to represent a duplicate class definition.
     */
    private record DuplicateClass(String internalName, Pair<FileContainer, FileContainer> jars) {}

    /**
     * The main map from file names to their associated ProgramFile.
     */
    private final Map<String, ClassFile> index = Maps.newMap();

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
        Set<FileContainer> roots = Sets.newLinkedSet();
        roots.addAll(project.appRootContainers());
        roots.addAll(project.libRootContainers());
        for (FileContainer root : roots) {
            traverse("", root);
        }
        displayDuplicateWarning();
    }

    /**
     * Finds a {@link ClassFile} by its name.
     *
     * <p>
     * First attempts to find a class file. If not found, falls back to searching for a java source file.
     * </p>
     *
     * @param className the fully-qualified name of the class to search
     * @return the found ClassFile, or {@code null} if not found.
     */
    @Nullable
    public ClassFile find(String className) {
        ClassFile classFile = index.get(className + ".class");
        if (classFile != null) {
            return classFile;
        } else {
            return index.get(className + ".java");
        }
    }

    /**
     * Adds a ProgramFile to the index.
     *
     * @param internalName the internal name of the class
     * @param file the ProgramFile to add
     */
    private void add(String internalName, ClassFile file) {
        if (index.containsKey(internalName)
                && !file.getClassName().contains("module-info")) {
            ClassFile file1 = index.get(internalName);
            Pair<FileContainer, FileContainer> jars = new Pair<>(
                    file1.getRootContainer(), file.getRootContainer());
            duplicateClasses.add(new DuplicateClass(internalName, jars));
            return;
        }
        index.put(internalName, file);
    }

    /**
     * Displays warnings for all detected duplicate class definitions.
     */
    private void displayDuplicateWarning() {
        MultiMap<Pair<FileContainer, FileContainer>, String> duplicateClassMap = Maps.newMultiMap();

        for (DuplicateClass dc : duplicateClasses) {
            duplicateClassMap.put(dc.jars(), dc.internalName());
        }
        StringBuilder message = new StringBuilder();
        duplicateClassMap.forEachSet((jars, classes) -> {
            message.append(prettyPrintInfo(jars, classes));
        });
        logger.warn(message.toString());
    }

    /**
     * Pretty-prints the information about duplicate classes.
     *
     * @param jars the pair of file containers containing the duplicates
     * @param classes the set of classes duplicated between the containers
     * @return the formatted message
     */
    private static String prettyPrintInfo(Pair<FileContainer, FileContainer> jars, Set<String> classes) {
       return String.format(
               """
               Non-deterministic classes (total %d) resolving introduced by %s and %s,
               %s is founded in both jars
               """,
               classes.size(), jars.first(), jars.second(), ppList(classes.stream().toList()));
    }

    /**
     * Formats a list of class names for output.
     *
     * @param classes the list of class names
     * @return a formatted string representing the class names
     */
    private static String ppList(List<String> classes) {
        if (classes.size() <= 4) {
            return classes.toString();
        } else {
            return String.format("%s ... (%d more)",
                    classes.subList(0, 4), classes.size() - 4);
        }
    }

    /**
     * Recursively traverses the file containers, adding all encountered files to the index.
     *
     * @param currentName the current name prefix used during traversal
     * @param container the file container to traverse
     */
    private void traverse(String currentName, FileContainer container) {
        for (ClassFile file : container.getFiles()) {
            add(currentName + file.getFileName(), file);
        }
        for (FileContainer subContainer : container.getSubContainers()) {
            traverse(currentName + subContainer.getFileName() + "/", subContainer);
        }
    }
}
