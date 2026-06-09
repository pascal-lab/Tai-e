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

package pascal.taie.analysis.pta.core.cs.element;

import pascal.taie.language.type.Type;

/**
 * A mock implementation of {@link AbstractPointer}.
 */
public final class MockPointer extends AbstractPointer {

    private final PointerDescriptor descriptor;

    private final Object allocation;

    private final Type type;

    MockPointer(PointerDescriptor descriptor, Object allocation, Type type,
                int index) {
        super(index);
        this.descriptor = descriptor;
        this.allocation = allocation;
        this.type = type;
    }

    public PointerDescriptor getDescriptor() {
        return descriptor;
    }

    public Object getAllocation() {
        return allocation;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "MockPointer{"
                + descriptor.description()
                + ": " + allocation
                + '}';
    }

}
