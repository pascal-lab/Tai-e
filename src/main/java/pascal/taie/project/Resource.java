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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * A <em>Resource</em> is a file that can be read from the file system or a jar file.
 * It has a path and can be read as an input stream or as a byte array.
 * <p>
 * The {@link ProgramFile} represents a logical, abstract file in the project,
 * while this interface represents a physical file in the file system or a jar file.
 * A {@link ProgramFile} contains a {@link Resource} that can be used to read the file.
 * </p>
 *
 * @see FileResource
 * @see ZipEntryResource
 */
public interface Resource {

    InputStream getInputStream() throws IOException;

    byte[] getContent() throws IOException;

    Path getPath();

    void release();
}
