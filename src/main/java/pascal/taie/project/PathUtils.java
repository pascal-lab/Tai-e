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

import java.nio.file.Path;

public class PathUtils {
    /**
     * @param path relative path from root (jar, dir, ...), e.g.
     *             <code>java/lang/Object.class</code>
     * @return the internal name of the class, which is the path with the file extension removed
     * and slashes replaced by dots. e.g. <code>java.lang.Object</code>
     */
    public static String getInternalName(Path path) {
        String extRemoved = removeExt(path.toString());
        return extRemoved.replace('\\', '/') // in case you're windows
                .replace('/', '.');
    }

    public static String getClassName(Path path) {
        return removeExt(path.getFileName().toString());
    }

    private static String removeExt(String s) {
        int dotIndex = s.lastIndexOf('.');
        return (dotIndex == -1) ? s : s.substring(0, dotIndex);
    }
}
