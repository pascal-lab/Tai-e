/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.ir.proginfo;

import pascal.taie.language.classes.ClassMember;
import pascal.taie.language.classes.JClass;

import javax.annotation.Nullable;

public abstract class MemberRef {

    private final JClass declaringClass;

    private final String name;

    private final boolean isStatic;

    public MemberRef(JClass declaringClass, String name, boolean isStatic) {
        this.declaringClass = declaringClass;
        this.name = name;
        this.isStatic = isStatic;
    }

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
     *
     * @return the concrete class member pointed by this reference.
     * @throws ResolutionFailedException if the class member
     *  cannot be resolved.
     */
    public abstract ClassMember resolve();

    /**
     *
     * @return the concrete class member pointed by this reference,
     *  or null if the member cannot be resolved.
     */
    public abstract @Nullable ClassMember resolveNullable();
}
