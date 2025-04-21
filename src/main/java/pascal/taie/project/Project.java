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

import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Representation of a Java project.
 */
public class Project {

    private final String mainClass;

    private final int javaVersion;

    private final List<String> inputClasses;

    private final Set<String> inputClassesSet;

    private final List<FileContainer> appRootContainers;

    private final List<FileContainer> libRootContainers;

    private final String classPath;

    Project(String mainClass,
            int javaVersion,
            List<String> inputClasses,
            List<FileContainer> appRootContainers,
            List<FileContainer> libRootContainers,
            String classPath) {
        this.mainClass = mainClass;
        this.javaVersion = javaVersion;
        this.inputClasses = inputClasses;
        this.inputClassesSet = Sets.newSet(inputClasses);
        this.appRootContainers = appRootContainers;
        this.libRootContainers = libRootContainers;
        this.classPath = classPath;
    }

    public String getMainClass() {
        return mainClass;
    }

    public int getJavaVersion() {
        return javaVersion;
    }

    public List<String> getInputClasses() {
        return inputClasses;
    }

    public List<FileContainer> getAppRootContainers() {
        return appRootContainers;
    }

    public List<FileContainer> getLibRootContainers() {
        return libRootContainers;
    }

    public boolean isApp(ProgramFile file) {
        return appRootContainers.contains(file.getRootContainer()) ||
                isMainOrInputClass(file);
    }

    private boolean isMainOrInputClass(ProgramFile file) {
        if (file instanceof ClassFile classFile) {
            String className = classFile.getBinaryName();
            return inputClassesSet.contains(className) || className.equals(mainClass);
        } else {
            return false;
        }
    }

    public String getClassPath() {
        return classPath;
    }

    /**
     * @param className the fully qualified name to the analysis file.
     * @return the first file (with the same fully qualified name) found in the containerLists.
     * (QUESTION: how to define priority between different rootContainers?)
     */
    public ProgramFile locate(String className) {
        List<List<FileContainer>> rootContainersList =
                List.of(appRootContainers, libRootContainers);

        for (List<FileContainer> rootContainers : rootContainersList) {
            for (FileContainer container : rootContainers) {
                // make sure to keep the order.
                ClassLocation classLocation = new ClassLocation(className);
                assert classLocation.hasNext();
                ProgramFile result = container.locate(classLocation);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * @param className the fully qualified name to the analysis file.
     * @return all the files with the full path.
     */
    public List<ProgramFile> locateFiles(String className) {
        List<ProgramFile> results = new ArrayList<>();

        Consumer<FileContainer> get = c -> {
            ClassLocation classLocation = new ClassLocation(className);
            assert classLocation.hasNext();
            ProgramFile result = c.locate(classLocation);
            if (result != null) {
                results.add(result);
            }
        };

        appRootContainers.forEach(get);

        libRootContainers.forEach(get);

        return results;
    }
}
