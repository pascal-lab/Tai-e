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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@InternalCanonicalized
public class MethodReference extends MemberReference {

    private static final ConcurrentMap<Key, MethodReference> map =
            new ConcurrentHashMap<>(4096);

    private final List<Type> parameterTypes;

    private final Type returnType;

    private final Subsignature subsignature;

    /**
     * Cache the resolved method for this reference to avoid redundant
     * method resolution.
     */
    private JMethod method;

    public static MethodReference get(
            JClass declaringClass, String name,
            List<Type> parameterTypes, Type returnType) {
        Subsignature subsignature = Subsignature.get(
                name, parameterTypes, returnType);
        Key key = new Key(declaringClass, subsignature);
        return map.computeIfAbsent(key, k ->
                new MethodReference(k, name, parameterTypes, returnType));
    }

    public static void clear() {
        map.clear();
    }

    private MethodReference(
            Key key, String name, List<Type> parameterTypes, Type returnType) {
        super(key.declaringClass, name);
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
        this.subsignature = key.subsignature;
    }

    public List<Type> getParameterTypes() {
        return parameterTypes;
    }

    public Type getReturnType() {
        return returnType;
    }

    public Subsignature getSubsignature() {
        return subsignature;
    }

    public JMethod getMethod() {
        return method;
    }

    public void setMethod(JMethod method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return StringReps.getSignatureOf(this);
    }

    private static class Key {

        private final JClass declaringClass;

        private final Subsignature subsignature;

        private Key(JClass declaringClass, Subsignature subsignature) {
            this.declaringClass = declaringClass;
            this.subsignature = subsignature;
        }

        @Override
        public int hashCode() {
            return HashUtils.hash(declaringClass, subsignature);
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
                    subsignature.equals(key.subsignature);
        }
    }
}
