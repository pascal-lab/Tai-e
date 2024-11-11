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

package pascal.taie.project;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class SearchIndex {
    private record DuplicateClass(String internalName, Pair<FileContainer, FileContainer> jars) {}

    private final Map<String, AnalysisFile> index = new TreeMap<>();
    private final List<DuplicateClass> duplicateClasses = new ArrayList<>();
    private static final Logger logger = LogManager.getLogger(SearchIndex.class);

    private void add(String internalName, AnalysisFile file) {
        if (index.containsKey(internalName)
                && file instanceof ClassLike cl
                && !cl.getInternalName().contains("module-info")) {
            AnalysisFile file1 = index.get(internalName);
            Pair<FileContainer, FileContainer> jars = new Pair<>(
                    file1.rootContainer(), file.rootContainer());
            duplicateClasses.add(new DuplicateClass(internalName, jars));
            return;
        }
        index.put(internalName, file);
    }

    private void displayDuplicateWarning() {
        MultiMap<Pair<FileContainer, FileContainer>, String> duplicateClassMap =
                Maps.newMultiMap();

        for (DuplicateClass dc : duplicateClasses) {
            duplicateClassMap.put(dc.jars(), dc.internalName());
        }
        StringBuilder message = new StringBuilder();
        duplicateClassMap.forEachSet((jars, classes) -> {
            message.append(ppInfo(jars, classes));
        });
        logger.warn(message.toString());
    }

    private static String ppInfo(Pair<FileContainer, FileContainer> jars, Set<String> classes) {
       return  String.format(
               """
               Non-deterministic classes (total %d) resolving introduced by %s and %s,
               %s is founded in both jars
               """,
               classes.size(), jars.first(), jars.second(), ppList(classes.stream().toList()));
    }
    private static String ppList(List<String> classes) {
        if (classes.size() <= 4) {
            return classes.toString();
        } else {
            return String.format("%s ... (%d more)",
                    classes.subList(0, 4), classes.size() - 4);
        }
    }

    public AnalysisFile get(String fileName) {
        return index.get(fileName);
    }

    public static SearchIndex makeIndex(Project project) {
        SearchIndex index = new SearchIndex();
        Set<FileContainer> roots = Sets.newSet();
        roots.addAll(project.getAppRootContainers());
        roots.addAll(project.getLibRootContainers());
        for (FileContainer root : roots) {
            index.trav("", root);
        }
        index.displayDuplicateWarning();
        return index;
    }

    private void trav(String currentName, FileContainer container) {
        for (AnalysisFile file : container.files()) {
            add(currentName + file.fileName(), file);
        }
        for (FileContainer subContainer : container.containers()) {
            trav(currentName + subContainer.fileName() + "/", subContainer);
        }
    }

    public AnalysisFile locate(String internalName) {
        AnalysisFile klass = index.get(internalName + ".class");
        if (klass != null) {
            return klass;
        } else {
            return index.get(internalName + ".java");
        }
    }
}
