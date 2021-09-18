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

package pascal.taie.language.classes;

import java.util.Collections;
import java.util.Set;

public abstract class ClassMember {

    protected final JClass declaringClass;

    protected final String name;

    protected final Set<Modifier> modifiers;

    protected String signature;

    // TODO: annotations, source location

    protected ClassMember(JClass declaringClass, String name,
                          Set<Modifier> modifiers) {
        this.declaringClass = declaringClass;
        this.name = name;
        this.modifiers = modifiers;
    }

    /**
     * @return the declaring class of the class member.
     */
    public JClass getDeclaringClass() {
        return declaringClass;
    }

    public String getName() {
        return name;
    }

    public String getSignature() {
        return signature;
    }

    public Set<Modifier> getModifiers() {
        return Collections.unmodifiableSet(modifiers);
    }

    public boolean isPublic() {
        return Modifier.hasPublic(modifiers);
    }

    public boolean isProtected() {
        return Modifier.hasProtected(modifiers);
    }

    public boolean isPrivate() {
        return Modifier.hasPrivate(modifiers);
    }

    public boolean isStatic() {
        return Modifier.hasStatic(modifiers);
    }

    @Override
    public String toString() {
        return signature;
    }
}
