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

import pascal.taie.ir.stmt.New;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;

import java.util.Optional;

/**
 * Objects that are created by new statements.
 */
public class NewObj extends Obj {

    private final New allocSite;

    NewObj(New allocSite) {
        this.allocSite = allocSite;
    }

    @Override
    public ReferenceType getType() {
        return allocSite.getRValue().getType();
    }

    @Override
    public New getAllocation() {
        return allocSite;
    }

    @Override
    public Optional<JMethod> getContainerMethod() {
        return Optional.of(allocSite.getContainer());
    }

    @Override
    public Type getContainerType() {
        return allocSite.getContainer()
                .getDeclaringClass()
                .getType();
    }

    @Override
    public String toString() {
        return String.format("NewObj{%s[%d@L%d] %s}",
                allocSite.getContainer(), allocSite.getIndex(),
                allocSite.getLineNumber(), allocSite.getRValue());
    }
}
