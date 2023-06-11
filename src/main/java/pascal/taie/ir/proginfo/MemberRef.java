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

package pascal.taie.ir.proginfo;

import pascal.taie.language.classes.ClassMember;
import pascal.taie.language.classes.JClass;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * Represents references to class members in IR.
 */
public abstract class MemberRef implements Serializable {

    private final JClass declaringClass;

    private final String name;

    private final boolean isStatic;

    public MemberRef(JClass declaringClass, String name, boolean isStatic) {
        this.declaringClass = declaringClass;
        this.name = name;
        this.isStatic = isStatic;
    }

    /**
     * @return the declaring class of the reference.
     */
    public JClass getDeclaringClass() {
        return declaringClass;
    }

    public String getName() {
        return name;
    }

    public boolean isStatic() {
        return isStatic;
    }

    /**
     * @return the concrete class member pointed by this reference.
     * @throws ResolutionFailedException if the class member
     *                                   cannot be resolved.
     */
    public abstract ClassMember resolve();

    /**
     * @return the concrete class member pointed by this reference,
     * or null if the member cannot be resolved.
     */
    @Nullable
    public abstract ClassMember resolveNullable();
}
