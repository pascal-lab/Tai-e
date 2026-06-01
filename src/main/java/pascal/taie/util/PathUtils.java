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

package pascal.taie.util;

import java.nio.file.Path;

public class PathUtils {

    public static final String JAVA = ".java";

    public static final String CLASS = ".class";

    public static final String JAR = ".jar";

    public static final String ZIP = ".zip";

    private PathUtils() {
    }

    /**
     * Converts a path to String with extension removed.
     */
    public static String toStringWithoutExt(Path path) {
        String pathStr = path.toString();
        int lastDotIndex = pathStr.lastIndexOf('.');
        return lastDotIndex == -1 ? pathStr : pathStr.substring(0, lastDotIndex);
    }

    /**
     * @param path relative path from root (jar, dir, ...), e.g.
     *             <code>java/lang/Object.class</code>
     * @return the fully-qualified class name, e.g., <code>java.lang.Object</code>
     */
    public static String toClassName(Path path) {
        return toStringWithoutExt(path).replace('\\', '/') // in case you're windows
                .replace('/', '.');
    }

    private static boolean withExt(Path path, String ext) {
        return path.getFileName().toString().endsWith(ext);
    }

    public static boolean isJavaFile(Path path) {
        return withExt(path, JAVA);
    }

    public static boolean isClassFile(Path path) {
        return withExt(path, CLASS);
    }

    public static boolean isJarFile(Path path) {
        return withExt(path, JAR);
    }

    /**
     * Checks if given path references a zip file.
     * NOTE: JAR file is also considered as zip file.
     */
    public static boolean isZipFile(Path path) {
        return withExt(path, ZIP) || withExt(path, JAR);
    }
}
