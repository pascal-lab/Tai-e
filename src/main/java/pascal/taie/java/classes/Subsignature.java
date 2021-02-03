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

import java.util.HashMap;
import java.util.Map;

/**
 * Method name and descriptor.
 */
public class Subsignature {

    private final String subsig;

    private final static Map<String, Subsignature> map = new HashMap<>();

    public static Subsignature get(String subsig) {
        return map.computeIfAbsent(subsig, Subsignature::new);
    }

    public static void clear() {
        map.clear();
    }

    private Subsignature(String subsig) {
        this.subsig = subsig;
    }

}
