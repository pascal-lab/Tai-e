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

public class DirContainer extends AbstractFileContainer {

    private final List<FileContainer> containers;

    private final List<ClassFile> files;

    private final String name;

    DirContainer(List<FileContainer> childContainers,
                        List<ClassFile> childFiles,
                        String name) {
        this.containers = childContainers;
        this.files = childFiles;
        this.name = name;
    }

    @Override
    public List<ClassFile> getFiles() {
        return files;
    }

    @Override
    public List<FileContainer> getSubContainers() {
        return containers;
    }

    @Override
    public String getFileName() {
        return name;
    }

    @Override
    public String getClassName() {
        return name;
    }
}
