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

package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.ClassReader;
import pascal.taie.project.ClassFile;
import pascal.taie.project.FileContainer;
import pascal.taie.project.Project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AllClassesCWBuilder {

    private static final List<String> excluded = List.of("android$widget$RemoteViews$BaseReflectionAction.class");

    public static List<String> outPutAll(FileContainer root) {
        return outPutAll("", root, true);
    }

    private static List<String> outPutAll(String current, FileContainer container, boolean isRoot) {
        String currentNext = (isRoot) ? "" : current + container.className() + ".";
        List<String> res = new ArrayList<>(container.files().stream()
                .filter(f -> f instanceof ClassFile)
                .filter(f -> ! excluded.contains(f.fileName()))
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
