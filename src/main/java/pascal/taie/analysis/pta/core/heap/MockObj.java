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

package pascal.taie.analysis.pta.core.heap;

import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.Hashes;

import java.util.Optional;

/**
 * Represents the objects whose allocation sites are not explicitly
 * written in the program.
 */
public class MockObj extends Obj {

    private final Descriptor desc;

    private final Object alloc;

    private final Type type;

    private final JMethod container;

    private final boolean isFunctional;

    public MockObj(Descriptor desc, Object alloc, Type type,
                   JMethod container, boolean isFunctional) {
        this.desc = desc;
        this.alloc = alloc;
        this.type = type;
        this.container = container;
        this.isFunctional = isFunctional;
    }

    public Descriptor getDescriptor() {
        return desc;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Object getAllocation() {
        return alloc;
    }

    @Override
    public Optional<JMethod> getContainerMethod() {
        return Optional.ofNullable(container);
    }

    @Override
    public Type getContainerType() {
        return container != null ?
                container.getDeclaringClass().getType() : type;
    }

    @Override
    public boolean isFunctional() {
        return isFunctional;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MockObj that = (MockObj) o;
        return desc.equals(that.desc) &&
                alloc.equals(that.alloc) &&
                type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Hashes.safeHash(desc, alloc, type);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(desc.string()).append('{');
        sb.append("alloc=").append(alloc).append(",");
        sb.append("type=").append(type);
        if (container != null) {
            sb.append(" in ").append(container);
        }
        sb.append("}");
        return sb.toString();
    }
}
