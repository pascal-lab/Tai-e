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

import pascal.taie.World;

public abstract class AbstractFileContainer implements FileContainer {
    public AnalysisFile locate(ClassLocation restLocation) {
        assert restLocation.hasNext() : "If a ClassLocation is terminated, never pass it to another locate call.";

        String current = restLocation.next();
        if (restLocation.hasNext()) {
            // If classPath.hasNext() then current is a package name.
            // There should exist at most 1 container with the same name.
            var fileContainer = containers().stream()
                    .filter(c -> c.className().equals(current))
                    .findAny();
            return fileContainer.map(c -> c.locate(restLocation)).orElse(null);
        } else {
            // else then current is a class name.
            // There should exist at most 1 file with the same name.
            var file = files().stream().
                    filter(f -> isTarget(f, current))
                    .findAny();
            return file.orElse(null);
        }
    }

    protected static boolean isTarget(AnalysisFile file, String className) {
        if (!(file instanceof ClassFile) && ! (!World.get().getOptions().getNoAppendJava()
            && file instanceof JavaSourceFile)) {
            return false;
        }

        int endIndex = file.fileName().indexOf('.');
        return file.fileName().substring(0, endIndex).equals(className);
    }
}
