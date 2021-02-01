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

import pascal.taie.util.InternalCanonicalized;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Method name and descriptor.
 */
@InternalCanonicalized
public class Subsignature {

    private final Key key;

    private final static Map<Key, Subsignature> map = new HashMap<>();

    public static Subsignature get(String name, Descriptor descriptor) {
        Key key = new Key(name, descriptor);
        return map.computeIfAbsent(key, Subsignature::new);
    }

    public static void clear() {
        map.clear();
    }

    private Subsignature(Key key) {
        this.key = key;
    }

    public String getName() {
        return key.name;
    }

    public Descriptor getDescriptor() {
        return key.descriptor;
    }

    private static class Key {

        private final String name;

        private final Descriptor descriptor;

        public Key(String name, Descriptor descriptor) {
            this.name = name;
            this.descriptor = descriptor;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return name.equals(key.name) && descriptor.equals(key.descriptor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, descriptor);
        }
    }
}
