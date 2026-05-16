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

package pascal.taie.frontend.java.project;

import java.util.List;
import java.util.Set;

/**
 * Representation of a Java project.
 */
public record Project(String classPath,
                      String mainClass, Set<String> inputClasses,
                      List<FileContainer> appRootContainers,
                      List<FileContainer> libRootContainers,
                      List<FileContainer> jreRootContainers,
                      int javaVersion) {

    /**
     * This list specifies the processing priority of class file types,
     * arranged from highest to lowest. To change the priority, simply
     * adjust the order of classes in this list.
     */
    private static final List<Class<? extends ClassFile>> CLASS_PRIORITY
            = List.of(DotClassFile.class, DotJavaFile.class);

    /**
     * @return if given class file represents an application class.
     * Now, treat all non-JRE classes as app classes (the same as soot frontend).
     */
    public boolean isApp(ClassFile file) {
        return !jreRootContainers.contains(file.getRootContainer());
    }

    /**
     * @return a {@link ClassIndex} for quick search of classes in the project.
     */
    public ClassIndex makeIndex() {
        return new ClassIndex(this);
    }

    /**
     * @return the class priority list.
     */
    List<Class<? extends ClassFile>> classPriority() {
        return CLASS_PRIORITY;
    }
}
