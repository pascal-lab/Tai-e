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
import java.util.List;

public class ZipContainer extends AbstractFileContainer {

    private final List<ProgramFile> files;

    private final List<FileContainer> containers;

    private final FileTime time;

    protected final String name;


    public ZipContainer(List<ProgramFile> files,
                        List<FileContainer> containers,
                        FileTime time,
                        String name) {
        this.files = files;
        this.containers = containers;
        this.time = time;
        this.name = name;
    }

    @Override
    public List<ProgramFile> getFiles() {
        return files;
    }

    @Override
    public List<FileContainer> getContainers() {
        return containers;
    }

    @Override
    public FileTime getTimeStamp() {
        return time;
    }

    @Override
    public String getFileName() {
        return name + ".zip";
    }

    @Override
    public String getClassName() {
        return name;
    }
}
