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

import pascal.taie.config.Options;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class OptionsProjectBuilder extends AbstractProjectBuilder {

    private final Options options;

    private Project project;

    public OptionsProjectBuilder(Options options) {
        this.options = options;
    }

    @Override
    protected String getMainClass() {
        return options.getMainClass();
    }

    @Override
    protected int getJavaVersion() {
        return options.getJavaVersion();
    }

    @Override
    protected List<String> getInputClasses() {
        return getInputClasses(options);
    }

    @Override
    protected List<FileContainer> getRootContainers() {
        return Stream.concat(
                project.getAppRootContainers().stream(),
                project.getLibRootContainers().stream()
        ).toList();
    }

    @Override
    public Project build() {
        try {
            List<String> appClassPaths = options.getAppClassPath();

            List<String> libClassPaths = new ArrayList<>(getClassPath(options));
            libClassPaths.removeAll(appClassPaths);

            project = new Project(
                    getMainClass(),
                    getJavaVersion(),
                    getInputClasses(),
                    FileLoader.get().loadRootContainers(
                            appClassPaths.stream().distinct().map(Path::of).toList()),
                    FileLoader.get().loadRootContainers(
                            Stream.concat(
                                libClassPaths.stream().distinct().map(Path::of),
                                listJrtModule(options)).toList()),
                    String.join(File.pathSeparator,
                            Stream.concat(
                                    options.getClassPath().stream(),
                                    options.getAppClassPath().stream()).toList()));
            if (options.getExtractAllClasses()) {
                List<String> inputClasses = project.getInputClasses();
                for (FileContainer path : project.getLibRootContainers()) {
                    inputClasses.addAll(outPutAll(path));
                }
            }
            return project;
        } catch (IOException e) {
            // TODO: more info
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static List<String> outPutAll(FileContainer root) {
        return outPutAll("", root, true);
    }

    private static List<String> outPutAll(String current, FileContainer container, boolean isRoot) {
        String currentNext = (isRoot) ? "" : current + container.className() + ".";
        List<String> res = new ArrayList<>(container.files().stream()
                .filter(f -> f instanceof ClassFile)
                .filter(f -> ! f.fileName().equals("module-info.class"))
                .map(c -> {
                    String fullClassName = currentNext + ((ClassFile) c).getClassName();
                    assert !fullClassName.contains("/");
                    return fullClassName;
                })
                .toList());
        res.addAll(outPutAll(currentNext, container.containers()));
        return res;
    }

    private static List<String> outPutAll(String current, List<FileContainer> containers) {
        return containers.stream()
                .flatMap(c -> outPutAll(current, c, false).stream())
                .toList();
    }

}
