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

import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.language.type.Type;

import java.util.Set;

/**
 * Represents fields in the program. Each instance contains various
 * information of a field, including field name, type, declaring class, etc.
 */
public class JField extends ClassMember {

    private final Type type;

    public JField(JClass declaringClass, String name, Set<Modifier> modifiers,
                  Type type) {
        super(declaringClass, name, modifiers);
        this.type = type;
        this.signature = StringReps.getSignatureOf(this);
    }

    public Type getType() {
        return type;
    }

    // TODO: more modifiers

    /**
     * @return the {@link FieldRef} pointing to this field.
     */
    public FieldRef getRef() {
        return FieldRef.get(declaringClass, name, type, isStatic());
    }

    @Override
    public String toString() {
        return StringReps.getSignatureOf(this);
    }
}
