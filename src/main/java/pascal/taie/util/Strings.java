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
 * Static utility methods for {@link String}.
 */
public final class Strings {

    private Strings() {
    }

    /**
     * Capitalizes a {@link String}, changing the first letter to upper case
     * as per {@link Character#toUpperCase(char)}. No other letters are changed.
     *
     * @param str the {@link String} to capitalize
     * @return the capitalized {@link String}
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        char oldFirst = str.charAt(0);
        char newFirst = Character.toUpperCase(oldFirst);
        if (oldFirst == newFirst) {
            return str;
        }
        char[] chars = str.toCharArray();
        chars[0] = newFirst;
        return new String(chars);
    }

}
