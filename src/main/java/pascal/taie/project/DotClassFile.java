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

import java.nio.file.attribute.FileTime;

/**
 * Represents a <code>.class</code> file in the project to be analyzed.
 *
 * @param className     The name of the class defined in this file (e.g., <code>String</code>).
 * @param internalName  The internal name of the class (e.g., <code>java/lang/String</code>).
 * @param timeStamp     The last modified time of the file.
 * @param resource      The resource from which this file originates.
 * @param rootContainer The container where the file is located.
 */
public record DotClassFile(
        String className,
        String internalName,
        FileTime timeStamp,
        Resource resource,
        FileContainer rootContainer
) implements ProgramFile, ClassFile {

    @Override
    public String getFileName() {
        return className + ".class";
    }

    @Override
    public FileContainer getRootContainer() {
        return rootContainer;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getInternalName() {
        return internalName;
    }

    @Override
    public FileTime getTimeStamp() {
        return timeStamp;
    }

    @Override
    public Resource getResource() {
        return resource;
    }
}
