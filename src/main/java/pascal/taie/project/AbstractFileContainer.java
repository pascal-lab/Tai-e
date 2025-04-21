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

/**
 * Abstract class for file containers.
 * This class provides a default implementation for the {@link #locate(ClassLocation)} method.
 *
 * @see FileContainer
 */
public abstract class AbstractFileContainer implements FileContainer {
    public ProgramFile locate(ClassLocation relativePath) {
        assert relativePath.hasNext() : "If a ClassLocation is terminated, never pass it to another locate call.";

        String current = relativePath.next();
        if (relativePath.hasNext()) {
            // If classPath.hasNext() then current is a package name.
            // There should exist at most 1 container with the same name.
            var fileContainer = getContainers().stream()
                    .filter(c -> c.getClassName().equals(current))
                    .findAny();
            return fileContainer.map(c -> c.locate(relativePath)).orElse(null);
        } else {
            // else then current is a class name.
            // There should exist at most 1 file with the same name.
            var file = getFiles().stream()
                    .filter(f -> isTarget(f, current))
                    .findAny();
            return file.orElse(null);
        }
    }

    protected static boolean isTarget(ProgramFile file, String className) {
        int endIndex = file.getFileName().indexOf('.');
        return file.getFileName().substring(0, endIndex).equals(className);
    }
}
