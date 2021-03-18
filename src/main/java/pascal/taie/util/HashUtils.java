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

package pascal.taie.util;

/**
 * Avoid array creation of Objects.hash().
 */
public class HashUtils {

    private HashUtils() {
    }

    public static int hash(Object o1, Object o2) {
        return o1.hashCode() * 31 + o2.hashCode();
    }

    public static int hash(Object o1, Object o2, Object o3) {
        int result = o1.hashCode();
        result = 31 * result + o2.hashCode();
        result = 31 * result + o3.hashCode();
        return result;
    }
}
