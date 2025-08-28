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

import java.util.List;
import java.util.Set;

/**
 * Representation of a Java project.
 */
public record Project(String classPath,
                      String mainClass, Set<String> inputClasses,
                      List<FileContainer> appRootContainers,
                      List<FileContainer> libRootContainers,
                      int javaVersion) {

    public boolean isApp(ClassFile file) {
        return appRootContainers.contains(file.getRootContainer())
                || inputClasses.contains(file.getClassName())
                || file.getClassName().equals(mainClass);
    }

    /**
     * @param className the fully qualified name to the analysis file.
     * @return the first file (with the same fully qualified name) found in the containerLists.
     * (QUESTION: how to define priority between different rootContainers?)
     */
    public ClassFile locate(String className) {
        List<List<FileContainer>> rootContainersList =
                List.of(appRootContainers, libRootContainers);
        for (List<FileContainer> rootContainers : rootContainersList) {
            for (FileContainer container : rootContainers) {
                // make sure to keep the order.
                ClassLocation classLocation = new ClassLocation(className);
                assert classLocation.hasNext();
                ClassFile result = container.locate(classLocation);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
