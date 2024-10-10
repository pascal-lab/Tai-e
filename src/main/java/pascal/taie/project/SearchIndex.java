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

import java.util.Map;
import java.util.TreeMap;

public class SearchIndex {
    Map<String, AnalysisFile> index = new TreeMap<>();

    public void add(String binaryName, AnalysisFile file) {
        index.put(binaryName, file);
    }

    public AnalysisFile get(String fileName) {
        return index.get(fileName);
    }

    public static SearchIndex makeIndex(Project project) {
        SearchIndex index = new SearchIndex();
        for (FileContainer container : project.getAppRootContainers()) {
            index.trav("", container);
        }
        for (FileContainer container : project.getLibRootContainers()) {
            index.trav("", container);
        }
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

    public AnalysisFile locate(String binaryName) {
        AnalysisFile klass = index.get(binaryName + ".class");
        if (klass != null) {
            return klass;
        } else {
            return index.get(binaryName + ".java");
        }
    }
}
