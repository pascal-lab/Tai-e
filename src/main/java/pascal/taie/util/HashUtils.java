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

import java.util.Objects;

/**
 * Avoids array creation of Objects.hash().
 */
public class HashUtils {

    private HashUtils() {
    }

    /**
     * @return hash code of two objects.
     * @throws NullPointerException if any parameter is null
     */
    public static int hash(Object o1, Object o2) {
        return o1.hashCode() * 31 + o2.hashCode();
    }

    /**
     * @return hash code of two objects, with null check.
     */
    public static int safeHash(Object o1, Object o2) {
        return Objects.hashCode(o1) * 31 + Objects.hashCode(o2);
    }

    /**
     * @return hash code of three objects.
     * @throws NullPointerException if any parameter is null
     */
    public static int hash(Object o1, Object o2, Object o3) {
        int result = o1.hashCode();
        result = 31 * result + o2.hashCode();
        result = 31 * result + o3.hashCode();
        return result;
    }

    /**
     * @return hash code of four objects, with null check.
     */
    public static int safeHash(Object o1, Object o2, Object o3) {
        int result = Objects.hashCode(o1);
        result = 31 * result + Objects.hashCode(o2);
        result = 31 * result + Objects.hashCode(o3);
        return result;
    }

    /**
     * @return hash code of four objects.
     * @throws NullPointerException if any parameter is null
     */
    public static int hash(Object o1, Object o2, Object o3, Object o4) {
        int result = o1.hashCode();
        result = 31 * result + o2.hashCode();
        result = 31 * result + o3.hashCode();
        result = 31 * result + o4.hashCode();
        return result;
    }

    /**
     * @return hash code of four objects, with null check.
     */
    public static int safeHash(Object o1, Object o2, Object o3, Object o4) {
        int result = Objects.hashCode(o1);
        result = 31 * result + Objects.hashCode(o2);
        result = 31 * result + Objects.hashCode(o3);
        result = 31 * result + Objects.hashCode(o4);
        return result;
    }
}
