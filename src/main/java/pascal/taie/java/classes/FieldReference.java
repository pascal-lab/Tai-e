/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.java.classes;

import pascal.taie.java.types.Type;
import pascal.taie.util.HashUtils;
import pascal.taie.util.InternalCanonicalized;
import pascal.taie.util.StringReps;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@InternalCanonicalized
public class FieldReference extends MemberReference {

    private static final ConcurrentMap<Key, FieldReference> map =
            new ConcurrentHashMap<>(4096);

    private final Type type;

    /**
     * Cache the resolved field for this reference to avoid redundant
     * field resolution.
     */
    private JField field;

    public static FieldReference get(
            JClass declaringClass, String name, Type type) {
        Key key = new Key(declaringClass, name, type);
        return map.computeIfAbsent(key, FieldReference::new);
    }

    public static void clear() {
        map.clear();
    }

    private FieldReference(Key key) {
        super(key.declaringClass, key.name);
        this.type = key.type;
    }

    public Type getType() {
        return type;
    }

    public JField getField() {
        return field;
    }

    public void setField(JField field) {
        this.field = field;
    }

    @Override
    public String toString() {
        return StringReps.getSignatureOf(this);
    }

    private static class Key {

        private final JClass declaringClass;

        private final String name;

        private final Type type;

        private Key(JClass declaringClass, String name, Type type) {
            this.declaringClass = declaringClass;
            this.name = name;
            this.type = type;
        }

        @Override
        public int hashCode() {
            return HashUtils.hash(declaringClass, name, type);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Key key = (Key) o;
            return declaringClass.equals(key.declaringClass) &&
                    name.equals(key.name) &&
                    type.equals(key.type);
        }
    }
}
