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

package pascal.taie.language.type;

import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassLoader;
import pascal.taie.util.Hashes;

public class ClassType implements ReferenceType {

    private final JClassLoader loader;

    private final String name;

    /**
     * The cache of {@link ClassType#getJClass()}.
     */
    private transient JClass jclass;

    public ClassType(JClassLoader loader, String name) {
        this.loader = loader;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public JClass getJClass() {
        if (jclass == null) {
            jclass = loader.loadClass(name);
        }
        return jclass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClassType classType = (ClassType) o;
        return loader.equals(classType.loader)
                && name.equals(classType.name);
    }

    @Override
    public int hashCode() {
        return Hashes.hash(loader, name);
    }

    @Override
    public String toString() {
        return name;
    }
}
