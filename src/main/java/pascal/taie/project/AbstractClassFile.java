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

import java.util.Objects;

abstract class AbstractClassFile implements ClassFile {

    private final String internalName;

    private final Resource resource;

    private final FileContainer rootContainer;

    AbstractClassFile(String internalName,
                      Resource resource, FileContainer rootContainer) {
        this.internalName = internalName;
        this.resource = resource;
        this.rootContainer = rootContainer;
    }

    @Override
    public String getInternalName() {
        return internalName;
    }

    @Override
    public String getBinaryName() {
        return internalName.replace('/', '.');
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public FileContainer getRootContainer() {
        return rootContainer;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractClassFile other = (AbstractClassFile) o;
        return internalName.equals(other.internalName)
                && resource.equals(other.resource)
                && rootContainer.equals(other.rootContainer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(internalName, resource, rootContainer);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + getBinaryName() + '}';
    }
}
