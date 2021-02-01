/*
 * Tai-e - A Program Analysis Framework for Java
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
import pascal.taie.util.InternalCanonicalized;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@InternalCanonicalized
public class Descriptor {

    private final static Map<Key, Descriptor> map = new HashMap<>();

    private final Key key;

    public static Descriptor get(List<Type> parameterTypes, Type returnType) {
        if (parameterTypes == null) {
            parameterTypes = Collections.emptyList();
        }
        Key key = new Key(parameterTypes, returnType);
        return map.computeIfAbsent(key, Descriptor::new);
    }

    public static void clear() {
        map.clear();
    }

    private Descriptor(Key key) {
        this.key = key;
    }

    public List<Type> getParameterTypes() {
        return key.parameterTypes;
    }

    public Type getReturnType() {
        return key.returnType;
    }

    private static class Key {

        private final List<Type> parameterTypes;

        private final Type returnType;

        private final int hashCode;

        public Key(List<Type> parameterTypes, Type returnType) {
            this.parameterTypes = parameterTypes;
            this.returnType = returnType;
            this.hashCode = Objects.hash(parameterTypes, returnType);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return parameterTypes.equals(key.parameterTypes)
                    && returnType.equals(key.returnType);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
