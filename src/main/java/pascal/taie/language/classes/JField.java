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

package pascal.taie.language.classes;

import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.language.annotation.AnnotationHolder;
import pascal.taie.language.generics.ReferenceTypeGSignature;
import pascal.taie.language.type.Type;
import pascal.taie.util.Experimental;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Represents fields in the program. Each instance contains various
 * information of a field, including field name, type, declaring class, etc.
 */
public class JField extends ClassMember {

    private final Type type;

    /**
     * In class file, a field can be associated with a constant value attribute if
     * it is static final.
     * If a (static final) field is associated with a constant value, in
     * {@code <cinit>} method, there will be no initialization for this field.
     * E.g.,
     * <pre>
     * {@code
     * class A {
     *     static final int i = 1;
     * }
     * }
     * </pre>
     * where {@code i} is a static final field, associated with a constant value
     * attribute in class file.
     * In the {@code <cinit>} method, there will be no
     * initialization for {@code i}.
     * <p>
     * Thus, to avoid losing the constant value, we need to store it in the {@code
     * constantValue } field.
     */
    @Nullable
    private final Literal constantValue;

    @Nullable
    @Experimental
    private final ReferenceTypeGSignature gSignature;

    public JField(JClass declaringClass, String name, Set<Modifier> modifiers,
                  Type type, @Nullable ReferenceTypeGSignature gSignature,
                  AnnotationHolder annotationHolder, Literal constantValue) {
        super(declaringClass, name, modifiers, annotationHolder);
        this.type = type;
        this.gSignature = gSignature;
        this.signature = StringReps.getSignatureOf(this);
        this.constantValue = constantValue;
    }

    public Type getType() {
        return type;
    }

    @Nullable
    @Experimental
    public ReferenceTypeGSignature getGSignature() {
        return gSignature;
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

    /**
     * See <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.2">JVM Spec 4.7.2</a>
     * @return constant value for static final fields, can be a primitive type or string value
     */
    public Literal getConstantValue() {
        return constantValue;
    }
}
